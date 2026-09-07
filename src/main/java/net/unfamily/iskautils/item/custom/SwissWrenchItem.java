package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;
import net.unfamily.iskautils.util.KeybindTooltipUtil;

import java.util.function.Consumer;

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
                                TooltipDisplay display,
                                Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, tooltipAdder, tooltipFlag);

        Component keybind = KeybindTooltipUtil.keybindOrTranslation(
                "key.iska_utils.swiss_wrench_rotate", "SWISS_WRENCH_ROTATE_KEY");
        ArtifactTooltipUtil.addLoreLine(tooltipAdder, "item.iska_utils.swiss_wrench.tooltip.desc0", keybind);
        ArtifactTooltipUtil.addLoreLine(tooltipAdder, "item.iska_utils.swiss_wrench.tooltip.desc_shift");

        if (Config.swissWrenchLegacyModes) {
            SetWrenchDirectionBlock.RotationMode currentMode = SetWrenchDirectionBlock.getSelectedRotationMode(stack);
            ArtifactTooltipUtil.addTechLine(tooltipAdder, "item.iska_utils.swiss_wrench.tooltip.current_mode",
                    currentMode.getDisplayName());
            ArtifactTooltipUtil.addTechLine(tooltipAdder, "item.iska_utils.swiss_wrench.tooltip.desc1");
        }
    }
}
