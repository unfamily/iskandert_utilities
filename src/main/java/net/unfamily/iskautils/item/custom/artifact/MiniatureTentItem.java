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

public class MiniatureTentItem extends Item {
    public MiniatureTentItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.miniature_tent.cursed"));
        ArtifactTooltipUtil.addLoreLine(tooltip, "tooltip.iska_utils.miniature_tent.desc0");
        ArtifactTooltipUtil.addTechLine(
                tooltip,
                "tooltip.iska_utils.miniature_tent.desc1",
                ArtifactBalanceFormat.multiplier(Config.miniatureTentProcMultiplier));
    }
}
