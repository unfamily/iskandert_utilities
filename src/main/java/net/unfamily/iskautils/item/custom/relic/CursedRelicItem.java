package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.RelicBalanceFormat;
import net.unfamily.iskautils.util.RelicTooltipUtil;

import java.util.function.Consumer;

/**
 * Base class for cursed relics.
 * Concrete effects are implemented elsewhere (events / keybind integration).
 */
public class CursedRelicItem extends Item {

    public CursedRelicItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void appendCursedArtifactTooltip(Consumer<Component> tooltip, String path) {
        tooltip.accept(Component.translatable("tooltip.iska_utils." + path + ".cursed"));
        switch (path) {
            case "totem_of_pain" -> RelicTooltipUtil.appendDescLines(
                    tooltip, path, 2, RelicBalanceFormat.percent(Config.totemOfPainProcChance));
            case "busted_crown" -> RelicTooltipUtil.appendDescLines(
                    tooltip, path, 2, RelicBalanceFormat.flatBonus(Config.bustedCrownHpPerCursedRelic));
            case "ritual_gauntlet" -> RelicTooltipUtil.appendDescLines(
                    tooltip,
                    path,
                    2,
                    RelicBalanceFormat.percent(Config.ritualGauntletCritChance),
                    RelicBalanceFormat.percentBonusFromMultiplier(Config.ritualGauntletCritDamageMultiplier));
            case "necrotic_crystal_heart" -> RelicTooltipUtil.appendDescLines(
                    tooltip, path, 4, RelicBalanceFormat.flatBonus(Config.necroticCrystalHeartHpCostPerSave));
            default -> RelicTooltipUtil.appendDescLines(tooltip, path);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        appendCursedArtifactTooltip(tooltip, id.getPath());
    }
}

