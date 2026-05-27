package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;

/**
 * Cursed Crystal Heart.
 * Placeholder item used as Suspicious Delivery drop; cursed behavior can be implemented later.
 */
public class CursedCrystalHeartItem extends Item {
    public CursedCrystalHeartItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> output, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, output, flag);
        output.accept(Component.translatable("tooltip.iska_utils.cursed_crystal_heart.desc0"));
    }
}

