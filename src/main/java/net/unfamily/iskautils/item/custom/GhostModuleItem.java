package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;

import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.glfw.GLFW;

/**
 * Custom Item for Ghost Module with tooltip showing max installable count
 */
public class GhostModuleItem extends Item {
    
    public GhostModuleItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        
        // Show info when shift is held
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (shiftDown) {
            // Show max installable count (always 1 for ghost module)
            tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max", 1)
                    .withStyle(ChatFormatting.GRAY));
        } else {
            // Show hint to press shift
            tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.press_shift")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
