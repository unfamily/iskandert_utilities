package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Old Brick relic.
 * While equipped in Curios, grants +2 armor (via {@link net.unfamily.iskautils.events.RelicTickEffects}).
 */
public class OldBrickItem extends Item {
    public OldBrickItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.old_brick.desc0"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.old_brick.desc1"));
    }
}
