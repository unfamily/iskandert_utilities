package net.unfamily.iskautils.integration.jei;

import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Refreshes dynamic JEI categories when player context (stages/mods) may have changed.
 */
public final class IskaUtilsJeiDynamicRefresh {
    private static IJeiRuntime runtime;
    private static long lastRefreshMs;
    private static final long DEBOUNCE_MS = 500L;

    private static final List<SuspiciousDeliveryJeiRecipe> LAST_DELIVERY = new ArrayList<>();
    private static final List<AncientTabletJeiRecipe> LAST_ANCIENT = new ArrayList<>();
    private static final List<FactoryJeiRecipe> LAST_FACTORY = new ArrayList<>();
    private static final List<ArcaneDictionaryJeiRecipe> LAST_ARCANE = new ArrayList<>();

    private IskaUtilsJeiDynamicRefresh() {}

    public static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        seedLastFromRuntime();
    }

    private static void seedLastFromRuntime() {
        if (runtime == null) {
            return;
        }
        var rm = runtime.getRecipeManager();
        LAST_DELIVERY.clear();
        LAST_DELIVERY.addAll(rm.createRecipeLookup(SuspiciousDeliveryRecipeCategory.RECIPE_TYPE).get().toList());
        LAST_ANCIENT.clear();
        LAST_ANCIENT.addAll(rm.createRecipeLookup(AncientTabletRecipeCategory.RECIPE_TYPE).get().toList());
        LAST_FACTORY.clear();
        LAST_FACTORY.addAll(rm.createRecipeLookup(FactoryRecipeCategory.RECIPE_TYPE).get().toList());
        LAST_ARCANE.clear();
        LAST_ARCANE.addAll(rm.createRecipeLookup(ArcaneDictionaryRecipeCategory.RECIPE_TYPE).get().toList());
    }

    public static void scheduleRefresh(Minecraft mc) {
        if (mc == null || runtime == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs < DEBOUNCE_MS) {
            return;
        }
        lastRefreshMs = now;
        mc.execute(() -> refreshAll(mc));
    }

    public static void refreshAll(Minecraft mc) {
        if (runtime == null || mc == null) {
            return;
        }
        refreshSuspiciousDelivery(mc);
        refreshAncientTab(mc);
        refreshFactory(mc);
        refreshArcaneDictionary(mc);
    }

    public static void refreshSuspiciousDelivery(Minecraft mc) {
        replaceRecipes(
                SuspiciousDeliveryRecipeCategory.RECIPE_TYPE,
                LAST_DELIVERY,
                SuspiciousDeliveryJeiRecipes.buildAll());
    }

    public static void refreshAncientTab(Minecraft mc) {
        AncientTabletJeiRecipes.reloadForClient(mc);
        replaceRecipes(
                AncientTabletRecipeCategory.RECIPE_TYPE,
                LAST_ANCIENT,
                AncientTabletJeiRecipes.cached());
    }

    public static void refreshFactory(Minecraft mc) {
        FactoryJeiRecipes.reloadForClient(mc);
        replaceRecipes(FactoryRecipeCategory.RECIPE_TYPE, LAST_FACTORY, FactoryJeiRecipes.buildAll());
    }

    public static void refreshArcaneDictionary(Minecraft mc) {
        ArcaneDictionaryJeiRecipes.reloadForClient(mc);
        replaceRecipes(
                ArcaneDictionaryRecipeCategory.RECIPE_TYPE,
                LAST_ARCANE,
                ArcaneDictionaryJeiRecipes.cached());
    }

    private static <T> void replaceRecipes(IRecipeType<T> type, List<T> lastHolder, List<T> rebuilt) {
        var recipeManager = runtime.getRecipeManager();
        if (!lastHolder.isEmpty()) {
            recipeManager.hideRecipes(type, lastHolder);
        }
        lastHolder.clear();
        if (rebuilt != null && !rebuilt.isEmpty()) {
            lastHolder.addAll(rebuilt);
            recipeManager.addRecipes(type, rebuilt);
        }
    }
}
