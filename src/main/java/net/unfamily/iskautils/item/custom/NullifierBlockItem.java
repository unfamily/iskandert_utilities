package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.unfamily.iskautils.Config;

import java.util.List;

public class NullifierBlockItem extends BlockItem {
    private final String tooltipKey;

    public NullifierBlockItem(Block block, Properties properties, String tooltipKey) {
        super(block, properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils." + tooltipKey + ".effect"));
        tooltip.add(Component.translatable("tooltip.iska_utils." + tooltipKey + ".radius", Config.enderNullifierRadius));
    }
}
