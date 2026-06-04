package net.unfamily.iskautils.item;

import net.minecraft.world.item.ItemStack;

/**
 * Items that store RF in custom data and expose it through {@link ModItemCapabilities}.
 */
public interface RfStoringItem {
    boolean canStoreEnergy();

    int getEnergyStored(ItemStack stack);

    void setEnergyStored(ItemStack stack, int energy);

    int getMaxEnergyStored(ItemStack stack);
}
