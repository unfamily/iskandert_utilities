package net.unfamily.iskautils.item.entropic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Consumer;

public class EntropicHoeItem extends HoeItem {
    public EntropicHoeItem(Properties properties) {
        super(EntropicGear.TIER, -2.0F, -3.0F, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = EntropicInteractions.onHoeUseOn(context);
        return result != InteractionResult.PASS ? result : super.useOn(context);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        EntropicTooltip.appendToolLines(tooltip, "entropic_hoe");
    }
}
