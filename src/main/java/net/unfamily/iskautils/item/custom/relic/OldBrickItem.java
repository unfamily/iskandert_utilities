package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.RelicBalanceFormat;
import net.unfamily.iskautils.util.RelicTooltipUtil;

import java.util.List;

/**
 * Old Brick relic.
 * While equipped in Curios, grants +2 armor (via {@link net.unfamily.iskautils.events.RelicTickEffects}).
 */
public class OldBrickItem extends Item {
    public OldBrickItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        RelicTooltipUtil.appendDescLines(tooltip, "old_brick", 3, RelicBalanceFormat.flatBonus(Config.oldBrickArmorBonus));
    }
}
