package net.unfamily.iskautils.item.entropic;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

/**
 * Pickaxe + axe + shovel in one indestructible tool.
 */
public class EntropicPaxelItem extends Item {
    public EntropicPaxelItem(Item.Properties properties) {
        super(properties
                .component(DataComponents.TOOL, createPaxelTool())
                .attributes(EntropicGear.paxelAttributes())
                .component(DataComponents.WEAPON, new Weapon(2, 0.0F)));
    }

    private static Tool createPaxelTool() {
        HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        float speed = EntropicGear.TIER.speed();
        return new Tool(
                List.of(
                        Tool.Rule.deniesDrops(blocks.getOrThrow(EntropicGear.TIER.incorrectBlocksForDrops())),
                        Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), speed),
                        Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_AXE), speed),
                        Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), speed)),
                1.0F,
                1,
                true);
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.entropic.unbreakable"));
    }
}
