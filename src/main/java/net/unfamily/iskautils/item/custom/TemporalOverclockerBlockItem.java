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
 * Custom BlockItem for Temporal Overclocker to add tooltip descriptions
 */
public class TemporalOverclockerBlockItem extends BlockItem {
    
    public TemporalOverclockerBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        
        // Add description from lang file
        tooltip.accept(Component.translatable("tooltip.iska_utils.temporal_overclocker.desc0")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.iska_utils.temporal_overclocker.desc1")
                .withStyle(ChatFormatting.GRAY));
    }
}

