package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.Config;

import java.util.List;

/**
 * Custom Item for Moderate Module with tooltip showing power and max installable count
 */
public class ModerateModuleItem extends Item {
    
    public ModerateModuleItem(Properties properties) {
        super(properties);
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Show info when shift is held
        if (Screen.hasShiftDown()) {
            // Show max installable count
            tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max", 
                    Config.fanAccelerationUpgradeMax)
                    .withStyle(ChatFormatting.GRAY));
        } else {
            // Show hint to press shift
            tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.press_shift")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
