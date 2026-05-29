package net.unfamily.iskautils.item.entropic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class EntropicShovelItem extends ShovelItem {
    public EntropicShovelItem(Properties properties) {
        super(EntropicTier.INSTANCE, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = EntropicInteractions.onShovelUseOn(context);
        return result != InteractionResult.PASS ? result : super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        EntropicTooltip.appendToolLines(tooltip, "entropic_shovel");
    }
}
