package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.List;

/**
 * Ice Diamond artifact.
 * Repairs items over time while consuming its own durability when carried in inventory, hands, or Curios.
 */
public class IceDiamondItem extends Item {
    public IceDiamondItem(Properties properties) {
        super(properties.stacksTo(1).durability(1024));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ArtifactTooltipUtil.appendDescLines(tooltip, "ice_diamond", 2);
    }
}
