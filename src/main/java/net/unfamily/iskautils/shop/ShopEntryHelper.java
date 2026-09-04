package net.unfamily.iskautils.shop;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.shop.ItemConverter;
import net.unfamily.iskautils.util.ModLogger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helpers for typed shop entries (item / fluid / gas) and {@code #tag} selectors.
 */
public final class ShopEntryHelper {
    private static final ModLogger LOGGER = ModLogger.of(ShopEntryHelper.class);
    private static final int TAG_CYCLE_MS = 1000;
    private static final Map<String, List<ItemStack>> ITEM_TAG_STACKS = new ConcurrentHashMap<>();
    private static final Map<String, List<Fluid>> FLUID_TAG_MEMBERS = new ConcurrentHashMap<>();

    private ShopEntryHelper() {}

    /** Clears cached tag members (call on shop/datapack reload). */
    public static void clearTagDisplayCaches() {
        ITEM_TAG_STACKS.clear();
        FLUID_TAG_MEMBERS.clear();
    }

    /** Shared cycle index so icon and label stay in sync across all shop UIs. */
    public static int tagCycleIndex(int size) {
        if (size <= 0) {
            return 0;
        }
        if (size == 1) {
            return 0;
        }
        return (int) ((System.currentTimeMillis() / TAG_CYCLE_MS) % size);
    }

    public static ShopEntry.EntryType parseType(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return ShopEntry.EntryType.ITEM;
        }
        return switch (raw.trim().toLowerCase()) {
            case "fluid" -> ShopEntry.EntryType.FLUID;
            case "gas" -> ShopEntry.EntryType.GAS;
            case "other" -> ShopEntry.EntryType.OTHER;
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
            case OTHER -> entry.other;
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

    /**
     * Search match against a tag entry: tag id plus every member display name
     * (not only the currently cycling icon/label).
     */
    public static boolean tagEntryMatchesSearch(@Nullable ShopEntry entry, String lowerQuery) {
        if (entry == null || lowerQuery == null || lowerQuery.isEmpty() || !isTagEntry(entry)) {
            return false;
        }
        String selector = resourceSelector(entry);
        if (selector != null && selector.toLowerCase().contains(lowerQuery)) {
            return true;
        }
        return switch (entry.type) {
            case ITEM -> {
                for (ItemStack stack : itemStacksFromTag(selector)) {
                    if (!stack.isEmpty() && stack.getHoverName().getString().toLowerCase().contains(lowerQuery)) {
                        yield true;
                    }
                }
                yield false;
            }
            case FLUID -> {
                for (Fluid fluid : fluidsFromTag(selector)) {
                    if (fluid != null && fluid != Fluids.EMPTY) {
                        String name = new FluidStack(fluid, 1).getHoverName().getString().toLowerCase();
                        if (name.contains(lowerQuery)) {
                            yield true;
                        }
                    }
                }
                yield false;
            }
            case GAS -> false;
            case OTHER -> false;
        };
    }

    /** True if the entry has any buy/sell offer (not buy=0, sell=0, free=false). Editor still lists these. */
    public static boolean hasTradeOffer(@Nullable ShopEntry entry) {
        return entry != null && (entry.free || entry.buy > 0 || entry.sell > 0);
    }

    /**
     * Player shop lists item/fluid/gas/other entries that have a trade offer.
     * Fluids/gases/other are catalog-only (not tradable here); use AutoShop to trade them.
     * Gas needs Mekanism. Entries with no offer stay in the editor only.
     */
    public static boolean isPlayerShopBrowsable(@Nullable ShopEntry entry) {
        if (entry == null || !hasTradeOffer(entry)) {
            return false;
        }
        return switch (entry.type) {
            case ITEM, FLUID, OTHER -> true;
            case GAS -> MekChemicalHelper.isLoaded();
        };
    }

    /** Player shop can only trade item entries; fluids/gases/other are catalog-only (trade via AutoShop). */
    public static boolean isPlayerShopTradable(@Nullable ShopEntry entry) {
        return entry != null && entry.type == ShopEntry.EntryType.ITEM;
    }

    public static boolean isAutoShopSelectable(@Nullable ShopEntry entry) {
        if (entry == null || !hasTradeOffer(entry)) {
            return false;
        }
        if (entry.type == ShopEntry.EntryType.GAS && !MekChemicalHelper.isLoaded()) {
            return false;
        }
        if (entry.type == ShopEntry.EntryType.OTHER) {
            return ShopOtherRegistry.isRegistered(entry.other);
        }
        return true;
    }

    /**
     * Validates and normalizes an entry after JSON parse.
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
        if (entry.type == ShopEntry.EntryType.OTHER) {
            if (isTagSelector(entry.other)) {
                LOGGER.warn("Skipping other shop entry {} in {}: other entries cannot use tags", entry.id, fileName);
                return false;
            }
            if (!ShopOtherRegistry.isRegistered(entry.other)) {
                LOGGER.warn("Skipping other shop entry {} in {}: unknown other id {}", entry.id, fileName, entry.other);
                return false;
            }
        }

        String selector = resourceSelector(entry);
        if (selector == null || selector.isBlank()) {
            LOGGER.warn("Skipping shop entry {} in {}: missing resource for type {}", entry.id, fileName, entry.type);
            return false;
        }

        if (isTagEntry(entry)) {
            if (entry.buy > 0 || entry.free) {
                LOGGER.warn("Tag shop entry {} in {} cannot be bought; forcing buy=0 free=false", entry.id, fileName);
                entry.buy = 0;
                entry.free = false;
            }
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
                Identifier tagId = Identifier.parse(trimmed.substring(1));
                TagKey<Item> itemTag = ItemTags.create(tagId);
                return stack.is(itemTag);
            } catch (Exception ignored) {
                return false;
            }
        }
        ItemStack parsed = ItemConverter.parseItemString(trimmed, 1);
        if (parsed.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(stack, parsed);
    }

    public static boolean matchesFluid(FluidStack stack, @Nullable String selector) {
        if (stack == null || stack.getFluid() == Fluids.EMPTY || selector == null || selector.isBlank()) {
            return false;
        }
        String trimmed = selector.trim();
        if (trimmed.startsWith("#")) {
            try {
                Identifier tagId = Identifier.parse(trimmed.substring(1));
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
            Identifier id = Identifier.parse(fluidId.trim());
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
            List<ItemStack> stacks = itemStacksFromTag(trimmed);
            if (stacks.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = stacks.get(tagCycleIndex(stacks.size())).copy();
            stack.setCount(Math.max(1, amount));
            return stack;
        }
        ItemStack stack = ItemConverter.parseItemString(trimmed, 1);
        if (!stack.isEmpty()) {
            stack.setCount(Math.max(1, amount));
        }
        return stack;
    }

    /**
     * Label shown next to a shop entry icon. Tag entries cycle the localized name of the
     * currently displayed member (same index as the icon).
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
            return switch (entry.type) {
                case ITEM -> {
                    ItemStack stack = displayStackForEntry(entry);
                    yield !stack.isEmpty() ? stack.getHoverName().getString() : trimmed;
                }
                case FLUID -> {
                    FluidStack fluid = displayFluidForEntry(entry);
                    yield !fluid.isEmpty() ? fluid.getHoverName().getString() : trimmed;
                }
                case GAS -> trimmed;
                case OTHER -> trimmed;
            };
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
            case OTHER -> trimmed;
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
            List<Fluid> fluids = fluidsFromTag(trimmed);
            if (fluids.isEmpty()) {
                return FluidStack.EMPTY;
            }
            Fluid fluid = fluids.get(tagCycleIndex(fluids.size()));
            return fluid != Fluids.EMPTY ? new FluidStack(fluid, amount) : FluidStack.EMPTY;
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
            case OTHER -> Component.literal(displayLabelForEntry(entry));
        };
    }

    private static ItemStack firstItemFromTag(String tagSelector) {
        List<ItemStack> stacks = itemStacksFromTag(tagSelector);
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0).copy();
    }

    @Nullable
    private static Fluid firstFluidFromTag(String tagSelector) {
        List<Fluid> fluids = fluidsFromTag(tagSelector);
        return fluids.isEmpty() ? null : fluids.get(0);
    }

    public static List<ItemStack> itemStacksFromTag(String tagSelector) {
        if (tagSelector == null || !tagSelector.trim().startsWith("#")) {
            return List.of();
        }
        String key = tagSelector.trim();
        return ITEM_TAG_STACKS.computeIfAbsent(key, ShopEntryHelper::computeItemStacksFromTag);
    }

    public static List<Fluid> fluidsFromTag(String tagSelector) {
        if (tagSelector == null || !tagSelector.trim().startsWith("#")) {
            return List.of();
        }
        String key = tagSelector.trim();
        return FLUID_TAG_MEMBERS.computeIfAbsent(key, ShopEntryHelper::computeFluidsFromTag);
    }

    private static List<ItemStack> computeItemStacksFromTag(String tagSelector) {
        List<ItemStack> stacks = new ArrayList<>();
        try {
            Identifier tagId = Identifier.parse(tagSelector.substring(1));
            TagKey<Item> itemTag = ItemTags.create(tagId);
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(itemTag)) {
                stacks.add(new ItemStack(holder.value()));
            }
            if (stacks.isEmpty()) {
                for (Item item : BuiltInRegistries.ITEM) {
                    if (item.builtInRegistryHolder().is(itemTag)) {
                        stacks.add(new ItemStack(item));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return List.copyOf(stacks);
    }

    private static List<Fluid> computeFluidsFromTag(String tagSelector) {
        List<Fluid> fluids = new ArrayList<>();
        try {
            Identifier tagId = Identifier.parse(tagSelector.substring(1));
            TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, tagId);
            for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(fluidTag)) {
                Fluid fluid = holder.value();
                if (fluid != Fluids.EMPTY) {
                    fluids.add(fluid);
                }
            }
            if (fluids.isEmpty()) {
                for (Fluid fluid : BuiltInRegistries.FLUID) {
                    if (fluid != Fluids.EMPTY && fluid.builtInRegistryHolder().is(fluidTag)) {
                        fluids.add(fluid);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return List.copyOf(fluids);
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
        if (bracketIndex != -1) {
            return trimmed.substring(0, bracketIndex);
        }
        return trimmed;
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
     * Fluid contained in an item (bucket/tank). Uses NeoForge transfer API on 26+.
     */
    public static FluidStack fluidContainedInItem(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return net.neoforged.neoforge.transfer.fluid.FluidUtil.getFirstStackContained(stack);
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
