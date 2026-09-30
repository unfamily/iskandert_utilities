package net.unfamily.iskautils.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Derives GUI / menu display stacks from Deep Drawer–style filter strings.
 * Authoritative state is always the string filter; never orphan {@link ItemStack} snapshots.
 */
public final class FilterDisplayItems {
    private FilterDisplayItems() {}

    public static ItemStack forFilter(@Nullable String filter, @Nullable HolderLookup.Provider registries) {
        if (filter == null || filter.isBlank()) {
            return ItemStack.EMPTY;
        }
        String trimmed = filter.trim();

        if (trimmed.startsWith("-")) {
            return DeepDrawerFilterVariants.itemStackFromIdFilter(trimmed);
        }

        if (trimmed.startsWith("#")) {
            return sampleItemForTag(trimmed.substring(1));
        }

        if (trimmed.startsWith("@")) {
            return sampleItemForMod(trimmed.substring(1));
        }

        if (trimmed.startsWith("?")) {
            return new ItemStack(Items.KNOWLEDGE_BOOK);
        }

        if (trimmed.startsWith("&")) {
            return macroPreview(trimmed.substring(1).trim().toLowerCase());
        }

        if (trimmed.startsWith("minecraft:enchanted_book[")) {
            return new ItemStack(Items.ENCHANTED_BOOK);
        }

        try {
            ResourceLocation id = ResourceLocation.parse(trimmed);
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception ignored) {
        }

        if (registries != null && DeepDrawerItemFilter.usesTypedFilterSyntax(trimmed)) {
            int checked = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (DeepDrawerItemFilter.matchesFilterEntry(stack, trimmed, registries)) {
                    return stack;
                }
                if (++checked > 512) {
                    break;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack macroPreview(String macro) {
        if (macro.equals("enchanted") || macro.startsWith("enchanted")) {
            return new ItemStack(Items.DIAMOND_PICKAXE);
        }
        if (macro.equals("damaged") || macro.startsWith("damaged")) {
            ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
            stack.setDamageValue(stack.getMaxDamage() / 2);
            return stack;
        }
        if (macro.startsWith("temperature")) {
            return new ItemStack(Items.BLAZE_POWDER);
        }
        if (macro.startsWith("light")) {
            return new ItemStack(Items.LANTERN);
        }
        if (macro.startsWith("tint")) {
            return new ItemStack(Items.RED_DYE);
        }
        return new ItemStack(Items.KNOWLEDGE_BOOK);
    }

    private static ItemStack sampleItemForTag(String tagId) {
        try {
            ResourceLocation tagLocation = ResourceLocation.parse(tagId);
            var itemTag = ItemTags.create(tagLocation);
            List<Item> items = new ArrayList<>();
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(itemTag)) {
                items.add(holder.value());
            }
            if (!items.isEmpty()) {
                int index = (int) ((System.currentTimeMillis() / 3500L) % items.size());
                return new ItemStack(items.get(index));
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack sampleItemForMod(String modId) {
        List<Item> modItems = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null && itemId.getNamespace().startsWith(modId)) {
                modItems.add(item);
            }
        }
        if (!modItems.isEmpty()) {
            int index = (int) ((System.currentTimeMillis() / 3500L) % modItems.size());
            return new ItemStack(modItems.get(index));
        }
        return ItemStack.EMPTY;
    }
}
