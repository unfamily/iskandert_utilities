package net.unfamily.iskautils.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.relic.TheRootsItem;

/**
 * Item stacks prepared for JEI / client UI (custom model data, etc.).
 */
public final class JeiDisplayStacks {
    private JeiDisplayStacks() {}

    public static ItemStack of(Item item) {
        ItemStack stack = new ItemStack(item);
        applyClientDisplay(stack);
        return stack;
    }

    public static ItemStack copy(ItemStack stack) {
        ItemStack copy = stack.copy();
        applyClientDisplay(copy);
        return copy;
    }

    /** Call from client-only code (JEI, client tick). */
    public static void applyClientDisplay(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.is(ModItems.THE_ROOTS.get())) {
            TheRootsItem.syncClientCustomModelData(stack);
        }
    }
}
