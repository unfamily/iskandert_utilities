package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.entity.DyeBushEmptyBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Empty dye bush block (full block, leaves-like).
 * Refills after a configurable time and becomes filled.
 */
public class DyeBushEmptyBlock extends Block implements EntityBlock {
    public static final MapCodec<DyeBushEmptyBlock> CODEC = simpleCodec(DyeBushEmptyBlock::new);

    public DyeBushEmptyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof DyeBushEmptyBlockEntity blockEntity && lvl instanceof ServerLevel server) {
                if (blockEntity.shouldRefill()) {
                    blockEntity.fillWithBerries(server, pos, st);
                }
            }
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DyeBushEmptyBlockEntity(pos, state);
    }
}
