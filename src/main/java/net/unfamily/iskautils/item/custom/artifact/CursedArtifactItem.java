package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.TooltipDisplay;

import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactBalanceFormat;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.function.Consumer;

/**
 * Base class for cursed artifacts.
 * Concrete effects are implemented elsewhere (events / keybind integration).
 */
public class CursedArtifactItem extends Item {

    public CursedArtifactItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void appendCursedArtifactTooltip(Consumer<Component> tooltip, String path) {
        tooltip.accept(Component.translatable("tooltip.iska_utils." + path + ".cursed"));
        switch (path) {
            case "totem_of_pain" -> ArtifactTooltipUtil.appendDescLines(
                    tooltip, path, 2, 2, ArtifactBalanceFormat.percent(Config.totemOfPainProcChance));
            case "busted_crown" -> ArtifactTooltipUtil.appendDescLines(
                    tooltip, path, 2, 2, ArtifactBalanceFormat.flatBonus(Config.bustedCrownHpPerCursedArtifact));
            case "ritual_gauntlet" -> {
                String prefix = "tooltip.iska_utils.ritual_gauntlet.";
                ArtifactTooltipUtil.addLoreLine(tooltip, prefix + "desc0");
                ArtifactTooltipUtil.addLoreLine(tooltip, prefix + "desc1");
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc2");
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc3",
                        ArtifactBalanceFormat.percent(Config.ritualGauntletCritChance));
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc4",
                        ArtifactBalanceFormat.percentBonusFromMultiplier(
                                Config.ritualGauntletCritDamageBeneficialNeutral),
                        ArtifactBalanceFormat.percentBonusFromMultiplier(
                                Config.ritualGauntletCritDamageHarmful));
            }
            case "necrotic_crystal_heart" -> ArtifactTooltipUtil.appendDescLines(
                    tooltip, path, 4, 4, ArtifactBalanceFormat.flatBonus(Config.necroticCrystalHeartHpCostPerSave));
            case "arcane_dictionary" -> ArtifactTooltipUtil.appendDescLines(
                    tooltip, path, 1, 1, Config.arcaneDictionaryMaxRollLevels);
            case "the_deception" -> ArtifactTooltipUtil.appendDescLines(tooltip, path, 3);
            default -> ArtifactTooltipUtil.appendDescLines(tooltip, path, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        appendCursedArtifactTooltip(tooltip, id.getPath());
    }
}
