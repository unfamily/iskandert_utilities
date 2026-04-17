package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom BlockItem for Deep Drawers to add tooltip descriptions
 */
public class DeepDrawersBlockItem extends BlockItem {
    
    public DeepDrawersBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        
        // Add description from lang file in light gray
        tooltip.accept(Component.translatable("tooltip.iska_utils.deep_drawers.desc0")
                .withStyle(ChatFormatting.GRAY));
        
        // Add warning in red about not being able to pick up if it contains items
        tooltip.accept(Component.translatable("tooltip.iska_utils.deep_drawers.desc1")
                .withStyle(ChatFormatting.RED));
    }
}

