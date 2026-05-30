package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactBalanceFormat;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.function.Consumer;

/**
 * Old Brick artifact.
 * While equipped in Curios, grants +2 armor (via {@link net.unfamily.iskautils.events.ArtifactTickEffects}).
 */
public class OldBrickItem extends Item {
    public OldBrickItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        ArtifactTooltipUtil.appendDescLines(tooltip, "old_brick", 3, 3, ArtifactBalanceFormat.flatBonus(Config.oldBrickArmorBonus));
    }
}
