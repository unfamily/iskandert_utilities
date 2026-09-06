package net.unfamily.iskautils.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
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
        BuiltInRegistries.ITEM.getTagNames()
                .filter(tagKey -> {
                    var tag = BuiltInRegistries.ITEM.getTag(tagKey);
                    return tag.isPresent() && tag.get().contains(itemHolder);
                })
                .map(tagKey -> tagKey.location().toString())
                .sorted()
                .forEach(tagId -> variants.add("#" + tagId));

        if (registryAccess != null) {
            try {
                var saved = stack.save(registryAccess);
                if (saved instanceof CompoundTag compound) {
                    String snbt = compound.toString();
                    if (!snbt.isEmpty()) {
                        variants.add("?" + snbt);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return variants;
    }
}
