package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Custom BlockItem for Deep Drawers to add tooltip descriptions
 */
public class DeepDrawersBlockItem extends BlockItem {
    
    public DeepDrawersBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Add description from lang file
        tooltip.add(Component.translatable("tooltip.iska_utils.deep_drawers.desc0"));
    }
}

