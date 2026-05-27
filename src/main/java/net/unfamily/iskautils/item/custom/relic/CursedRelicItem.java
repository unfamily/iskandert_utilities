package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.RelicBalanceFormat;
import net.unfamily.iskautils.util.RelicTooltipUtil;

import java.util.List;

/**
 * Base class for cursed relics.
 * Concrete effects are implemented elsewhere (events / keybind integration).
 */
public class CursedRelicItem extends Item {

    public CursedRelicItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void appendCursedArtifactTooltip(List<Component> tooltip, String path) {
        tooltip.add(Component.translatable("tooltip.iska_utils." + path + ".cursed"));
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
            default -> RelicTooltipUtil.appendDescLines(tooltip, path);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
        appendCursedArtifactTooltip(tooltip, id.getPath());
    }
}

