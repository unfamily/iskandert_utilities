package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.Config;

/**
 * Client entry points for optional recipe-viewer integrations (JEI/EMI/REI).
 * Safe to call when those mods are absent — never hard-references their classes.
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
