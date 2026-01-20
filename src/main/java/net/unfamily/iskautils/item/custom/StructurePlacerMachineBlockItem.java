package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Custom BlockItem for Structure Placer Machine to add tooltip descriptions
 */
public class StructurePlacerMachineBlockItem extends BlockItem {
    
    public StructurePlacerMachineBlockItem(Block block, Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Add shift placement hint as first line
        tooltip.add(1, Component.translatable("tooltip.iska_utils.shift_place_reverse")
                .withStyle(ChatFormatting.GRAY));
    }
}
