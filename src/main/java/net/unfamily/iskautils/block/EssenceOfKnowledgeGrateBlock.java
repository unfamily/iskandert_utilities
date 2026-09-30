package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.fluid.ModFluids;
import net.unfamily.iskautils.util.ExperienceFluidMath;

/**
 * Thin grate that drains player experience into a fluid tank on the block below.
 */
public class EssenceOfKnowledgeGrateBlock extends Block {
    public static final MapCodec<EssenceOfKnowledgeGrateBlock> CODEC = simpleCodec(EssenceOfKnowledgeGrateBlock::new);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public EssenceOfKnowledgeGrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends EssenceOfKnowledgeGrateBlock> codec() {
        return CODEC;
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
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos currentPos,
            BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, currentPos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        int rate = Config.essenceOfKnowledgeGrateXpPointsPerTick;
        if (rate <= 0) {
            return;
        }
        transferExperienceTick(level, pos, player, rate);
    }

    static void transferExperienceTick(Level level, BlockPos gratePos, Player player, int xpPointsPerTick) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, gratePos.below(), Direction.UP);
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
        FluidStack offer = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), offerMb);
        int accepted = handler.fill(offer, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }

        FluidStack filled = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), accepted);
        int executed = handler.fill(filled, IFluidHandler.FluidAction.EXECUTE);
        if (executed <= 0) {
            return;
        }

        long pointsTaken = ExperienceFluidMath.xpPointsFromMb(executed);
        if (pointsTaken > 0) {
            player.giveExperiencePoints(-(int) Math.min(pointsTaken, Integer.MAX_VALUE));
        }
    }
}
