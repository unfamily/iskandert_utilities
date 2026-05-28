package net.unfamily.iskautils.data.load;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;

/**
 * Runtime stonecutter recipe resolution for the Factory (not added to {@link FactoryLoader#SOURCES} or JEI).
 */
public final class FactoryStonecutterSupport {

    private FactoryStonecutterSupport() {}

    public static Optional<FactoryLoader.Source> resolve(ItemStack input, Level level) {
        if (input.isEmpty() || !Config.factoryStonecutterEnabled || level == null) {
            return Optional.empty();
        }
        SelectableRecipe.SingleInputSet<StonecutterRecipe> filtered =
                level.recipeAccess().stonecutterRecipes().selectByInput(input);
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        ContextMap displayContext = SlotDisplayContext.fromLevel(level);
        Map<Identifier, FactoryLoader.Output> outputsById = new LinkedHashMap<>();
        for (SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry : filtered.entries()) {
            ItemStack result = resolveResultStack(entry, input, displayContext);
            if (result.isEmpty()) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(result.getItem());
            int count = Math.max(1, result.getCount());
            outputsById.putIfAbsent(id, new FactoryLoader.Output(id, count));
        }
        if (outputsById.isEmpty()) {
            return Optional.empty();
        }
        List<FactoryLoader.Output> outputs = new ArrayList<>(outputsById.values());
        return Optional.of(new FactoryLoader.Source(
                new FactoryLoader.Selector.ItemSelector(input.getItem()),
                1,
                List.copyOf(outputs),
                Math.max(0, Config.factoryStonecutterEnergyPerOp)));
    }

    /**
     * Server has full {@link RecipeHolder}s; the client only receives {@link SelectableRecipe#optionDisplay()} entries.
     */
    private static ItemStack resolveResultStack(
            SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry,
            ItemStack input,
            ContextMap displayContext) {
        Optional<RecipeHolder<StonecutterRecipe>> holder = entry.recipe().recipe();
        if (holder.isPresent()) {
            return holder.get().value().assemble(new SingleRecipeInput(input));
        }
        return entry.recipe().optionDisplay().resolveForFirstStack(displayContext);
    }
}
