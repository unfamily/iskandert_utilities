package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.custom.BurningBrazierItem;
import java.util.function.Consumer;

/**
 * Arcane Candle - eternal brazier extension: cursed burning flames, no durability.
 * Unlike other artifacts, it is fully usable from inventory or hands (not Curios-only).
 * Player ignite on place uses the same rules as {@link BurningBrazierItem}
 * ({@code burning_brazier_super_hot} or {@code iska_utils_internal-curse_flame} stage).
 */
public class CursedCandleItem extends BurningBrazierItem {
    private static final String TOOLTIP_PATH = "cursed_candle";

    public CursedCandleItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Block getFlameBlock() {
        return ModBlocks.CURSED_BURNING_FLAME.get();
    }

    @Override
    protected boolean consumesDurability() {
        return false;
    }

    @Override
    protected String flamesTooltipPath() {
        return TOOLTIP_PATH;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.iska_utils." + TOOLTIP_PATH + ".cursed"));
        appendFlameTooltip(tooltip);
    }
}
