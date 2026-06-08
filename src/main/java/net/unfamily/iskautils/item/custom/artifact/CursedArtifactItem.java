package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.integration.apotheosis.ApotheosisCompat;
import net.unfamily.iskautils.util.ArtifactBalanceFormat;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;
import net.unfamily.iskautils.util.ClientPlayerAccess;

import java.util.List;

/**
 * Base class for cursed artifacts.
 * Concrete effects are implemented elsewhere (events / keybind integration).
 */
public class CursedArtifactItem extends Item {

    public CursedArtifactItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void appendCursedArtifactTooltip(List<Component> tooltip, String path) {
        tooltip.add(Component.translatable("tooltip.iska_utils." + path + ".cursed"));
        switch (path) {
            case "totem_of_pain" -> ArtifactTooltipUtil.appendDescLines(
                    tooltip, path, 2, 2, ArtifactBalanceFormat.percent(Config.totemOfPainProcChance));
            case "busted_crown" -> ArtifactTooltipUtil.appendDescLines(
                    tooltip, path, 2, 2, ArtifactBalanceFormat.flatBonus(Config.bustedCrownHpPerCursedArtifact));
            case "ritual_gauntlet" -> {
                String prefix = "tooltip.iska_utils.ritual_gauntlet.";
                ArtifactTooltipUtil.addLoreLine(tooltip::add, prefix + "desc0");
                ArtifactTooltipUtil.addLoreLine(tooltip::add, prefix + "desc1");
                ArtifactTooltipUtil.addTechLine(tooltip::add, prefix + "desc2");
                ArtifactTooltipUtil.addTechLine(tooltip::add, prefix + "desc3",
                        ArtifactBalanceFormat.percent(Config.ritualGauntletCritChance));
                ArtifactTooltipUtil.addTechLine(tooltip::add, prefix + "desc4",
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
            case "entropic_ring" -> {
                ArtifactTooltipUtil.addLoreLine(tooltip::add, "tooltip.iska_utils.entropic_ring.desc0");
                Player player = ClientPlayerAccess.getLocalPlayer();
                ArtifactTooltipUtil.addTechLine(tooltip::add, "tooltip.iska_utils.entropic_ring.desc3",
                        ArtifactBalanceFormat.flatBonus(ApotheosisCompat.getEffectiveDamagePer100Hp(player)));
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    ApotheosisCompat.WorldTierInfo tierInfo = ApotheosisCompat.getWorldTierInfo(player);
                    if (tierInfo != null) {
                        ArtifactTooltipUtil.addTechLine(tooltip::add, "tooltip.iska_utils.entropic_ring.apotheosis",
                                tierInfo.displayName(), tierInfo.ringPowerMultiplier());
                    }
                }
            }
            default -> ArtifactTooltipUtil.appendDescLines(tooltip, path, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
        appendCursedArtifactTooltip(tooltip, id.getPath());
    }
}
