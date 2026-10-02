package net.unfamily.iskautils.integration.jei;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** JEI display for one Ancient Tablet entry. {@link #recipeId} feeds JEI native copy via getRegistryName. */
public record AncientTabletJeiRecipe(
        @Nullable ResourceLocation recipeId,
        List<ItemStack> inputs,
        List<ItemStack> outputs,
        boolean mustOrdered,
        boolean destroyIfWrong,
        int fuelCost) {}
