package net.unfamily.iskautils.data.load;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;

/**
 * Runtime stonecutter recipe resolution for the Factory (not added to {@link FactoryLoader#SOURCES} or JEI).
 */
public final class FactoryStonecutterSupport {

    private FactoryStonecutterSupport() {}

    public static Optional<FactoryLoader.Source> resolve(ItemStack input, Level level) {
        if (input.isEmpty() || !Config.factoryStonecutterEnabled || level == null) {
            return Optional.empty();
        }
        return resolve(input, level.getRecipeManager(), level.registryAccess());
    }

    public static Optional<FactoryLoader.Source> resolve(
            ItemStack input, RecipeManager recipeManager, HolderLookup.Provider registries) {
        if (input.isEmpty() || !Config.factoryStonecutterEnabled) {
            return Optional.empty();
        }
        Map<ResourceLocation, FactoryLoader.Output> outputsById = new LinkedHashMap<>();
        for (RecipeHolder<?> holder : recipeManager.getAllRecipesFor(RecipeType.STONECUTTING)) {
            if (!(holder.value() instanceof StonecutterRecipe recipe)) {
                continue;
            }
            if (recipe.getIngredients().isEmpty() || !recipe.getIngredients().getFirst().test(input)) {
                continue;
            }
            ItemStack result = recipe.getResultItem(registries);
            if (result.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
            int count = Math.max(1, result.getCount());
            outputsById.putIfAbsent(id, new FactoryLoader.Output(id, count));
        }
        if (outputsById.isEmpty()) {
            return Optional.empty();
        }
        List<FactoryLoader.Output> outputs = new ArrayList<>(outputsById.values());
        return FactoryLoader.tryCompileSource(
                ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory_stonecutter"),
                BuiltInRegistries.ITEM.getKey(input.getItem()).toString(),
                1,
                outputs,
                Math.max(0, Config.factoryStonecutterEnergyPerOp));
    }
}
