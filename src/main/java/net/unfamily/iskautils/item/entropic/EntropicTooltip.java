package net.unfamily.iskautils.item.entropic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.RelicBalanceFormat;

import java.util.List;

/** Tooltip lines for entropic gear (values from config). */
public final class EntropicTooltip {
    private EntropicTooltip() {}

    public static void appendToolLines(List<Component> tooltip, String path) {
        if ("entropic_paxel".equals(path)) {
            appendPaxelLines(tooltip);
            return;
        }

        appendUnbreakable(tooltip);
        switch (path) {
            case "entropic_helmet" -> {
                tooltip.add(Component.translatable(
                        "tooltip.iska_utils.entropic.helmet.hp",
                        RelicBalanceFormat.flatBonus(Config.entropicHelmetBaseHp)));
                tooltip.add(Component.translatable(
                        "tooltip.iska_utils.entropic.helmet.hp_per_piece",
                        RelicBalanceFormat.flatBonus(Config.entropicHelmetHpPerEntropicPiece)));
            }
            case "entropic_chestplate" -> tooltip.add(Component.translatable(
                    "tooltip.iska_utils.entropic.chestplate.toughness",
                    RelicBalanceFormat.flatBonus(Config.entropicChestplateToughnessBonusPerStep),
                    RelicBalanceFormat.flatBonus(Config.entropicChestplateMissingHpPerStep)));
            case "entropic_leggings" -> tooltip.add(Component.translatable(
                    "tooltip.iska_utils.entropic.leggings.armor",
                    RelicBalanceFormat.flatBonus(Config.entropicLeggingsArmorBonusPerStep),
                    RelicBalanceFormat.flatBonus(Config.entropicLeggingsMissingHpPerStep)));
            case "entropic_boots" -> tooltip.add(Component.translatable("tooltip.iska_utils.entropic.boots.fall"));
            case "entropic_axe" -> {
                appendAxeStrip(tooltip);
                appendArmorPen(tooltip);
            }
            case "entropic_sword", "entropic_spear" -> appendArmorPen(tooltip);
            case "entropic_pickaxe" -> appendPickaxeFortune(tooltip);
            case "entropic_shovel" -> appendShovelBrush(tooltip);
            case "entropic_hoe" -> tooltip.add(Component.translatable("tooltip.iska_utils.entropic.hoe.crop"));
            default -> {}
        }
    }

    private static void appendUnbreakable(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.iska_utils.entropic.unbreakable")
                .withStyle(ChatFormatting.YELLOW));
    }

    private static void appendPaxelLines(List<Component> tooltip) {
        appendUnbreakable(tooltip);
        tooltip.add(Component.translatable("tooltip.iska_utils.entropic.paxel.combo"));
        appendPickaxeFortune(tooltip);
        appendAxeStrip(tooltip);
        appendArmorPen(tooltip);
        appendShovelBrush(tooltip);
    }

    private static void appendPickaxeFortune(List<Component> tooltip) {
        if (Config.entropicPickaxeBonusFortuneLevels <= 0) {
            return;
        }
        tooltip.add(Component.translatable(
                "tooltip.iska_utils.entropic.pickaxe.fortune",
                RelicBalanceFormat.flatBonus(Config.entropicPickaxeBonusFortuneLevels)));
    }

    private static void appendAxeStrip(List<Component> tooltip) {
        if (!Config.entropicAxeStripEnabled) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.iska_utils.entropic.axe.strip"));
    }

    private static void appendShovelBrush(List<Component> tooltip) {
        if (!Config.entropicShovelBrushEnabled) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.iska_utils.entropic.shovel.brush"));
    }

    private static void appendArmorPen(List<Component> tooltip) {
        if (Config.entropicArmorPenChance <= 0.0D || Config.entropicArmorPenIgnoreFraction <= 0.0D) {
            return;
        }
        tooltip.add(Component.translatable(
                "tooltip.iska_utils.entropic.armor_pen",
                RelicBalanceFormat.percent(Config.entropicArmorPenChance),
                RelicBalanceFormat.percent(Config.entropicArmorPenIgnoreFraction)));
    }
}
