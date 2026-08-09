package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactBalanceFormat;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.function.Consumer;

/**
 * Entropic Champagne — a seasonal curio (Jan 1–10).
 * When equipped in Curios, gives a 15% chance to re-apply a positive mob effect
 * for a random duration when that effect expires naturally.
 */
public class EntropicChampagneItem extends Item {

    public EntropicChampagneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        ArtifactTooltipUtil.addLoreLine(tooltip, "tooltip.iska_utils.entropic_champagne.desc0");
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.entropic_champagne.desc1");
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.entropic_champagne.desc2",
                ArtifactBalanceFormat.percent(Config.entropicChampagneProcChance),
                Config.entropicChampagneMinDurationSeconds,
                Config.entropicChampagneMaxDurationSeconds);
    }
}
