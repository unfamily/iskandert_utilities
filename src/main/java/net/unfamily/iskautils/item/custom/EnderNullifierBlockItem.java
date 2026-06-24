package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.unfamily.iskautils.Config;

import java.util.List;

/**
 * Custom BlockItem for Ender Nullifier to show config-driven range in tooltip.
 */
public class EnderNullifierBlockItem extends BlockItem {

    public EnderNullifierBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.ender_nullifier.radius", Config.enderNullifierRadius));
    }
}
