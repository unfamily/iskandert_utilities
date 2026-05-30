package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.List;

/**
 * Rough ring base for entropic smithing. Obtained from Suspicious Delivery only.
 */
public class CoarselyForgedRingItem extends Item {
    public CoarselyForgedRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ArtifactTooltipUtil.appendDescLines(tooltip, "coarsely_forged_ring", 3);
    }
}
