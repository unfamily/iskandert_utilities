package net.unfamily.iskautils.client.gui;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * An output-only slot that allows extraction but blocks insertion.
 * Used for crafting output slots - items are placed here by the machine,
 * and the player can only take them out.
 */
public class OutputSlot extends SlotItemHandler {

    public OutputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }
}
