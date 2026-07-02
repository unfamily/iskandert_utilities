package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import java.util.List;

/**
 * Custom item for the Auto Shop with usage tooltips.
 */
public class AutoShopItem extends BlockItem {

    public AutoShopItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.iska_utils.auto_shop.tooltip.usage"));
        tooltip.add(Component.translatable("item.iska_utils.auto_shop.tooltip.gui"));
    }
}
