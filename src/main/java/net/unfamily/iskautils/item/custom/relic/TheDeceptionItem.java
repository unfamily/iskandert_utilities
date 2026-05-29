package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * The Deception: cursed Curio relic and placeable chair block.
 */
public class TheDeceptionItem extends BlockItem {
    public TheDeceptionItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        CursedRelicItem.appendCursedArtifactTooltip(tooltip, "the_deception");
    }
}
