package net.unfamily.iskautils.stage;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskalib.stage.StageCatalog;
import net.unfamily.iskalib.stage.StageReloadHooks;
import net.unfamily.iskautils.command.StageActionDefinition;
import net.unfamily.iskautils.command.StageActionsLoader;
import net.unfamily.iskautils.iska_utils_stages.StageItemHandler;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.shop.ShopStage;
import net.unfamily.iskautils.util.ArtifactEquipStages;
import net.unfamily.iskautils.util.ModLogger;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Wires Utils stage loaders into Library {@link StageCatalog} and {@link StageReloadHooks}.
 */
public final class UtilsStageBridge {
    private static final ModLogger LOGGER = ModLogger.of(UtilsStageBridge.class);

    private UtilsStageBridge() {}

    public static void install() {
        StageCatalog.addContributor(UtilsStageBridge::collectKnownStages);
        StageReloadHooks.setListener(UtilsStageBridge::reloadStageBlock);
        StageCatalog.registerKnownStages(ArtifactEquipStages.allStages());
    }

    private static Collection<String> collectKnownStages() {
        Set<String> stages = new LinkedHashSet<>();
        stages.addAll(ArtifactEquipStages.allStages());

        for (ShopCategory category : ShopLoader.getCategories().values()) {
            addShopStages(stages, category.stages);
        }
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            addShopStages(stages, entry.stages);
            addShopStages(stages, entry.stageRewards);
        }

        for (StageActionDefinition def : StageActionsLoader.getLoadedActions()) {
            for (StageActionDefinition.StageCondition condition : def.getStages()) {
                if (condition != null && condition.stage != null && !condition.stage.isBlank()) {
                    stages.add(condition.stage.trim());
                }
            }
        }

        stages.addAll(StageItemHandler.collectReferencedStages());
        return stages;
    }

    private static void addShopStages(Set<String> out, ShopStage[] stages) {
        if (stages == null) {
            return;
        }
        for (ShopStage stage : stages) {
            if (stage != null && stage.stage != null && !stage.stage.isBlank()) {
                out.add(stage.stage.trim());
            }
        }
    }

    private static int reloadStageBlock(CommandSourceStack source) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ResourceManager rm = server != null ? server.getResourceManager() : null;
        StageActionsLoader.loadAll(rm);
        StageItemHandler.loadAll(rm);
        LOGGER.info("Reloaded stage actions ({}) and stage items", StageActionsLoader.getActionIds().size());
        return 1;
    }
}
