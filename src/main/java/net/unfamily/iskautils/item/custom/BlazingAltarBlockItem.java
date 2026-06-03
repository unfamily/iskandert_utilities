package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;
import org.jetbrains.annotations.Nullable;

public class BlazingAltarBlockItem extends BlockItem {

    public BlazingAltarBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
            BlockPos pos,
            Level level,
            @Nullable Player player,
            ItemStack stack,
            BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BlazingAltarBlockEntity altar) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                altar.loadFromDropTag(customData.copyTag(), level.registryAccess());
            }
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }
}
