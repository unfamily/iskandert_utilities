package net.unfamily.iskautils.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.function.Consumer;

/**
 * Suspicious delivery shears: 512 durability, intrinsic silk touch, shear-harvest blocks only.
 */
public class DurableShearsItem extends ShearsItem {
    public static final int MAX_DURABILITY = 512;

    public DurableShearsItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .durability(MAX_DURABILITY)
                .component(DataComponents.TOOL, ShearsItem.createToolProperties()));
    }

    public static boolean isShearHarvestable(BlockState state) {
        return state.is(BlockTags.WOOL)
                || state.is(BlockTags.LEAVES)
                || state.is(Blocks.COBWEB)
                || state.is(Blocks.VINE)
                || state.is(Blocks.GLOW_LICHEN);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        ArtifactTooltipUtil.appendDescLines(tooltip, "durable_shears", 0);
    }
}
