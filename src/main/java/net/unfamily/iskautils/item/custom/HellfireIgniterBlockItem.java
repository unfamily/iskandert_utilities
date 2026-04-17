package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.unfamily.iskautils.Config;

import java.util.List;
import java.util.function.Consumer;

/**
 * Custom BlockItem for Hellfire Igniter to add tooltip descriptions
 */
public class HellfireIgniterBlockItem extends BlockItem {
    
    public HellfireIgniterBlockItem(Block block, Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        
        // Add shift placement hint only when vanilla-like mode is off (inverted placement is disabled in vanilla-like)
        if (!Config.hellfireIgniterVanillaLike) {
            tooltip.accept(Component.translatable("tooltip.iska_utils.shift_place_reverse"));
        }
    }
}
