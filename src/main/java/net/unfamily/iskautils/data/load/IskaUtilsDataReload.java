package net.unfamily.iskautils.data.load;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.command.MacroLoader;
import net.unfamily.iskautils.command.StageActionsLoader;
import net.unfamily.iskautils.data.DynamicPotionPlateScanner;
import net.unfamily.iskautils.iska_utils_stages.StageItemHandler;
import net.unfamily.iskautils.item.CommandItemRegistry;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskalib.structure.StructureLoader;
import net.unfamily.iskautils.structure.StructureMonouseLoader;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeLoader;
import net.unfamily.iskautils.arcane.ArcaneDictionaryLoader;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoader;
import org.slf4j.Logger;

/**
 * Reloads IskaUtils JSON from the {@code load} tree in all datapacks using the current server {@link ResourceManager}.
 */
public final class IskaUtilsDataReload {
    private static final Logger LOGGER = LogUtils.getLogger();

    private IskaUtilsDataReload() {}

    public static void reloadAllFromServer() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("reloadAllFromServer called with no server; skipping datapack merge");
            return;
        }
        ResourceManager rm = server.getResourceManager();

        // Must be safe to call multiple times and during /reload.
        CommandItemRegistry.reloadDefinitions();
        DynamicPotionPlateScanner.loadAll(rm);
        StructureMonouseLoader.loadAll(rm);
        ShopLoader.loadAll(rm);
        MacroLoader.reloadAllMacros();
        StageActionsLoader.loadAll(rm);
        StageItemHandler.loadAll(rm);
        StructureLoader.reloadAllDefinitions(true);
        FactoryLoader.loadFromRecipeManager(server.getRecipeManager(), rm);
        SuspiciousDeliveryLoader.loadAll(rm);
        AncientTabletRecipeLoader.loadAll(rm);
        ArcaneDictionaryLoader.loadAll(rm);
    }
}

