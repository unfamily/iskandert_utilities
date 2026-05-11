package net.unfamily.iskautils.integration.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One JEI page: shared input stacks, minimal input amount, up to 27 dye outputs for this page.
 */
public record FactoryJeiRecipe(int inputAmount, List<ItemStack> inputs, List<ItemStack> outputs) {}
