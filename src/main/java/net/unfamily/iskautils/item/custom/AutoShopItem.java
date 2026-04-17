package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.Item;



import java.util.List;
import java.util.function.Consumer;

/**
 * Item personalizzato per l'Auto Shop con tooltip informativi
 */
public class AutoShopItem extends BlockItem {
    
    public AutoShopItem(Block block, Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        
        // Aggiungi informazioni su come usare l'Auto Shop
        tooltip.accept(Component.translatable("item.iska_utils.auto_shop.tooltip.usage"));
        tooltip.accept(Component.translatable("item.iska_utils.auto_shop.tooltip.modes"));
        tooltip.accept(Component.translatable("item.iska_utils.auto_shop.tooltip.currency"));
    }
} 