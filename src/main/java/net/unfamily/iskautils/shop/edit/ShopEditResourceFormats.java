package net.unfamily.iskautils.shop.edit;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskalib.item.ItemConverter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds shop JSON resource selector variants: {@code id} | {@code id[components]} | {@code #tag}.
 */
public final class ShopEditResourceFormats {

    private ShopEditResourceFormats() {}

    public static List<String> variantsFromStack(ItemStack stack) {
        List<String> variants = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return variants;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return variants;
        }
        String plain = itemId.toString();
        variants.add(plain);

        HolderLookup.Provider registries = registries();
        if (registries != null && !stack.getComponentsPatch().isEmpty()) {
            String compound = ItemConverter.formatAsKubeJsItemString(stack, registries);
            if (compound != null && !compound.isBlank() && !compound.equals(plain) && !variants.contains(compound)) {
                variants.add(compound);
            }
        }

        var itemHolder = BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
        BuiltInRegistries.ITEM.getTagNames()
                .filter(tagKey -> {
                    var tag = BuiltInRegistries.ITEM.getTag(tagKey);
                    return tag.isPresent() && tag.get().contains(itemHolder);
                })
                .map(tagKey -> "#" + tagKey.location())
                .sorted()
                .forEach(tag -> {
                    if (!variants.contains(tag)) {
                        variants.add(tag);
                    }
                });
        return variants;
    }

    public static int indexOfOrZero(List<String> variants, @Nullable String current) {
        if (current == null || current.isBlank() || variants.isEmpty()) {
            return 0;
        }
        int idx = variants.indexOf(current);
        return idx >= 0 ? idx : 0;
    }

    public static String preferredFromStack(ItemStack stack) {
        List<String> variants = variantsFromStack(stack);
        if (variants.isEmpty()) {
            return "";
        }
        if (variants.size() > 1 && !stack.getComponentsPatch().isEmpty()) {
            return variants.get(1);
        }
        return variants.get(0);
    }

    @Nullable
    private static HolderLookup.Provider registries() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.registryAccess();
        }
        if (mc.getConnection() != null) {
            return mc.getConnection().registryAccess();
        }
        return null;
    }
}
