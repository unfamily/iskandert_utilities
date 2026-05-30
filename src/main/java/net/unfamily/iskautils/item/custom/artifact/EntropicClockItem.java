package net.unfamily.iskautils.item.custom.artifact;

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
 * Temporal Overclocker upgrade item. Installed in the upgrade slot of the machine GUI.
 */
public class EntropicClockItem extends Item {
    public EntropicClockItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        ArtifactTooltipUtil.addLoreLine(tooltip, "tooltip.iska_utils.entropic_clock.desc0");
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.entropic_clock.desc1",
                Component.translatable("block.iska_utils.temporal_overclocker"));
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.entropic_clock.desc2",
                ArtifactBalanceFormat.flatBonus(Config.entropicClockMaxFactorMultiplier));
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.entropic_clock.desc3",
                Config.entropicClockEntropyPerTick);
    }
}
