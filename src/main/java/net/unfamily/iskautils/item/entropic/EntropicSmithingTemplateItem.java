package net.unfamily.iskautils.item.entropic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

/**
 * Smithing template for upgrading netherite gear with entropy crystal.
 */
public class EntropicSmithingTemplateItem extends SmithingTemplateItem {
    private static final Component APPLIES_TO = Component.translatable(
            Util.makeDescriptionId("item", Identifier.fromNamespaceAndPath("iska_utils", "smithing_template.entropic_upgrade.applies_to")));
    private static final Component INGREDIENTS = Component.translatable(
            Util.makeDescriptionId("item", Identifier.fromNamespaceAndPath("iska_utils", "smithing_template.entropic_upgrade.ingredients")));
    private static final Component BASE_SLOT = Component.translatable(
            Util.makeDescriptionId("item", Identifier.fromNamespaceAndPath("iska_utils", "smithing_template.entropic_upgrade.base_slot_description")));
    private static final Component ADDITIONS_SLOT = Component.translatable(
            Util.makeDescriptionId("item", Identifier.fromNamespaceAndPath("iska_utils", "smithing_template.entropic_upgrade.additions_slot_description")));

    public EntropicSmithingTemplateItem(Item.Properties properties) {
        super(APPLIES_TO, INGREDIENTS, BASE_SLOT, ADDITIONS_SLOT, ICONS, MATERIAL_ICONS, properties);
    }

    private static final List<Identifier> ICONS = List.of(
            Identifier.withDefaultNamespace("container/slot/helmet"),
            Identifier.withDefaultNamespace("container/slot/sword"),
            Identifier.withDefaultNamespace("container/slot/chestplate"),
            Identifier.withDefaultNamespace("container/slot/pickaxe"),
            Identifier.withDefaultNamespace("container/slot/leggings"),
            Identifier.withDefaultNamespace("container/slot/axe"),
            Identifier.withDefaultNamespace("container/slot/boots"),
            Identifier.withDefaultNamespace("container/slot/hoe"),
            Identifier.withDefaultNamespace("container/slot/shovel"),
            Identifier.withDefaultNamespace("container/slot/nautilus_armor"),
            Identifier.withDefaultNamespace("container/slot/spear"));

    private static final List<Identifier> MATERIAL_ICONS = List.of(
            Identifier.withDefaultNamespace("container/slot/emerald"));

    public static Item.Properties defaultProperties() {
        return EntropicGear.baseProperties().stacksTo(64);
    }
}
