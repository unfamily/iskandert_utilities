package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactBalanceFormat;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.List;

public class MiniatureTentItem extends ArcaneArtifactItem {
    public MiniatureTentItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.iska_utils.miniature_tent.cursed"));
        ArtifactTooltipUtil.addLoreLine(tooltip::add, "tooltip.iska_utils.miniature_tent.desc0");
        ArtifactTooltipUtil.addTechLine(
                tooltip::add,
                "tooltip.iska_utils.miniature_tent.desc1",
                ArtifactBalanceFormat.multiplier(Config.miniatureTentProcMultiplier));
    }
}
