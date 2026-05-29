package net.unfamily.iskautils.item.entropic;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

public class EntropicSmithingTemplateItem extends SmithingTemplateItem {
    private static final Component APPLIES_TO = Component.translatable(
            Util.makeDescriptionId("item", ResourceLocation.fromNamespaceAndPath("iska_utils", "smithing_template.entropic_upgrade.applies_to")));
    private static final Component INGREDIENTS = Component.translatable(
            Util.makeDescriptionId("item", ResourceLocation.fromNamespaceAndPath("iska_utils", "smithing_template.entropic_upgrade.ingredients")));
    private static final Component UPGRADE = Component.translatable(
            Util.makeDescriptionId("upgrade", ResourceLocation.fromNamespaceAndPath("iska_utils", "entropic_upgrade")));
    private static final Component BASE_SLOT = Component.translatable(
            Util.makeDescriptionId("item", ResourceLocation.fromNamespaceAndPath("iska_utils", "smithing_template.entropic_upgrade.base_slot_description")));
    private static final Component ADDITIONS_SLOT = Component.translatable(
            Util.makeDescriptionId("item", ResourceLocation.fromNamespaceAndPath("iska_utils", "smithing_template.entropic_upgrade.additions_slot_description")));

    public EntropicSmithingTemplateItem(Item.Properties properties) {
        super(APPLIES_TO, INGREDIENTS, UPGRADE, BASE_SLOT, ADDITIONS_SLOT, ICONS, MATERIAL_ICONS);
    }

    private static final List<ResourceLocation> ICONS = List.of(
            ResourceLocation.withDefaultNamespace("container/slot/helmet"),
            ResourceLocation.withDefaultNamespace("container/slot/sword"),
            ResourceLocation.withDefaultNamespace("container/slot/chestplate"),
            ResourceLocation.withDefaultNamespace("container/slot/pickaxe"),
            ResourceLocation.withDefaultNamespace("container/slot/leggings"),
            ResourceLocation.withDefaultNamespace("container/slot/axe"),
            ResourceLocation.withDefaultNamespace("container/slot/boots"),
            ResourceLocation.withDefaultNamespace("container/slot/hoe"),
            ResourceLocation.withDefaultNamespace("container/slot/shovel"));

    private static final List<ResourceLocation> MATERIAL_ICONS = List.of(
            ResourceLocation.withDefaultNamespace("container/slot/emerald"));

    public static Item.Properties defaultProperties() {
        return EntropicGear.baseProperties().stacksTo(64);
    }
}
