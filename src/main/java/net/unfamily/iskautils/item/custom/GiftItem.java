package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

/**
 * Gift Item - Special block item with tooltip describing availability
 */
public class GiftItem extends BlockItem {

    public GiftItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        // Add aqua tooltip as first (reusing existing translation)
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.gift.place")
                .withStyle(ChatFormatting.AQUA));
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.gift.availability"));
    }
}
