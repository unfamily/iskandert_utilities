package net.unfamily.iskautils.data.load.ancienttablet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AncientTabletRecipeLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile List<AncientTabletRecipeEntry> ENTRIES = List.of();

    private AncientTabletRecipeLoader() {}

    public static void loadAll(ResourceManager rm) {
        Map<ResourceLocation, JsonElement> merged =
                IskaUtilsLoadJson.collectMergedJsonUnderDirectory(
                        rm,
                        "recipe",
                        IskaUtilsLoadPaths::isJsonUnderRecipeTree);
        List<AncientTabletRecipeEntry> out = new ArrayList<>();
        for (var entry : IskaUtilsLoadJson.orderedEntries(merged)) {
            parseFile(entry.getKey(), entry.getValue(), out);
        }
        ENTRIES = List.copyOf(out);
        LOGGER.info("Loaded {} Ancient Tablet recipe entries from datapacks", ENTRIES.size());
    }

    public static List<AncientTabletRecipeEntry> getEntries() {
        return ENTRIES;
    }

    private static void parseFile(ResourceLocation fileId, JsonElement root, List<AncientTabletRecipeEntry> out) {
        if (!root.isJsonObject()) {
            return;
        }
        JsonObject obj = root.getAsJsonObject();
        if (!obj.has("type") || !IskaUtilsLoadPaths.TYPE_ANCIENT_TABLET.equals(obj.get("type").getAsString())) {
            return;
        }
        if (!obj.has("entries") || !obj.get("entries").isJsonArray()) {
            LOGGER.warn("Ancient Tablet file {} has no entries array", fileId);
            return;
        }
        JsonArray entries = obj.getAsJsonArray("entries");
        String ctx = fileId.toString();
        for (JsonElement el : entries) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject e = el.getAsJsonObject();
            boolean mustOrdered = e.has("must_ordered") && e.get("must_ordered").getAsBoolean();
            boolean destroyIfWrong = e.has("destroy_if_wrong") && e.get("destroy_if_wrong").getAsBoolean();
            List<AncientTabletRequirement> require =
                    AncientTabletRequirementParser.parseArray(ctx, e.get("require"), true);
            List<AncientTabletRequirement> produce =
                    AncientTabletRequirementParser.parseArray(ctx, e.get("produce"), false);
            if (require.isEmpty() || produce.isEmpty()) {
                LOGGER.warn("Ancient Tablet entry in {} skipped (empty require or produce)", fileId);
                continue;
            }
            out.add(new AncientTabletRecipeEntry(fileId, mustOrdered, destroyIfWrong, require, produce));
        }
    }

    public static List<ItemStack> exampleInputsForJei(AncientTabletRecipeEntry entry) {
        return AncientTabletRecipeMatcher.expandToExampleStacks(entry.require());
    }
}
