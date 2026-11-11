package net.unfamily.iskautils.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1.21.1 stand-in for stonecutter-style {@code RecipeDisplay} entries (vanilla added in 26.x).
 */
public record RecipeCompiledDisplay(Ingredient input, ItemStack result) {}
