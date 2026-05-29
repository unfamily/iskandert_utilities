package net.unfamily.iskautils.item.entropic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class EntropicShovelItem extends ShovelItem {
    public EntropicShovelItem(Properties properties) {
        super(EntropicTier.INSTANCE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.entropic.unbreakable"));
    }
}
