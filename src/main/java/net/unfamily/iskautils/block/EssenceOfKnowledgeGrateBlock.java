package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.fluid.ModFluids;
import net.unfamily.iskautils.util.ExperienceFluidMath;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin grate that drains player experience into a fluid tank on the block below.
 * Base drain always runs; sneak toggles acceleration on/off (resets off when leaving all grates).
 * Standing on multiple grates (edges) shares one acceleration session and drains into each tank.
 */
public class EssenceOfKnowledgeGrateBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<EssenceOfKnowledgeGrateBlock> CODEC = simpleCodec(EssenceOfKnowledgeGrateBlock::new);

    /** Matches {@code plate_base} height (0.5 / 16 block). */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.5, 16);

    private static final Identifier FALLBACK_XP_FLUID =
            Identifier.fromNamespaceAndPath("cognition", "cognitium_source");

    private static final Map<UUID, StandProgress> STANDING = new ConcurrentHashMap<>();

    /**
     * Per-player session while standing on one or more adjacent grates.
     * Acceleration is shared across all grates underfoot and kept when moving between them.
     */
    private static final class StandProgress {
        int accelTicks;
        long lastGameTime;
        boolean accelerationEnabled;
        boolean shiftWasDown;
        boolean announced;
    }

    public EssenceOfKnowledgeGrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir();
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        if (directionToNeighbour == Direction.DOWN && !canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        StandProgress progress = updateStanding(player, level.getGameTime());
        handleShiftToggle(player, progress);
        if (!progress.announced) {
            progress.announced = true;
            showAccelerationStatus(player, progress.accelerationEnabled);
        }

        int rate = drainRate(progress);
        if (rate <= 0) {
            return;
        }
        transferExperienceTick(level, pos, player, rate);
    }

    /** Clears stand state and action-bar hint after the player leaves the grate. */
    public static void clearIfLeft(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        StandProgress progress = STANDING.get(player.getUUID());
        if (progress == null) {
            return;
        }
        if (player.level().getGameTime() <= progress.lastGameTime + 1L) {
            return;
        }
        STANDING.remove(player.getUUID());
        clearActionBar(player);
    }

    private static StandProgress updateStanding(Player player, long gameTime) {
        UUID id = player.getUUID();
        StandProgress progress = STANDING.get(id);
        // Only start a new session after fully leaving all grates (gap > 1 tick).
        // Moving between / straddling multiple grates keeps acceleration.
        if (progress == null || gameTime > progress.lastGameTime + 1L) {
            progress = new StandProgress();
            progress.accelTicks = 0;
            progress.accelerationEnabled = false;
            progress.shiftWasDown = player.isShiftKeyDown();
            progress.announced = false;
            STANDING.put(id, progress);
        }
        if (progress.lastGameTime != gameTime) {
            if (progress.accelerationEnabled) {
                progress.accelTicks++;
            } else {
                progress.accelTicks = 0;
            }
            progress.lastGameTime = gameTime;
        }
        return progress;
    }

    private static void handleShiftToggle(Player player, StandProgress progress) {
        boolean shiftDown = player.isShiftKeyDown();
        if (shiftDown && !progress.shiftWasDown) {
            progress.accelerationEnabled = !progress.accelerationEnabled;
            if (!progress.accelerationEnabled) {
                progress.accelTicks = 0;
            }
            showAccelerationStatus(player, progress.accelerationEnabled);
        }
        progress.shiftWasDown = shiftDown;
    }

    private static void showAccelerationStatus(Player player, boolean enabled) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Component sneakKey = Component.keybind("key.sneak");
        serverPlayer.sendSystemMessage(
                enabled
                        ? Component.translatable(
                                        "message.iska_utils.essence_of_knowledge_grate.acceleration.enabled", sneakKey)
                                .withStyle(ChatFormatting.GREEN)
                        : Component.translatable(
                                        "message.iska_utils.essence_of_knowledge_grate.acceleration.disabled", sneakKey)
                                .withStyle(ChatFormatting.GRAY),
                true);
    }

    private static void clearActionBar(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.sendSystemMessage(Component.empty(), true);
    }

    private static int drainRate(StandProgress progress) {
        int base = Config.essenceOfKnowledgeGrateXpPointsPerTick;
        if (base <= 0) {
            return 0;
        }
        if (!progress.accelerationEnabled) {
            return base;
        }
        int max = Math.max(base, Config.essenceOfKnowledgeGrateMaxXpPointsPerTick);
        int rampTicks = Math.max(1, Config.essenceOfKnowledgeGrateAccelerationTicks);
        if (max <= base) {
            return base;
        }
        int rampProgress = Math.min(progress.accelTicks, rampTicks);
        return base + (int) (((long) (max - base) * rampProgress) / rampTicks);
    }

    static void transferExperienceTick(Level level, BlockPos gratePos, Player player, int xpPointsPerTick) {
        ResourceHandler<FluidResource> handler =
                level.getCapability(Capabilities.Fluid.BLOCK, gratePos.below(), Direction.UP);
        if (handler == null) {
            return;
        }

        long totalXp = ExperienceFluidMath.levelsToXp(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        if (totalXp <= 0) {
            return;
        }

        long toDrain = Math.min(totalXp, xpPointsPerTick);
        long mb = ExperienceFluidMath.mbFromXpPoints(toDrain);
        if (mb <= 0) {
            return;
        }

        int offerMb = mb > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) mb;
        int executed = fillPreferred(handler, offerMb);
        if (executed <= 0) {
            return;
        }

        long pointsTaken = ExperienceFluidMath.xpPointsFromMb(executed);
        if (pointsTaken > 0) {
            player.giveExperiencePoints(-(int) Math.min(pointsTaken, Integer.MAX_VALUE));
            if (level.getGameTime() % 8L == 0L) {
                level.playSound(
                        null,
                        gratePos,
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.BLOCKS,
                        0.25F,
                        0.8F + level.getRandom().nextFloat() * 0.3F);
            }
        }
    }

    private static int fillPreferred(ResourceHandler<FluidResource> handler, int offerMb) {
        int executed = tryInsert(handler, ModFluids.CONDENSED_KNOWLEDGE.getSource(), offerMb);
        if (executed > 0) {
            return executed;
        }
        Fluid fallback = BuiltInRegistries.FLUID.getOptional(FALLBACK_XP_FLUID).orElse(null);
        if (fallback != null) {
            return tryInsert(handler, fallback, offerMb);
        }
        return 0;
    }

    private static int tryInsert(ResourceHandler<FluidResource> handler, Fluid fluid, int offerMb) {
        FluidResource resource = FluidResource.of(new FluidStack(fluid, offerMb));
        try (Transaction transaction = Transaction.openRoot()) {
            int executed = handler.insert(resource, offerMb, transaction);
            if (executed <= 0) {
                return 0;
            }
            transaction.commit();
            return executed;
        }
    }
}
