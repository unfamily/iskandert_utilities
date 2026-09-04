package net.unfamily.iskautils.shop.edit;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.iskalib.item.ItemConverter;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

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
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
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
        BuiltInRegistries.ITEM.getTags()
                .filter(named -> named.contains(itemHolder))
                .map(named -> "#" + named.key().location())
                .sorted()
                .forEach(tag -> {
                    if (!variants.contains(tag)) {
                        variants.add(tag);
                    }
                });
        return variants;
    }

    /**
     * Variants for a fluid selector: plain id plus fluid tags that contain the member.
     * If {@code current} is a {@code #tag}, it is included and a sample member is resolved when possible.
     */
    public static List<String> variantsFromFluid(@Nullable String current) {
        List<String> variants = new ArrayList<>();
        if (current == null || current.isBlank()) {
            return variants;
        }
        String trimmed = current.trim();
        Fluid member = null;
        if (trimmed.startsWith("#")) {
            if (!variants.contains(trimmed)) {
                variants.add(trimmed);
            }
            List<Fluid> fromTag = ShopEntryHelper.fluidsFromTag(trimmed);
            if (!fromTag.isEmpty()) {
                member = fromTag.get(0);
            }
        } else {
            member = ShopEntryHelper.resolveFluid(trimmed);
        }
        if (member != null && member != Fluids.EMPTY) {
            Identifier id = BuiltInRegistries.FLUID.getKey(member);
            if (id != null) {
                String plain = id.toString();
                if (!variants.contains(plain)) {
                    if (trimmed.startsWith("#")) {
                        variants.add(plain);
                    } else {
                        variants.add(0, plain);
                    }
                }
            }
            var fluidHolder = BuiltInRegistries.FLUID.wrapAsHolder(member);
            BuiltInRegistries.FLUID.getTags()
                    .filter(named -> named.contains(fluidHolder))
                    .map(named -> "#" + named.key().location())
                    .sorted()
                    .forEach(tag -> {
                        if (!variants.contains(tag)) {
                            variants.add(tag);
                        }
                    });
        } else if (!trimmed.startsWith("#") && !variants.contains(trimmed)) {
            variants.add(trimmed);
        }
        return variants;
    }

    /**
     * Variants for a gas/chemical selector: plain id plus chemical tags that contain the member when Mekanism is present.
     * If {@code current} is a {@code #tag}, it is included (sample member resolution is best-effort).
     */
    public static List<String> variantsFromGas(@Nullable String current) {
        List<String> variants = new ArrayList<>();
        if (current == null || current.isBlank()) {
            return variants;
        }
        String trimmed = current.trim();
        if (trimmed.startsWith("#")) {
            variants.add(trimmed);
            return variants;
        }
        variants.add(trimmed);
        if (!MekChemicalHelper.isLoaded()) {
            return variants;
        }
        for (String tag : chemicalTagsContaining(trimmed)) {
            if (!variants.contains(tag)) {
                variants.add(tag);
            }
        }
        return variants;
    }

    private static List<String> chemicalTagsContaining(String chemicalId) {
        List<String> tags = new ArrayList<>();
        try {
            Object stack = MekChemicalHelper.createStackFromId(chemicalId, 1L);
            if (stack == null || MekChemicalHelper.isEmpty(stack)) {
                return tags;
            }
            Object holder = resolveChemicalHolder(stack);
            if (holder == null) {
                return tags;
            }
            Object tagsObj = holder.getClass().getMethod("tags").invoke(holder);
            java.util.stream.Stream<?> stream;
            if (tagsObj instanceof java.util.stream.Stream<?> s) {
                stream = s;
            } else if (tagsObj instanceof Iterable<?> iterable) {
                stream = StreamSupport.stream(iterable.spliterator(), false);
            } else {
                return tags;
            }
            stream
                    .map(tagKey -> {
                        try {
                            Object location = tagKey.getClass().getMethod("location").invoke(tagKey);
                            return location != null ? "#" + location : null;
                        } catch (Throwable ignored) {
                            return null;
                        }
                    })
                    .filter(s -> s != null && !s.isBlank())
                    .sorted()
                    .forEach(tags::add);
        } catch (Throwable ignored) {
        }
        return tags;
    }

    @Nullable
    private static Object resolveChemicalHolder(Object chemicalStack) {
        try {
            return chemicalStack.getClass().getMethod("getChemicalHolder").invoke(chemicalStack);
        } catch (Throwable ignored) {
        }
        try {
            Object chemical = chemicalStack.getClass().getMethod("getChemical").invoke(chemicalStack);
            if (chemical == null) {
                return null;
            }
            try {
                return chemical.getClass().getMethod("getAsHolder").invoke(chemical);
            } catch (Throwable ignored) {
                return chemical;
            }
        } catch (Throwable ignored) {
            return null;
        }
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
