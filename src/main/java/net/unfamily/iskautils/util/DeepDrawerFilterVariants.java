package net.unfamily.iskautils.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Deep Drawer–style filter variant generation for Extractor and Pattern Crafter editors.
 */
public final class DeepDrawerFilterVariants {
    private DeepDrawerFilterVariants() {}

    /**
     * Order: item id, mod id (non-minecraft), &enchanted, &damaged, tags, optional ?SNBT.
     */
    public static List<String> generateAllFilterVariants(
            @NotNull ItemStack stack, @Nullable HolderLookup.Provider registryAccess) {
        List<String> variants = new ArrayList<>();
        if (stack.isEmpty()) {
            return variants;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return variants;
        }

        variants.add("-" + itemId);

        String namespace = itemId.getNamespace();
        if (!"minecraft".equals(namespace)) {
            variants.add("@" + namespace);
        }
        if (stack.isEnchanted()) {
            variants.add("&enchanted");
        }
        if (stack.isDamaged()) {
            variants.add("&damaged");
        }

        var itemHolder = BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
        BuiltInRegistries.ITEM.getTags()
                .filter(named -> named.contains(itemHolder))
                .map(named -> named.key().location().toString())
                .sorted()
                .forEach(tagId -> variants.add("#" + tagId));

        if (registryAccess != null) {
            try {
                Tag encoded = ItemStack.CODEC
                        .encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack)
                        .getOrThrow();
                String snbt = encoded.toString();
                if (!snbt.isEmpty()) {
                    variants.add("?" + snbt);
                }
            } catch (Exception ignored) {
            }
        }
        return variants;
    }
}
