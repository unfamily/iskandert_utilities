package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.item.ModItems;

/**
 * Drops machine inventories and internal entropy charge when a block is broken.
 */
public final class MachineBreakDrops {
    private MachineBreakDrops() {}

    public static void dropContainerContents(Level level, BlockPos pos, Container container) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, x, y, z, stack.copy());
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    /** Converts whole stored charge units back into Drop of Entropy stacks (partial charge is lost). */
    public static void dropStoredEntropyCharge(Level level, BlockPos pos, int storedCharge) {
        int dropCount = EntropyCharges.wholeDropsFromStored(storedCharge);
        if (dropCount <= 0) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        ItemStack remaining = new ItemStack(ModItems.DROP_OF_ENTROPY.get(), dropCount);
        while (!remaining.isEmpty()) {
            int split = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            Containers.dropItemStack(level, x, y, z, remaining.split(split));
        }
    }
}
