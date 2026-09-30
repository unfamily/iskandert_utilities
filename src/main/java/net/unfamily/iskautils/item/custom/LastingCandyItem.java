package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.function.Consumer;

/**
 * Lasting Candy — seasonal Halloween curio (Oct 26–Nov 5).
 * Cancels incoming damage briefly, then enters a cooldown.
 */
public class LastingCandyItem extends Item {

    public LastingCandyItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        ArtifactTooltipUtil.addLoreLine(tooltip, "tooltip.iska_utils.lasting_candy.desc0");
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.lasting_candy.desc1");
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.lasting_candy.desc2",
                Config.lastingCandyInvulnSeconds,
                Config.lastingCandyCooldownSeconds);
        tooltip.accept(Component.translatable("tooltip.iska_utils.lasting_candy.availability"));
    }
}
