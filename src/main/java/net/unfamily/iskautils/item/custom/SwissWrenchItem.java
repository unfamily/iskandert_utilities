package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock;

/**
 * Universal wrench. Rotation is driven by the Swiss Rotate keybind (default R).
 * Shift+use passes through so other mods can pick up or interact with blocks.
 */
public class SwissWrenchItem extends Item {

    public SwissWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Never consume block use: Shift+RMB stays free for other mods; rotate via keybind only.
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, tooltipAdder, tooltipFlag);

        tooltipAdder.accept(Component.translatable("item.iska_utils.swiss_wrench.tooltip.desc0"));
        tooltipAdder.accept(Component.translatable("item.iska_utils.swiss_wrench.tooltip.desc_shift"));

        if (Config.swissWrenchLegacyModes) {
            SetWrenchDirectionBlock.RotationMode currentMode = SetWrenchDirectionBlock.getSelectedRotationMode(stack);
            tooltipAdder.accept(Component.translatable("item.iska_utils.swiss_wrench.tooltip.current_mode",
                    currentMode.getDisplayName()));
            tooltipAdder.accept(Component.translatable("item.iska_utils.swiss_wrench.tooltip.desc1"));
        }
    }
}
