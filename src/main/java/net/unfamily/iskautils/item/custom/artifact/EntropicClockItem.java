package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.unfamily.iskautils.item.custom.UpgradeModuleTooltipHelper;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.List;

/**
 * Temporal Overclocker and Entropic Spawner upgrade item.
 */
public class EntropicClockItem extends Item {
    public EntropicClockItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ArtifactTooltipUtil.addLoreLine(tooltip::add, "tooltip.iska_utils.entropic_clock.desc0");
        UpgradeModuleTooltipHelper.appendEntropicClockTooltip(tooltip, flag);
    }
}
