package net.unfamily.iskautils.client.gui;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/** Nine-slot paginated view over Pattern Crafter output storage. */
public final class OutputPageViewHandler implements IItemHandlerModifiable {
    private static final int VIEW_SIZE = 9;
    private final IItemHandler backend;
    private int offset;

    public OutputPageViewHandler(IItemHandler backend) {
        this.backend = backend;
    }

    public void setOffset(int offset) {
        this.offset = Math.max(0, Math.min(offset, Math.max(0, backend.getSlots() - VIEW_SIZE)));
    }

    public int getOffset() {
        return offset;
    }

    @Override public int getSlots() { return VIEW_SIZE; }
    @Override public @NotNull ItemStack getStackInSlot(int slot) {
        return valid(slot) ? backend.getStackInSlot(offset + slot) : ItemStack.EMPTY;
    }
    @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return valid(slot) ? backend.insertItem(offset + slot, stack, simulate) : stack;
    }
    @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return valid(slot) ? backend.extractItem(offset + slot, amount, simulate) : ItemStack.EMPTY;
    }
    @Override public int getSlotLimit(int slot) {
        return valid(slot) ? backend.getSlotLimit(offset + slot) : 0;
    }
    @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return valid(slot) && backend.isItemValid(offset + slot, stack);
    }
    @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (valid(slot) && backend instanceof IItemHandlerModifiable mod) {
            mod.setStackInSlot(offset + slot, stack);
        }
    }
    private boolean valid(int slot) {
        return slot >= 0 && slot < VIEW_SIZE && offset + slot < backend.getSlots();
    }
}
