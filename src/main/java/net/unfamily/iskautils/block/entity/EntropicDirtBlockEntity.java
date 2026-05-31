package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.ModItems;

public class EntropicDirtBlockEntity extends BlockEntity {
    public EntropicDirtBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPIC_DIRT_BE.get(), pos, state);
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
}
