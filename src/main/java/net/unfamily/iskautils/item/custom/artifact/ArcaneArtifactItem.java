package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.integration.apotheosis.ApotheosisCompat;
import net.unfamily.iskautils.util.ArtifactBalanceFormat;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;
import net.unfamily.iskautils.util.ClientPlayerAccess;

import java.util.function.Consumer;

/**
 * Base class for arcane artifacts.
 * Concrete effects are implemented elsewhere (events / keybind integration).
 */
public class ArcaneArtifactItem extends Item {

    public ArcaneArtifactItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void appendArcaneArtifactTooltip(Consumer<Component> tooltip, String path) {
        tooltip.accept(Component.translatable("tooltip.iska_utils." + path + ".cursed"));
        switch (path) {
            case "totem_of_pain" -> {
                String prefix = "tooltip.iska_utils.totem_of_pain.";
                ArtifactTooltipUtil.addLoreLine(tooltip, prefix + "desc0");
                ArtifactTooltipUtil.addLoreLine(tooltip, prefix + "desc1");
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc2",
                        ArtifactBalanceFormat.percent(Config.totemOfPainProcChance));
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc3",
                        ArtifactBalanceFormat.percent(Config.curseOfPainDamagePerLevel));
            }
            case "busted_crown" -> {
                String prefix = "tooltip.iska_utils.busted_crown.";
                String hpBonus = ArtifactBalanceFormat.flatBonus(Config.bustedCrownHpPerArcaneArtifact);
                ArtifactTooltipUtil.addLoreLine(tooltip, prefix + "desc0");
                ArtifactTooltipUtil.addLoreLine(tooltip, prefix + "desc1");
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc2", hpBonus);
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc3", hpBonus);
            }
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
            case "necrotic_crystal_heart" -> {
                String prefix = "tooltip.iska_utils.necrotic_crystal_heart.";
                ArtifactTooltipUtil.addLoreLine(tooltip, prefix + "desc0");
                ArtifactTooltipUtil.addLoreLine(tooltip, prefix + "desc1");
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc2");
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc3",
                        ArtifactBalanceFormat.flatBonus(Config.necroticCrystalHeartHpCostPerSave));
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc4",
                        ArtifactBalanceFormat.flatBonus(Config.necroticCrystalHeartMinMaxHealth));
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc5");
            }
            case "arcane_dictionary" -> ArtifactTooltipUtil.appendDescLines(
                    tooltip, path, 1, 1, Config.arcaneDictionaryMaxRollLevels);
            case "the_deception" -> ArtifactTooltipUtil.appendDescLines(tooltip, path, 3);
            case "calling_bell" -> {
                String prefix = "tooltip.iska_utils.calling_bell.";
                ArtifactTooltipUtil.addLoreLine(tooltip, prefix + "desc0");
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc1", Config.callingBellArcaneArtifactThreshold);
                ArtifactTooltipUtil.addTechLine(tooltip, prefix + "desc2",
                        ArtifactBalanceFormat.flatBonus(Config.callingBellHpBonus),
                        ArtifactBalanceFormat.flatBonus(Config.callingBellArmorBonus),
                        ArtifactBalanceFormat.flatBonus(Config.callingBellToughnessBonus));
            }
            case "entropic_ring" -> {
                ArtifactTooltipUtil.addLoreLine(tooltip, "tooltip.iska_utils.entropic_ring.desc0");
                Player player = ClientPlayerAccess.getLocalPlayer();
                ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.entropic_ring.desc3",
                        ArtifactBalanceFormat.flatBonus(ApotheosisCompat.getEffectiveDamagePer100Hp(player)));
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    ApotheosisCompat.WorldTierInfo tierInfo = ApotheosisCompat.getWorldTierInfo(player);
                    if (tierInfo != null) {
                        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.entropic_ring.apotheosis",
                                tierInfo.displayName(), tierInfo.ringPowerMultiplier());
                    }
                }
            }
            default -> ArtifactTooltipUtil.appendDescLines(tooltip, path, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        appendArcaneArtifactTooltip(tooltip, id.getPath());
    }
}
