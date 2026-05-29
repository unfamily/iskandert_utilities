package net.unfamily.iskautils.item.entropic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Unbreakable;

import java.util.List;

/**
 * Shared item properties for entropic gear (1.21.1).
 */
public final class EntropicGear {
    private EntropicGear() {}

    public static Item.Properties baseProperties() {
        return new Item.Properties()
                .fireResistant()
                .rarity(Rarity.EPIC)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true));
    }

    public static Item.Properties armorProperties() {
        return baseProperties().stacksTo(1);
    }

    public static Item.Properties swordProperties() {
        return baseProperties().attributes(SwordItem.createAttributes(EntropicTier.INSTANCE, 5, -2.4F));
    }

    public static Item.Properties pickaxeProperties() {
        return baseProperties().attributes(PickaxeItem.createAttributes(EntropicTier.INSTANCE, 1.0F, -2.8F));
    }

    public static Item.Properties axeProperties() {
        return baseProperties().attributes(AxeItem.createAttributes(EntropicTier.INSTANCE, 7.0F, -3.0F));
    }

    public static Item.Properties shovelProperties() {
        return baseProperties().attributes(ShovelItem.createAttributes(EntropicTier.INSTANCE, 1.5F, -3.0F));
    }

    public static Item.Properties hoeProperties() {
        return baseProperties().attributes(HoeItem.createAttributes(EntropicTier.INSTANCE, -4.0F, 0.0F));
    }

    public static Tool createPaxelTool() {
        float speed = EntropicTier.INSTANCE.getSpeed();
        return new Tool(
                List.of(
                        Tool.Rule.deniesDrops(EntropicTier.INSTANCE.getIncorrectBlocksForDrops()),
                        Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, speed),
                        Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, speed),
                        Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, speed)),
                1.0F,
                1);
    }

    public static ItemAttributeModifiers paxelAttributes() {
        return DiggerItem.createAttributes(EntropicTier.INSTANCE, 1.0F, -2.8F);
    }
}
