package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom BlockItem for Fan to add tooltip descriptions
 */
public class FanBlockItem extends BlockItem {
    
    public FanBlockItem(Block block, Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        List<Component> lines = new ArrayList<>();
        super.appendHoverText(stack, context, tooltipDisplay, lines::add, flag);

        // Add shift placement hint as first line (color from lang)
        lines.add(Math.min(1, lines.size()), Component.translatable("tooltip.iska_utils.shift_place_reverse"));
        lines.forEach(tooltip);
    }
}
