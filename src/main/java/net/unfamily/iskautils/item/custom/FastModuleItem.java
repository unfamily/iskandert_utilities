package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.integration.PatternCrafterTooltipHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.glfw.GLFW;

/**
 * Custom Item for Fast Module with tooltip showing power and max installable count
 * (Modular Fan and Pattern Crafter when present).
 */
public class FastModuleItem extends Item {

    public FastModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);

        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

        if (shiftDown) {
            tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max",
                    Config.fanAccelerationUpgradeMax)
                    .withStyle(ChatFormatting.GRAY));
            if (ModList.get().isLoaded("pattern_crafter")) {
                List<Component> tmp = new ArrayList<>();
                PatternCrafterTooltipHelper.addSpeedModuleTooltip(tmp, "fast");
                tmp.forEach(tooltip);
            }
        } else {
            tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.press_shift")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
