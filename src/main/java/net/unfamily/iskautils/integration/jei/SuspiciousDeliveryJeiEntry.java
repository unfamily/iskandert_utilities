package net.unfamily.iskautils.integration.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** One output slot on a Suspicious Delivery JEI page. */
public record SuspiciousDeliveryJeiEntry(
        double weightPercent,
        List<ItemStack> displayStacks,
        int entryLuck) {

    public SuspiciousDeliveryJeiEntry {
        displayStacks = List.copyOf(displayStacks);
    }
}
