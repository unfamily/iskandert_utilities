package net.unfamily.iskautils.integration.jei;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public record FactoryJeiRecipe(int inputAmount, List<ItemStack> inputs, List<ItemStack> outputs) {}
