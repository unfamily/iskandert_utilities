package net.unfamily.iskautils.block.entity;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.util.FilterDisplayItems;
import org.jetbrains.annotations.NotNull;

/**
 * Read-only view of Pattern Crafter variable filters for menu slot sync.
 * Display stacks are always derived from string filters, never from stored ghost items.
 */
public final class InputFilterDisplayHandler extends ItemStackHandler {
    private final IntFunction<String> filterAt;
    private final Supplier<HolderLookup.Provider> registryAccess;

    public InputFilterDisplayHandler(
            int size, IntFunction<String> filterAt, Supplier<HolderLookup.Provider> registryAccess) {
        super(size);
        this.filterAt = filterAt;
        this.registryAccess = registryAccess;
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            return ItemStack.EMPTY;
        }
        return FilterDisplayItems.forFilter(filterAt.apply(slot), registryAccess.get());
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        // String filters are authoritative; ignore container sync of orphan stacks.
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        // Legacy ItemStack filters are migrated to strings on block entity load.
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return new CompoundTag();
    }
}
