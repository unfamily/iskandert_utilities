package net.unfamily.iskautils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.iskautils.block.entity.EntropyTntBlockEntity;
import net.unfamily.iskautils.entity.PrimedEntropyTnt;
import net.unfamily.iskautils.util.EntropyExplosionHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Entropy TNT: primes a {@link PrimedEntropyTnt} on redstone/ignition instead of exploding instantly.
 */
public class EntropyTntBlock extends Block implements EntityBlock {
    public static final BooleanProperty UNSTABLE = BlockStateProperties.UNSTABLE;

    public EntropyTntBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(UNSTABLE, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UNSTABLE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntropyTntBlockEntity(pos, state);
    }

    @Override
    public boolean onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
        return prime(level, pos, igniter);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(pos) && onCaughtFire(state, level, pos, null, null)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean isMoving) {
        if (level.hasNeighborSignal(pos) && onCaughtFire(state, level, pos, null, null)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild && state.getValue(UNSTABLE)) {
            onCaughtFire(state, level, pos, null, null);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        if (!level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return;
        }
        EntropyExplosionHelper.Params params = resolveParams(level, pos);
        PrimedEntropyTnt primed = new PrimedEntropyTnt(
                level,
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                explosion.getIndirectSourceEntity(),
                params);
        int fuse = primed.getFuse();
        primed.setFuse((short) (level.getRandom().nextInt(fuse / 4) + fuse / 8));
        level.addFreshEntity(primed);
    }

    public static boolean prime(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return false;
        }
        EntropyExplosionHelper.Params params = resolveParams(level, pos);
        PrimedEntropyTnt primed = new PrimedEntropyTnt(
                level,
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                igniter,
                params);
        level.addFreshEntity(primed);
        level.playSound(null, primed.getX(), primed.getY(), primed.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        return true;
    }

    private static EntropyExplosionHelper.Params resolveParams(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EntropyTntBlockEntity be) {
            return be.getParams();
        }
        return EntropyExplosionHelper.Params.entropyTntDefaults();
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (onCaughtFire(state, level, pos, hitResult.getDirection(), player)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            Item item = stack.getItem();
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else {
                stack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(item));
        } else if (level instanceof ServerLevel serverLevel && !serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            player.sendOverlayMessage(Component.translatable("block.minecraft.tnt.disabled"));
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = hit.getBlockPos();
            Entity owner = projectile.getOwner();
            if (projectile.isOnFire()
                    && projectile.mayInteract(serverLevel, pos)
                    && onCaughtFire(state, level, pos, null, owner instanceof LivingEntity living ? living : null)) {
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(UNSTABLE, Boolean.FALSE);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof EntropyTntBlockEntity be) {
            be.loadFromItem(stack);
        }
    }
}
