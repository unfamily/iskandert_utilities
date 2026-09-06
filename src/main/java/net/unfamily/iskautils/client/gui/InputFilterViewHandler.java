package net.unfamily.iskautils.client.gui;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/**
 * IItemHandler view over a slice of another handler. Used for paginated input filter:
 * the menu shows 18 slots at a time; this view delegates to backend slots [offset .. offset+17].
 * Implements IItemHandlerModifiable so NeoForge slot sync can call setStackInSlot.
 * When acceptWrites is false (during page change cooldown), setStackInSlot is a no-op to avoid
 * sync from the previous page overwriting the newly visible page's backend slots.
 */
public class InputFilterViewHandler implements IItemHandlerModifiable {

    private static final int VIEW_SLOTS = 18;

    private final IItemHandler backend;
    private int offset;
    /** If false, setStackInSlot does nothing (used during page transition to avoid overwriting data). */
    private volatile boolean acceptWrites = true;

    public InputFilterViewHandler(IItemHandler backend) {
        this.backend = backend;
        this.offset = 0;
    }

    public void setAcceptWrites(boolean accept) {
        this.acceptWrites = accept;
    }

    public void setOffset(int offset) {
        this.offset = Math.max(0, Math.min(offset, Math.max(0, backend.getSlots() - VIEW_SLOTS)));
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public int getSlots() {
        return VIEW_SLOTS;
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= VIEW_SLOTS) return ItemStack.EMPTY;
        int backendSlot = offset + slot;
        return backendSlot < backend.getSlots() ? backend.getStackInSlot(backendSlot) : ItemStack.EMPTY;
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= VIEW_SLOTS) return stack;
        int backendSlot = offset + slot;
        return backendSlot < backend.getSlots() ? backend.insertItem(backendSlot, stack, simulate) : stack;
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= VIEW_SLOTS) return ItemStack.EMPTY;
        int backendSlot = offset + slot;
        return backendSlot < backend.getSlots() ? backend.extractItem(backendSlot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < 0 || slot >= VIEW_SLOTS) return 0;
        int backendSlot = offset + slot;
        return backendSlot < backend.getSlots() ? backend.getSlotLimit(backendSlot) : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= VIEW_SLOTS) return false;
        int backendSlot = offset + slot;
        return backendSlot < backend.getSlots() && backend.isItemValid(backendSlot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= VIEW_SLOTS) return;
        if (!acceptWrites) return; // block writes during page transition so old sync doesn't overwrite new page
        int backendSlot = offset + slot;
        if (backendSlot < backend.getSlots() && backend instanceof IItemHandlerModifiable mod) {
            mod.setStackInSlot(backendSlot, stack);
        }
    }
}
