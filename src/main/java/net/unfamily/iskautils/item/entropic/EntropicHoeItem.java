package net.unfamily.iskautils.item.entropic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class EntropicHoeItem extends HoeItem {
    public EntropicHoeItem(Properties properties) {
        super(EntropicTier.INSTANCE, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = EntropicInteractions.onHoeUseOn(context);
        return result != InteractionResult.PASS ? result : super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        EntropicTooltip.appendToolLines(tooltip, "entropic_hoe");
    }
}
