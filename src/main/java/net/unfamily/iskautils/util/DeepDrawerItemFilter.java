package net.unfamily.iskautils.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared item matching rules for Deep Drawer GUI search and Deep Drawer Extractor filters.
 * <p>
 * Without a typed prefix ({@code - @ & # ?}), search uses display name (case-insensitive contains).
 * With a prefix, the same rules as the extractor apply.
 */
public final class DeepDrawerItemFilter {

    private DeepDrawerItemFilter() {
    }

    public static boolean usesTypedFilterSyntax(@NotNull String query) {
        if (query.isEmpty()) {
            return false;
        }
        return switch (query.charAt(0)) {
            case '-', '@', '&', '#', '?' -> true;
            default -> false;
        };
    }

    /**
     * Deep Drawers GUI search: name contains when untyped; extractor syntax when typed.
     */
    public static boolean matchesSearch(
            @NotNull ItemStack stack,
            @NotNull String query,
            @Nullable HolderLookup.Provider registryAccess) {
        if (stack.isEmpty()) {
            return false;
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (usesTypedFilterSyntax(trimmed)) {
            return matchesFilterEntry(stack, trimmed, registryAccess);
        }
        return stack.getHoverName().getString().toLowerCase().contains(trimmed.toLowerCase());
    }

    /**
     * Single extractor-style filter entry (trimmed).
     */
    public static boolean matchesFilterEntry(
            @NotNull ItemStack stack,
            @NotNull String filter,
            @Nullable HolderLookup.Provider registryAccess) {
        if (stack.isEmpty() || filter.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return false;
        }
        return matchesFilterEntry(stack, item, itemId, itemId.toString(), itemId.getNamespace(), filter, registryAccess);
    }

    public static boolean matchesFilterEntry(
            @NotNull ItemStack stack,
            @NotNull Item item,
            @NotNull Identifier itemId,
            @NotNull String itemIdStr,
            @NotNull String itemModId,
            @NotNull String filter,
            @Nullable HolderLookup.Provider registryAccess) {
        if (filter.isEmpty()) {
            return false;
        }

        if (filter.startsWith("-")) {
            return itemIdStr.equals(filter.substring(1));
        }

        if (filter.startsWith("@")) {
            return itemModId.startsWith(filter.substring(1));
        }

        if (filter.startsWith("&")) {
            String macroFilter = filter.substring(1).toLowerCase();
            return switch (macroFilter) {
                case "enchanted" -> stack.isEnchanted();
                case "damaged" -> stack.isDamaged();
                default -> false;
            };
        }

        if (filter.startsWith("#")) {
            String tagFilter = filter.substring(1);
            try {
                Identifier tagId = Identifier.parse(tagFilter);
                TagKey<Item> itemTag = ItemTags.create(tagId);
                return item.builtInRegistryHolder().is(itemTag);
            } catch (Exception ignored) {
                return false;
            }
        }

        if (filter.startsWith("?")) {
            String nbtFilter = filter.substring(1);
            if (registryAccess == null) {
                return false;
            }
            try {
                var ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
                var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
                return encoded.toString().contains(nbtFilter);
            } catch (Exception ignored) {
                return false;
            }
        }

        return itemIdStr.equals(filter);
    }
}
