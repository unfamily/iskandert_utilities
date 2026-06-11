package net.unfamily.iskautils.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Deep Drawer Extractor GUI tooltips (text copied from Another Dynamics duct filter UI). */
public final class DeepDrawerExtractorGuiTooltips {
    private DeepDrawerExtractorGuiTooltips() {}

    public static Component grayLine(String translationKey) {
        return Component.translatable(translationKey).withStyle(ChatFormatting.GRAY);
    }

    public static Tooltip concatChannelTooltip() {
        MutableComponent tip = Component.empty();
        tip.append(Component.translatable("gui.iska_utils.deep_drawer_extractor.filters.concat.tooltip.concat"));
        tip.append("\n");
        tip.append(Component.translatable("gui.iska_utils.deep_drawer_extractor.filters.concat.tooltip.and"));
        tip.append("\n");
        tip.append(Component.translatable("gui.iska_utils.deep_drawer_extractor.concat.channel.click_forward"));
        tip.append("\n");
        tip.append(Component.translatable("gui.iska_utils.deep_drawer_extractor.concat.channel.click_back"));
        return Tooltip.create(tip);
    }
}
