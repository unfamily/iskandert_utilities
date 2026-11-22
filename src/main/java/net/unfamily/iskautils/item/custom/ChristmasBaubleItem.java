package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.List;

/**
 * Seasonal Christmas bauble curios (charm/ring). Effects apply only while Curio-equipped.
 */
public class ChristmasBaubleItem extends Item {
    private final String tooltipKey;

    public ChristmasBaubleItem(Properties properties, String tooltipKey) {
        super(properties.stacksTo(1));
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ArtifactTooltipUtil.addLoreLine(tooltip::add, "tooltip.iska_utils." + tooltipKey + ".desc0");
        ArtifactTooltipUtil.addTechLine(tooltip::add, "tooltip.iska_utils." + tooltipKey + ".desc1");
    }
}