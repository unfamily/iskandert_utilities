package net.unfamily.iskautils.data.load.ancienttablet;

import net.unfamily.iskautils.util.ModLogger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AncientTabletRecipeLoader {
    private static final ModLogger LOGGER = ModLogger.of(AncientTabletRecipeLoader.class);

    private static volatile List<AncientTabletRecipeEntry> ENTRIES = List.of();

    private AncientTabletRecipeLoader() {}

    public static void loadAll(ResourceManager rm) {
        Map<ResourceLocation, JsonElement> merged =
                IskaUtilsLoadJson.collectMergedJsonUnderDirectory(
                        rm,
                        "recipe",
                        IskaUtilsLoadPaths::isJsonUnderRecipeTree);
        loadAllMerged(merged);
    }

    public static void loadAllMerged(Map<ResourceLocation, JsonElement> merged) {
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
        if (!obj.has("type") || !IskaUtilsLoadPaths.TYPE_ANCIENT_TAB.equals(obj.get("type").getAsString())) {
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
            int fuelCost = AncientTabletRecipeEntry.DEFAULT_FUEL_COST;
            if (e.has("fuel_cost")) {
                fuelCost = e.get("fuel_cost").getAsInt();
            } else if (e.has("chaos_cost")) {
                fuelCost = e.get("chaos_cost").getAsInt();
            }
            if (fuelCost < 1) {
                LOGGER.warn("Ancient Tablet entry in {} has fuel_cost < 1, using 1", fileId);
                fuelCost = 1;
            }
            List<AncientTabletRequirement> require =
                    AncientTabletRequirementParser.parseArray(ctx, e.get("require"), true);
            List<AncientTabletRequirement> produce =
                    AncientTabletRequirementParser.parseArray(ctx, e.get("produce"), false);

            net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost gateHost =
                    net.unfamily.iskautils.script.LoadEntryIfParser.parseGateHost(e);
            List<AncientTabIfVariant> ifVariants = parseIfVariants(e, ctx, fileId);

            if (!ifVariants.isEmpty() && (!require.isEmpty() || !produce.isEmpty())) {
                LOGGER.warn(
                        "Ancient Tablet entry in {} has both flat require/produce and if[]; using if[] only",
                        fileId);
                require = List.of();
                produce = List.of();
            }

            if (ifVariants.isEmpty() && (require.isEmpty() || produce.isEmpty())) {
                LOGGER.warn("Ancient Tablet entry in {} skipped (empty require or produce)", fileId);
                continue;
            }
            out.add(new AncientTabletRecipeEntry(
                    fileId, mustOrdered, destroyIfWrong, fuelCost, gateHost, require, produce, ifVariants));
        }
    }

    private static List<AncientTabIfVariant> parseIfVariants(JsonObject e, String ctx, ResourceLocation fileId) {
        if (!e.has("if") || !e.get("if").isJsonArray()) {
            return List.of();
        }
        JsonArray ifArray = e.getAsJsonArray("if");
        List<AncientTabIfVariant> variants = new ArrayList<>();
        for (JsonElement branchEl : ifArray) {
            var branchOpt = net.unfamily.iskautils.script.LoadEntryIfParser.parseTopLevelIfBranch(branchEl, ctx);
            var payloadOpt = net.unfamily.iskautils.script.LoadEntryIfParser.payloadObject(branchEl, ctx);
            if (branchOpt.isEmpty() || payloadOpt.isEmpty()) {
                continue;
            }
            JsonObject payload = payloadOpt.get();
            List<AncientTabletRequirement> req =
                    AncientTabletRequirementParser.parseArray(ctx, payload.get("require"), true);
            List<AncientTabletRequirement> prod =
                    AncientTabletRequirementParser.parseArray(ctx, payload.get("produce"), false);
            if (req.isEmpty() || prod.isEmpty()) {
                LOGGER.warn("Ancient Tablet if branch in {} skipped (empty require or produce)", fileId);
                continue;
            }
            variants.add(new AncientTabIfVariant(branchOpt.get(), req, prod));
        }
        return List.copyOf(variants);
    }

    public static List<ItemStack> exampleInputsForJei(AncientTabletRecipeEntry entry) {
        return AncientTabletRecipeMatcher.expandToExampleStacks(entry.require());
    }
}
