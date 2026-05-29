package net.unfamily.iskautils.item.entropic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.EntropicGearUtil;

import java.util.function.Consumer;

/** Generic entropic tool wrapper with shared interactions and tooltips. */
public class EntropicToolItem extends Item {
    public EntropicToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (EntropicGearUtil.isEntropicAxeTool(stack)) {
            InteractionResult result = EntropicInteractions.onAxeUse(level, player, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (stack.is(ModItems.ENTROPIC_SHOVEL.get())) {
            InteractionResult result = EntropicInteractions.onShovelUseOn(context);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        EntropicTooltip.appendToolLines(tooltip, BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
    }
}
