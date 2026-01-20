package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.Item;



import java.util.List;

/**
 * Item personalizzato per l'Auto Shop con tooltip informativi
 */
public class AutoShopItem extends BlockItem {
    
    public AutoShopItem(Block block, Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Add shift placement hint as first line
        tooltip.add(1, Component.translatable("tooltip.iska_utils.shift_place_reverse")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        
        // Aggiungi informazioni su come usare l'Auto Shop
        tooltip.add(Component.translatable("item.iska_utils.auto_shop.tooltip.usage"));
        tooltip.add(Component.translatable("item.iska_utils.auto_shop.tooltip.modes"));
        tooltip.add(Component.translatable("item.iska_utils.auto_shop.tooltip.currency"));
    }
} 