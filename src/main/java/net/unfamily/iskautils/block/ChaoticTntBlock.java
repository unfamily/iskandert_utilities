package net.unfamily.iskautils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.RandomSource;
import net.unfamily.iskautils.explosion.ExplosionSystem;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class ChaoticTntBlock extends Block {
    public static final BooleanProperty UNSTABLE = BooleanProperty.create("unstable");

    public ChaoticTntBlock() {
        super(Properties.of().strength(0.0F).sound(net.minecraft.world.level.block.SoundType.GRASS).ignitedByLava());
        this.registerDefaultState(this.stateDefinition.any().setValue(UNSTABLE, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UNSTABLE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            if (level.hasNeighborSignal(pos)) {
                triggerExplosion(level, pos, null);
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.hasNeighborSignal(pos)) {
            triggerExplosion(level, pos, null);
            level.removeBlock(pos, false);
        }
    }

    private void triggerExplosion(Level level, BlockPos pos, @Nullable Entity entity) {
        if (level.isClientSide()) return;

        // Create explosion with specified parameters: 250 50 10 1000 true
        ExplosionSystem.createExplosion(
            (ServerLevel) level,
            pos,
            250,  // horizontal radius
            50,   // vertical radius
            10,   // tick interval (cambiato da 1 a 10)
            1000.0f, // damage
            true  // break unbreakable
        );

        // Handle durability consumption if entity is a player with an item
        if (entity instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isDamageableItem()) {
                mainHand.setDamageValue(mainHand.getDamageValue() + 1);
                if (mainHand.getDamageValue() >= mainHand.getMaxDamage()) {
                    mainHand.shrink(1);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(UNSTABLE, Boolean.FALSE);
    }
} 