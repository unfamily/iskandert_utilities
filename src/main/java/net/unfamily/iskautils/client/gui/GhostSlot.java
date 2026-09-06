package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * A ghost slot that displays an item reference but doesn't allow normal interaction.
 * Used for filter slots - clicking copies the carried item as a filter reference.
 * The item is NOT consumed; it's just used as a visual reference.
 */
public class GhostSlot extends SlotItemHandler {

    public GhostSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
