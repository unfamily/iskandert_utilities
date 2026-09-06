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
import net.unfamily.iskalib.item.ItemConverter;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.util.ModLogger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helpers for typed shop entries and {@code #tag} selectors.
 */
public final class ShopEntryHelper {
    private static final ModLogger LOGGER = ModLogger.of(ShopEntryHelper.class);
    private static final int TAG_CYCLE_MS = 1000;
    private static final Map<String, List<ItemStack>> ITEM_TAG_STACKS = new ConcurrentHashMap<>();
    private static final Map<String, List<Fluid>> FLUID_TAG_MEMBERS = new ConcurrentHashMap<>();

    private ShopEntryHelper() {}

    public static void clearTagDisplayCaches() {
        ITEM_TAG_STACKS.clear();
        FLUID_TAG_MEMBERS.clear();
    }

    public static int tagCycleIndex(int size) {
        if (size <= 0) {
            return 0;
        }
        if (size == 1) {
            return 0;
        }
        return (int) ((System.currentTimeMillis() / TAG_CYCLE_MS) % size);
    }

    /**
     * Parse type string. Legacy bare {@code item} / blank → {@code iska_utils:item}.
     * Unknown types return null (caller must skip).
     */
    @Nullable
    public static Identifier parseTypeId(@Nullable String raw) {
        ShopEntryTypeRegistry.ensureBuiltins();
        if (raw == null || raw.isBlank() || "item".equalsIgnoreCase(raw.trim())) {
            return ShopEntryTypes.ITEM;
        }
        String trimmed = raw.trim();
        Identifier parsed;
        try {
            if (trimmed.indexOf(':') < 0) {
                // Reject short fluid/gas/other — only legacy item is allowed without namespace
                LOGGER.warn("Unknown shop entry type '{}' (use iska_utils:* ids; legacy bare item only)", trimmed);
                return null;
            }
            parsed = Identifier.parse(trimmed);
        } catch (Exception e) {
            LOGGER.warn("Invalid shop entry type '{}'", trimmed);
            return null;
        }
        if (ShopEntryTypeRegistry.get(parsed) == null) {
            LOGGER.warn("Unknown shop entry type '{}'", parsed);
            return null;
        }
        return parsed;
    }

    /** @deprecated use {@link #parseTypeId(String)} */
    @Deprecated
    public static Identifier parseType(@Nullable String raw) {
        Identifier id = parseTypeId(raw);
        return id != null ? id : ShopEntryTypes.ITEM;
    }

    public static String typeIdString(@Nullable ShopEntry entry) {
        if (entry == null || entry.typeId == null) {
            return ShopEntryTypes.ITEM.toString();
        }
        return entry.typeId.toString();
    }

    @Nullable
    public static String resourceSelector(@Nullable ShopEntry entry) {
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        return handler != null ? handler.resourceSelector(entry) : null;
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
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        if (handler != null && !handler.usesBuy()) {
            return handler.isPlayerShopTradable(entry);
        }
        return entry.buy > 0 || entry.free;
    }

    public static boolean isSellAllowed(@Nullable ShopEntry entry) {
        if (entry == null) {
            return false;
        }
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        if (handler != null && !handler.usesSell()) {
            return false;
        }
        return entry.sell > 0;
    }

    public static boolean tagEntryMatchesSearch(@Nullable ShopEntry entry, String lowerQuery) {
        if (entry == null || lowerQuery == null || lowerQuery.isEmpty() || !isTagEntry(entry)) {
            return false;
        }
        String selector = resourceSelector(entry);
        if (selector != null && selector.toLowerCase().contains(lowerQuery)) {
            return true;
        }
        if (ShopEntryTypes.isItem(entry)) {
            for (ItemStack stack : itemStacksFromTag(selector)) {
                if (!stack.isEmpty() && stack.getHoverName().getString().toLowerCase().contains(lowerQuery)) {
                    return true;
                }
            }
        } else if (ShopEntryTypes.isFluid(entry)) {
            for (Fluid fluid : fluidsFromTag(selector)) {
                if (fluid != null && fluid != Fluids.EMPTY) {
                    String name = new FluidStack(fluid, 1).getHoverName().getString().toLowerCase();
                    if (name.contains(lowerQuery)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasTradeOffer(@Nullable ShopEntry entry) {
        if (entry == null) {
            return false;
        }
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        if (handler != null && !handler.usesBuy() && !handler.usesSell()) {
            return handler.isPlayerShopTradable(entry);
        }
        return entry.free || entry.buy > 0 || entry.sell > 0;
    }

    public static boolean isPlayerShopBrowsable(@Nullable ShopEntry entry) {
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        return handler != null && handler.isPlayerShopBrowsable(entry);
    }

    public static boolean isPlayerShopTradable(@Nullable ShopEntry entry) {
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        return handler != null && handler.isPlayerShopTradable(entry);
    }

    public static boolean isAutoShopSelectable(@Nullable ShopEntry entry) {
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        return handler != null && handler.isAutoShopSelectable(entry);
    }

    public static boolean validateEntry(ShopEntry entry, String fileName) {
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        if (handler == null) {
            LOGGER.warn("Skipping shop entry {} in {}: unknown type {}", entry.id, fileName, entry.typeId);
            return false;
        }
        if (!handler.validate(entry, fileName)) {
            return false;
        }

        if (handler.usesResourceSelector()) {
            String selector = handler.resourceSelector(entry);
            if (selector == null || selector.isBlank()) {
                LOGGER.warn("Skipping shop entry {} in {}: missing resource for type {}", entry.id, fileName, entry.typeId);
                return false;
            }
        }

        if (isTagEntry(entry) && (entry.buy > 0 || entry.free)) {
            LOGGER.warn("Tag shop entry {} in {} cannot be bought; forcing buy=0 free=false", entry.id, fileName);
            entry.buy = 0;
            entry.free = false;
        }

        if (handler.usesAmount()) {
            if (entry.amount <= 0) {
                entry.amount = 1;
            }
            entry.itemCount = entry.amount;
        } else {
            entry.amount = 1;
            entry.itemCount = 1;
        }
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
        ItemStack template = ItemConverter.parseItemString(trimmed, 1);
        return !template.isEmpty() && ItemStack.isSameItemSameComponents(stack, template);
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
        return fluid != null && fluid != Fluids.EMPTY && stack.getFluid().isSame(fluid);
    }

    public static ItemStack displayStackForEntry(ShopEntry entry) {
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        return handler != null ? handler.displayItemStack(entry) : ItemStack.EMPTY;
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

    public static String displayLabelForEntry(@Nullable ShopEntry entry) {
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        return handler != null ? handler.displayLabel(entry) : "";
    }

    public static FluidStack displayFluidForEntry(@Nullable ShopEntry entry) {
        if (!ShopEntryTypes.isFluid(entry)) {
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
        if (!ShopEntryTypes.isGas(entry) || !MekChemicalHelper.isLoaded()) {
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
        ShopEntryTypeHandler handler = ShopEntryTypeRegistry.get(entry);
        return handler != null ? handler.displayName(entry) : Component.empty();
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
        return bracketIndex != -1 ? trimmed.substring(0, bracketIndex) : trimmed;
    }

    @Nullable
    public static ShopEntry findMatchingFluidEntry(FluidStack stack, boolean preferBuy) {
        if (stack == null || stack.getFluid() == Fluids.EMPTY) {
            return null;
        }
        ShopEntry preferred = null;
        ShopEntry fallback = null;
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            if (!ShopEntryTypes.isFluid(entry) || !isAutoShopSelectable(entry)) {
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

    @Nullable
    public static ShopEntry findMatchingGasEntry(@Nullable Object chemicalStack, boolean preferBuy) {
        if (!MekChemicalHelper.isLoaded() || chemicalStack == null || MekChemicalHelper.isEmpty(chemicalStack)) {
            return null;
        }
        ShopEntry preferred = null;
        ShopEntry fallback = null;
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            if (!ShopEntryTypes.isGas(entry) || !isAutoShopSelectable(entry)) {
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

    public static FluidStack fluidContainedInItem(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
    }

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
}
