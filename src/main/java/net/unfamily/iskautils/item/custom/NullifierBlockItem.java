package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.unfamily.iskautils.Config;

import java.util.function.Consumer;

public class NullifierBlockItem extends BlockItem {
    private final String tooltipKey;

    public NullifierBlockItem(Block block, Properties properties, String tooltipKey) {
        super(block, properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils." + tooltipKey + ".effect"));
        int radius = switch (tooltipKey) {
            case "wander_nullifier" -> Config.wanderNullifierRadius;
            case "soul_nullifier"   -> Config.soulNullifierRadius;
            default                 -> Config.enderNullifierRadius;
        };
        tooltip.accept(Component.translatable("tooltip.iska_utils." + tooltipKey + ".radius", radius));
    }
}
