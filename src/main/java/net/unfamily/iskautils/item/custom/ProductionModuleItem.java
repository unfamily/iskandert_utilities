package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.unfamily.iskautils.util.ScreenAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.integration.ColossalReactorsTooltipHelper;
import net.unfamily.iskautils.integration.PatternCrafterTooltipHelper;

import java.util.List;

/**
 * Production Module: Pattern Crafter only (1.2.0.0.0+).
 * Tooltip is shown on Shift when a compatible Pattern Crafter version is loaded.
 */
public class ProductionModuleItem extends Item {

    public ProductionModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        boolean showPatternCrafter = PatternCrafterTooltipHelper.supportsProductionModule();
        boolean showColossalReactors = ColossalReactorsTooltipHelper.supportsRadiationScrubberProductionModule();
        if (!showPatternCrafter && !showColossalReactors) {
            return;
        }

        if (ScreenAccess.hasShiftDown()) {
            if (showPatternCrafter) {
                PatternCrafterTooltipHelper.addProductionModuleTooltip(tooltip);
            }
            if (showColossalReactors) {
                ColossalReactorsTooltipHelper.addRadiationScrubberProductionModuleTooltip(tooltip);
            }
        } else {
            tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.press_shift")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
