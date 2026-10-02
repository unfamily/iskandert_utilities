package net.unfamily.iskautils.integration.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.unfamily.iskautils.crafting.FactorySourcesRecipe;
import net.unfamily.iskautils.crafting.ModFactoryRecipes;
import net.unfamily.iskautils.data.load.CraftingEntryPools;
import net.unfamily.iskautils.data.load.FactoryLoader;
import net.unfamily.iskautils.data.load.RecipeManagerRecipes;

public final class FactoryJeiRecipes {

    private FactoryJeiRecipes() {}

    public static void reloadForClient(Minecraft mc) {
        if (!FactoryLoader.getSources().isEmpty()) {
            return;
        }
        RecipeManager recipes = null;
        var server = mc.getSingleplayerServer();
        if (server != null) {
            recipes = server.getRecipeManager();
        }
        if (recipes != null) {
            FactoryLoader.loadFromRecipeManager(recipes);
        }
    }

    public static List<FactoryJeiRecipe> buildAll() {
        Minecraft mc = Minecraft.getInstance();
        RecipeManager recipes = null;
        if (mc != null) {
            var server = mc.getSingleplayerServer();
            if (server != null) {
                recipes = server.getRecipeManager();
            }
        }
        if (recipes != null) {
            return buildFromRecipeManager(recipes);
        }
        return buildFromCachedSources();
    }

    private static List<FactoryJeiRecipe> buildFromRecipeManager(RecipeManager recipes) {
        ServerPlayer player = CraftingEntryPools.resolveJeiPlayer();
        final int pageSize = FactoryJeiBackgroundDrawable.GRID_COLS * FactoryJeiBackgroundDrawable.GRID_ROWS;
        List<FactoryJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<FactorySourcesRecipe> holder :
                RecipeManagerRecipes.holdersOfType(recipes, ModFactoryRecipes.FACTORY_TYPE.get())) {
            for (FactoryLoader.Source src : holder.value().compiledSources()) {
                appendPages(out, holder.id().identifier(), src, player, pageSize);
            }
        }
        return out;
    }

    private static List<FactoryJeiRecipe> buildFromCachedSources() {
        ServerPlayer player = CraftingEntryPools.resolveJeiPlayer();
        final int pageSize = FactoryJeiBackgroundDrawable.GRID_COLS * FactoryJeiBackgroundDrawable.GRID_ROWS;
        List<FactoryJeiRecipe> out = new ArrayList<>();
        for (FactoryLoader.Source src : FactoryLoader.getSources()) {
            appendPages(out, null, src, player, pageSize);
        }
        return out;
    }

    private static void appendPages(
            List<FactoryJeiRecipe> out,
            @org.jetbrains.annotations.Nullable net.minecraft.resources.Identifier recipeId,
            FactoryLoader.Source src,
            ServerPlayer player,
            int pageSize) {
        if (!src.gateHost().checkAllMods()) {
            return;
        }
        List<FactoryLoader.Output> outputs = src.resolveOutputs(player);
        if (outputs.isEmpty() && src.hasGate() && player == null) {
            return;
        }
        if (outputs.isEmpty() && !src.hasIfBranches() && src.flatOutputs().isEmpty()) {
            return;
        }
        if (outputs.isEmpty()) {
            return;
        }
        List<ItemStack> inputs = FactoryLoader.expandInputForJei(src);
        List<ItemStack> fullOutputs = new ArrayList<>();
        for (FactoryLoader.Output o : outputs) {
            FactoryLoader.resolveOutputStack(o).ifPresent(fullOutputs::add);
        }
        if (fullOutputs.isEmpty()) {
            return;
        }
        for (int p = 0; p < fullOutputs.size(); p += pageSize) {
            int end = Math.min(p + pageSize, fullOutputs.size());
            out.add(new FactoryJeiRecipe(
                    recipeId, src.inputAmount(), inputs, new ArrayList<>(fullOutputs.subList(p, end))));
        }
    }
}
