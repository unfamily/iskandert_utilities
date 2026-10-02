package net.unfamily.iskautils.integration.jei;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** JEI display for one Factory mapping page. {@link #recipeId} feeds JEI native copy via getRegistryName. */
public record FactoryJeiRecipe(
        @Nullable ResourceLocation recipeId,
        int inputAmount,
        List<ItemStack> inputs,
        List<ItemStack> outputs) {}
