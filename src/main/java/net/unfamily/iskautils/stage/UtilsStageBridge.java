package net.unfamily.iskautils.stage;

import net.unfamily.iskalib.stage.StageCatalog;
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
 * Thin Utils contributor for stages owned outside Library catalogs (shop + artifact equip gates).
 * Stage actions/items loaders and reload live in IskaLib {@code StageBootstrap}.
 */
public final class UtilsStageBridge {
    private static final ModLogger LOGGER = ModLogger.of(UtilsStageBridge.class);

    private UtilsStageBridge() {}

    public static void install() {
        StageCatalog.addContributor(UtilsStageBridge::collectKnownStages);
        StageCatalog.registerKnownStages(ArtifactEquipStages.allStages());
        LOGGER.info("Utils stage catalog contributor installed");
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
}
