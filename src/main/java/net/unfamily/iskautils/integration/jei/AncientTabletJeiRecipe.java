package net.unfamily.iskautils.integration.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record AncientTabletJeiRecipe(
        List<ItemStack> inputs,
        List<ItemStack> outputs,
        boolean mustOrdered,
        boolean destroyIfWrong,
        int fuelCost) {}
