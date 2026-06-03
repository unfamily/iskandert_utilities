package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.EntropicSoilUtil;

public class EntropicDirtBlockEntity extends BlockEntity {
    private static final String TAG_LIGHT_REVERT = "light_revert_ticks";
    private static final String TAG_LIGHT_REVERT_TARGET = "light_revert_target";
    private static final int LIGHT_REVERT_INTERVAL = 4;

    private int lightRevertTicks;
    private int lightRevertTarget = -1;

    public EntropicDirtBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPIC_DIRT_BE.get(), pos, state);
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, EntropicDirtBlockEntity blockEntity) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        long schedule = server.getGameTime() + pos.asLong();
        if ((schedule & (LIGHT_REVERT_INTERVAL - 1L)) == 0L) {
            blockEntity.tickLightRevert(server, LIGHT_REVERT_INTERVAL);
        }
    }

    private void tickLightRevert(ServerLevel level, int elapsed) {
        if (!EntropicSoilUtil.isIlluminated(level, worldPosition)) {
            if (lightRevertTicks != 0 || lightRevertTarget != -1) {
                lightRevertTicks = 0;
                lightRevertTarget = -1;
                setChanged();
            }
            return;
        }
        if (lightRevertTarget < 0) {
            lightRevertTarget = EntropicSoilUtil.rollInclusive(
                    Config.entropicDirtToVanillaMinTicks,
                    Config.entropicDirtToVanillaMaxTicks,
                    level.getRandom());
            setChanged();
        }
        lightRevertTicks += elapsed;
        if (lightRevertTicks >= lightRevertTarget) {
            level.setBlock(worldPosition, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void onDirtPlaced(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EntropicDirtBlockEntity be) {
            be.lightRevertTicks = 0;
            be.lightRevertTarget = -1;
            be.setChanged();
        }
    }

    public static boolean tryRestoreWithEntropy(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!stack.is(ModItems.DROP_OF_ENTROPY.get())) {
            return false;
        }
        if (!level.getBlockState(pos).is(ModBlocks.ENTROPIC_DIRT.get())) {
            return false;
        }
        stack.shrink(1);
        level.setBlock(pos, ModBlocks.ENTROPIC_SOIL.get().defaultBlockState(), Block.UPDATE_ALL);
        EntropicSoilBlockEntity.onSoilPlaced(level, pos);
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(TAG_LIGHT_REVERT, lightRevertTicks);
        output.putInt(TAG_LIGHT_REVERT_TARGET, lightRevertTarget);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lightRevertTicks = input.getInt(TAG_LIGHT_REVERT).orElse(0);
        lightRevertTarget = input.getInt(TAG_LIGHT_REVERT_TARGET).orElse(-1);
    }
}
