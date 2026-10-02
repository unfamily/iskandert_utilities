package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.data.load.FactoryLoader;

/**
 * Client entry points for optional recipe-viewer integrations (JEI/EMI/REI).
 * Safe to call when those mods are absent — never hard-references their classes from callers
 * except through this bridge / dedicated integration packages.
 */
public final class RecipeViewerClientBridge {

    private static Class<?> jeiRecipesGuiClass;
    private static boolean jeiRecipesGuiResolved;

    private RecipeViewerClientBridge() {}

    public static boolean isJeiIntegrationActive() {
        return Config.enableJeiIntegration && ModList.get().isLoaded("jei");
    }

    /**
     * Schedules a JEI dynamic recipe refresh when JEI integration is active.
     */
    public static void scheduleJeiRefresh(Minecraft mc) {
        if (mc == null || !isJeiIntegrationActive()) {
            return;
        }
        net.unfamily.iskautils.integration.jei.IskaUtilsJeiDynamicRefresh.scheduleRefresh(mc);
    }

    /**
     * Opens Factory category recipes in the active recipe viewer.
     * If Factory recipes are empty, opens stonecutter recipes when that fallback is enabled.
     */
    public static void openFactoryRecipes() {
        if (isJeiIntegrationActive()) {
            net.unfamily.iskautils.integration.jei.IskaUtilsJeiDynamicRefresh.showFactoryOrStonecutter();
            return;
        }
        boolean factoryEmpty = FactoryLoader.getSources().isEmpty();
        if (ModList.get().isLoaded("emi")) {
            openEmiFactoryOrStonecutter(factoryEmpty);
            return;
        }
        if (ModList.get().isLoaded("roughlyenoughitems")) {
            openReiFactoryOrStonecutter(factoryEmpty);
        }
    }

    private static void openEmiFactoryOrStonecutter(boolean factoryEmpty) {
        try {
            Class<?> emiApi = Class.forName("dev.emi.emi.api.EmiApi");
            Class<?> emiRecipeCategory = Class.forName("dev.emi.emi.api.recipe.EmiRecipeCategory");
            if (!factoryEmpty) {
                Object factoryCat = Class.forName("net.unfamily.iskautils.integration.emi.FactoryEmiRecipe")
                        .getField("CATEGORY")
                        .get(null);
                emiApi.getMethod("displayRecipeCategory", emiRecipeCategory).invoke(null, factoryCat);
                return;
            }
            if (!Config.factoryStonecutterEnabled) {
                return;
            }
            Object stonecutting = Class.forName("dev.emi.emi.api.recipe.VanillaEmiRecipeCategories")
                    .getField("STONECUTTING")
                    .get(null);
            emiApi.getMethod("displayRecipeCategory", emiRecipeCategory).invoke(null, stonecutting);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // EMI absent or API mismatch — ignore.
        }
    }

    private static void openReiFactoryOrStonecutter(boolean factoryEmpty) {
        try {
            Class<?> categoryId = Class.forName("me.shedaniel.rei.api.common.category.CategoryIdentifier");
            Class<?> viewSearchBuilder = Class.forName("me.shedaniel.rei.api.client.view.ViewSearchBuilder");
            Object builder = viewSearchBuilder.getMethod("builder").invoke(null);
            Object id;
            if (!factoryEmpty) {
                id = Class.forName("net.unfamily.iskautils.integration.rei.FactoryReiCategory")
                        .getField("ID")
                        .get(null);
            } else if (Config.factoryStonecutterEnabled) {
                id = Class.forName("me.shedaniel.rei.plugin.common.BuiltinPlugin")
                        .getField("STONECUTTING")
                        .get(null);
            } else {
                return;
            }
            viewSearchBuilder.getMethod("addCategory", categoryId).invoke(builder, id);
            viewSearchBuilder.getMethod("open").invoke(builder);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // REI absent or API mismatch — ignore.
        }
    }

    /**
     * True when {@code screen} is JEI's RecipesGui (resolved reflectively so missing JEI cannot crash).
     */
    public static boolean isJeiRecipesGui(Screen screen) {
        if (screen == null || !isJeiIntegrationActive()) {
            return false;
        }
        Class<?> clazz = resolveJeiRecipesGuiClass();
        return clazz != null && clazz.isInstance(screen);
    }

    private static Class<?> resolveJeiRecipesGuiClass() {
        if (jeiRecipesGuiResolved) {
            return jeiRecipesGuiClass;
        }
        jeiRecipesGuiResolved = true;
        try {
            jeiRecipesGuiClass = Class.forName("mezz.jei.gui.recipes.RecipesGui");
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            jeiRecipesGuiClass = null;
        }
        return jeiRecipesGuiClass;
    }
}
