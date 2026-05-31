package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.EntropicDirtBlockEntity;

public class DropOfEntropyItem extends Item {
    public DropOfEntropyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(ModBlocks.ENTROPIC_DIRT.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server
                && EntropicDirtBlockEntity.tryRestoreWithEntropy(server, pos, stack)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
