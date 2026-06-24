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
 * Ancient Star artifact for the Curios curio slot.
 */
public class AncientStarItem extends Item {
    public AncientStarItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        ArtifactTooltipUtil.addLoreLine(tooltip, "tooltip.iska_utils.ancient_star.desc0");
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.ancient_star.desc2",
                ArtifactBalanceFormat.flatBonus(Config.ancientStarArmorBonus));
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.ancient_star.desc3",
                ArtifactBalanceFormat.percent(Config.ancientStarHighHpRatio),
                ArtifactBalanceFormat.flatBonus(Config.ancientStarDamageBonus));
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.ancient_star.desc4",
                ArtifactBalanceFormat.percent(Config.ancientStarLowHpRatio),
                ArtifactBalanceFormat.flatBonus(Config.ancientStarToughnessBonus));
    }
}
