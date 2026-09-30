package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.List;

/**
 * Lasting Candy — seasonal Halloween curio (Oct 26–Nov 5).
 * Cancels incoming damage briefly, then enters a cooldown.
 */
public class LastingCandyItem extends Item {

    public LastingCandyItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ArtifactTooltipUtil.addLoreLine(tooltip::add, "tooltip.iska_utils.lasting_candy.desc0");
        ArtifactTooltipUtil.addTechLine(tooltip::add, "tooltip.iska_utils.lasting_candy.desc1");
        ArtifactTooltipUtil.addTechLine(tooltip::add, "tooltip.iska_utils.lasting_candy.desc2",
                Config.lastingCandyInvulnSeconds,
                Config.lastingCandyCooldownSeconds);
        tooltip.add(Component.translatable("tooltip.iska_utils.lasting_candy.availability"));
    }
}
