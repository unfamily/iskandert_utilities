package net.unfamily.iskautils.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.unfamily.iskalib.item.ItemConverter;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.util.ModLogger;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers for typed shop entries (item / fluid / gas) and {@code #tag} selectors.
 */
public final class ShopEntryHelper {
    private static final ModLogger LOGGER = ModLogger.of(ShopEntryHelper.class);

    private ShopEntryHelper() {}

    public static ShopEntry.EntryType parseType(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return ShopEntry.EntryType.ITEM;
        }
        return switch (raw.trim().toLowerCase()) {
            case "fluid" -> ShopEntry.EntryType.FLUID;
            case "gas" -> ShopEntry.EntryType.GAS;
            default -> ShopEntry.EntryType.ITEM;
        };
    }

    @Nullable
    public static String resourceSelector(@Nullable ShopEntry entry) {
        if (entry == null) {
            return null;
        }
        return switch (entry.type) {
            case FLUID -> entry.fluid;
            case GAS -> entry.gas;
            case ITEM -> entry.item;
        };
    }

    public static boolean isTagEntry(@Nullable ShopEntry entry) {
        String selector = resourceSelector(entry);
        return selector != null && selector.trim().startsWith("#");
    }

    public static boolean isTagSelector(@Nullable String selector) {
        return selector != null && selector.trim().startsWith("#");
    }

    public static boolean isBuyAllowed(@Nullable ShopEntry entry) {
        if (entry == null || isTagEntry(entry)) {
            return false;
        }
        return entry.buy > 0 || entry.free;
    }

    public static boolean isSellAllowed(@Nullable ShopEntry entry) {
        return entry != null && entry.sell > 0;
    }

    public static boolean isPlayerShopBrowsable(@Nullable ShopEntry entry) {
        if (entry == null) {
            return false;
        }
        return switch (entry.type) {
            case ITEM, FLUID -> true;
            case GAS -> MekChemicalHelper.isLoaded();
        };
    }

    /** Player shop can only trade item entries; fluids/gases are catalog-only. */
    public static boolean isPlayerShopTradable(@Nullable ShopEntry entry) {
        return entry != null && entry.type == ShopEntry.EntryType.ITEM;
    }

    public static boolean isAutoShopSelectable(@Nullable ShopEntry entry) {
        if (entry == null) {
            return false;
        }
        return entry.type != ShopEntry.EntryType.GAS || MekChemicalHelper.isLoaded();
    }

    /**
     * Validates and normalizes an entry after JSON parse.
     *
     * @return false if the entry must be skipped
     */
    public static boolean validateEntry(ShopEntry entry, String fileName) {
        if (entry.type == ShopEntry.EntryType.GAS && !MekChemicalHelper.isGasSupportEnabled()) {
            LOGGER.warn("Skipping gas shop entry {} in {}: gas support disabled on this loader", entry.id, fileName);
            return false;
        }
        if (entry.type == ShopEntry.EntryType.GAS && isTagSelector(entry.gas)) {
            LOGGER.warn("Skipping gas shop entry {} in {}: gas entries cannot use tags", entry.id, fileName);
            return false;
        }

        String selector = resourceSelector(entry);
        if (selector == null || selector.isBlank()) {
            LOGGER.warn("Skipping shop entry {} in {}: missing resource for type {}", entry.id, fileName, entry.type);
            return false;
        }

        if (isTagEntry(entry) && (entry.buy > 0 || entry.free)) {
            LOGGER.warn("Tag shop entry {} in {} cannot be bought; forcing buy=0 free=false", entry.id, fileName);
            entry.buy = 0;
            entry.free = false;
        }

        if (entry.amount <= 0) {
            entry.amount = 1;
        }
        entry.itemCount = entry.amount;
        return true;
    }

    public static boolean matchesItem(ItemStack stack, @Nullable String selector) {
        if (stack == null || stack.isEmpty() || selector == null || selector.isBlank()) {
            return false;
        }
        String trimmed = selector.trim();
        if (trimmed.startsWith("#")) {
            try {
                ResourceLocation tagId = ResourceLocation.parse(trimmed.substring(1));
                TagKey<Item> itemTag = ItemTags.create(tagId);
                return stack.is(itemTag);
            } catch (Exception ignored) {
                return false;
            }
        }
        ItemStack parsed = ItemConverter.parseItemString(trimmed, 1);
        return !parsed.isEmpty() && ItemStack.isSameItemSameComponents(stack, parsed);
    }

    public static boolean matchesFluid(FluidStack stack, @Nullable String selector) {
        if (stack == null || stack.getFluid() == Fluids.EMPTY || selector == null || selector.isBlank()) {
            return false;
        }
        String trimmed = selector.trim();
        if (trimmed.startsWith("#")) {
            try {
                ResourceLocation tagId = ResourceLocation.parse(trimmed.substring(1));
                TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, tagId);
                return stack.getFluid().builtInRegistryHolder().is(fluidTag);
            } catch (Exception ignored) {
                return false;
            }
        }
        Fluid fluid = resolveFluid(trimmed);
        return fluid != null && stack.getFluid() == fluid;
    }

    public static boolean matchesGas(@Nullable Object chemicalStack, @Nullable String selector) {
        if (!MekChemicalHelper.isLoaded() || chemicalStack == null || MekChemicalHelper.isEmpty(chemicalStack)) {
            return false;
        }
        if (selector == null || selector.isBlank() || isTagSelector(selector)) {
            return false;
        }
        String id = MekChemicalHelper.getRegistryName(chemicalStack);
        return id != null && id.equals(selector.trim());
    }

    @Nullable
    public static Fluid resolveFluid(@Nullable String fluidId) {
        if (fluidId == null || fluidId.isBlank() || fluidId.startsWith("#")) {
            return null;
        }
        try {
            ResourceLocation id = ResourceLocation.parse(fluidId.trim());
            return BuiltInRegistries.FLUID.getOptional(id).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    public static ItemStack displayStackForEntry(ShopEntry entry) {
        if (entry == null || entry.type != ShopEntry.EntryType.ITEM) {
            return ItemStack.EMPTY;
        }
        return displayStackForItemSelector(entry.item, entry.amount);
    }

    public static ItemStack displayStackForItemSelector(@Nullable String selector, int amount) {
        if (selector == null || selector.isBlank()) {
            return ItemStack.EMPTY;
        }
        String trimmed = selector.trim();
        if (trimmed.startsWith("#")) {
            return firstItemFromTag(trimmed);
        }
        ItemStack stack = ItemConverter.parseItemString(trimmed, 1);
        if (!stack.isEmpty()) {
            stack.setCount(Math.max(1, amount));
        }
        return stack;
    }

    /**
     * Label shown next to a shop entry icon: tag selectors stay as {@code #id};
     * concrete resources use their localized display name.
     */
    public static String displayLabelForEntry(@Nullable ShopEntry entry) {
        if (entry == null) {
            return "";
        }
        String selector = resourceSelector(entry);
        if (selector == null || selector.isBlank()) {
            return "";
        }
        String trimmed = selector.trim();
        if (trimmed.startsWith("#")) {
            return trimmed;
        }
        return switch (entry.type) {
            case ITEM -> {
                ItemStack stack = ItemConverter.parseItemString(trimmed, 1);
                yield !stack.isEmpty() ? stack.getHoverName().getString() : trimmed;
            }
            case FLUID -> {
                FluidStack fluid = displayFluidForEntry(entry);
                yield !fluid.isEmpty() ? fluid.getHoverName().getString() : trimmed;
            }
            case GAS -> {
                Object chemical = MekChemicalHelper.createStackFromId(trimmed, Math.max(1, entry.amount));
                Component name = MekChemicalHelper.getDisplayName(chemical);
                yield !name.getString().isEmpty() ? name.getString() : trimmed;
            }
        };
    }

    public static FluidStack displayFluidForEntry(@Nullable ShopEntry entry) {
        if (entry == null || entry.type != ShopEntry.EntryType.FLUID) {
            return FluidStack.EMPTY;
        }
        String selector = entry.fluid;
        if (selector == null || selector.isBlank()) {
            return FluidStack.EMPTY;
        }
        String trimmed = selector.trim();
        int amount = Math.max(1, entry.amount);
        if (trimmed.startsWith("#")) {
            Fluid first = firstFluidFromTag(trimmed);
            return first != null && first != Fluids.EMPTY ? new FluidStack(first, amount) : FluidStack.EMPTY;
        }
        Fluid fluid = resolveFluid(trimmed);
        return fluid != null && fluid != Fluids.EMPTY ? new FluidStack(fluid, amount) : FluidStack.EMPTY;
    }

    @Nullable
    public static Object displayGasForEntry(@Nullable ShopEntry entry) {
        if (entry == null || entry.type != ShopEntry.EntryType.GAS || !MekChemicalHelper.isLoaded()) {
            return null;
        }
        String selector = entry.gas;
        if (selector == null || selector.isBlank() || isTagSelector(selector)) {
            return null;
        }
        return MekChemicalHelper.createStackFromId(selector.trim(), Math.max(1L, entry.amount));
    }

    public static Component displayTooltipForEntry(@Nullable ShopEntry entry) {
        if (entry == null) {
            return Component.empty();
        }
        if (isTagEntry(entry)) {
            String selector = resourceSelector(entry);
            return Component.literal(selector != null ? selector.trim() : "");
        }
        return switch (entry.type) {
            case ITEM -> {
                ItemStack stack = displayStackForEntry(entry);
                yield !stack.isEmpty() ? stack.getHoverName() : Component.literal(displayLabelForEntry(entry));
            }
            case FLUID -> {
                FluidStack fluid = displayFluidForEntry(entry);
                yield !fluid.isEmpty() ? fluid.getHoverName() : Component.literal(displayLabelForEntry(entry));
            }
            case GAS -> {
                Object chemical = displayGasForEntry(entry);
                Component name = MekChemicalHelper.getDisplayName(chemical);
                yield !name.getString().isEmpty() ? name : Component.literal(displayLabelForEntry(entry));
            }
        };
    }

    private static ItemStack firstItemFromTag(String tagSelector) {
        try {
            ResourceLocation tagId = ResourceLocation.parse(tagSelector.trim().substring(1));
            TagKey<Item> itemTag = ItemTags.create(tagId);
            for (Item item : BuiltInRegistries.ITEM) {
                if (item.builtInRegistryHolder().is(itemTag)) {
                    return new ItemStack(item);
                }
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private static Fluid firstFluidFromTag(String tagSelector) {
        try {
            ResourceLocation tagId = ResourceLocation.parse(tagSelector.trim().substring(1));
            TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, tagId);
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (fluid != Fluids.EMPTY && fluid.builtInRegistryHolder().is(fluidTag)) {
                    return fluid;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String extractBaseId(@Nullable String resourceString) {
        if (resourceString == null) {
            return null;
        }
        String trimmed = resourceString.trim();
        if (trimmed.startsWith("#")) {
            return trimmed;
        }
        int bracketIndex = trimmed.indexOf('[');
        return bracketIndex != -1 ? trimmed.substring(0, bracketIndex) : trimmed;
    }

    /**
     * Prefers an entry matching {@code preferBuy}, otherwise any buy/sell-capable match.
     */
    @Nullable
    public static ShopEntry findMatchingFluidEntry(FluidStack stack, boolean preferBuy) {
        if (stack == null || stack.getFluid() == Fluids.EMPTY) {
            return null;
        }
        ShopEntry preferred = null;
        ShopEntry fallback = null;
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            if (entry.type != ShopEntry.EntryType.FLUID || !isAutoShopSelectable(entry)) {
                continue;
            }
            if (!matchesFluid(stack, entry.fluid)) {
                continue;
            }
            boolean preferredOk = preferBuy ? isBuyAllowed(entry) : isSellAllowed(entry);
            boolean anyOk = isBuyAllowed(entry) || isSellAllowed(entry);
            if (preferredOk) {
                preferred = entry;
                break;
            }
            if (fallback == null && anyOk) {
                fallback = entry;
            }
        }
        return preferred != null ? preferred : fallback;
    }

    /**
     * Prefers an entry matching {@code preferBuy}, otherwise any buy/sell-capable match.
     */
    @Nullable
    public static ShopEntry findMatchingGasEntry(@Nullable Object chemicalStack, boolean preferBuy) {
        if (!MekChemicalHelper.isLoaded() || chemicalStack == null || MekChemicalHelper.isEmpty(chemicalStack)) {
            return null;
        }
        ShopEntry preferred = null;
        ShopEntry fallback = null;
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            if (entry.type != ShopEntry.EntryType.GAS || !isAutoShopSelectable(entry)) {
                continue;
            }
            if (!matchesGas(chemicalStack, entry.gas)) {
                continue;
            }
            boolean preferredOk = preferBuy ? isBuyAllowed(entry) : isSellAllowed(entry);
            boolean anyOk = isBuyAllowed(entry) || isSellAllowed(entry);
            if (preferredOk) {
                preferred = entry;
                break;
            }
            if (fallback == null && anyOk) {
                fallback = entry;
            }
        }
        return preferred != null ? preferred : fallback;
    }

    /** Chooses buy/sell so the entry remains selectable for AutoShop apply. */
    public static boolean resolveBuyModeForEntry(@Nullable ShopEntry entry, boolean preferBuy) {
        if (entry == null) {
            return preferBuy;
        }
        if (preferBuy) {
            if (isBuyAllowed(entry)) {
                return true;
            }
            if (isSellAllowed(entry)) {
                return false;
            }
        } else {
            if (isSellAllowed(entry)) {
                return false;
            }
            if (isBuyAllowed(entry)) {
                return true;
            }
        }
        return preferBuy;
    }

    /**
     * Fluid contained in an item (bucket/tank). Uses legacy FluidUtil on 1.21.1.
     */
    public static FluidStack fluidContainedInItem(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
    }

    /**
     * JEI may expose fluid types with amount 0; normalize so matching/isEmpty checks still work.
     */
    public static FluidStack normalizeFluidIngredient(@Nullable FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        if (fluid.getAmount() <= 0) {
            return new FluidStack(fluid.getFluid(), 1000);
        }
        return fluid;
    }

    public static boolean isFluidIngredient(@Nullable Object ingredient) {
        return ingredient instanceof FluidStack fluid && fluid.getFluid() != Fluids.EMPTY;
    }
}
