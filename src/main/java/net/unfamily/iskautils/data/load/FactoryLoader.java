package net.unfamily.iskautils.data.load;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.crafting.FactorySourcesRecipe;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import net.unfamily.iskautils.script.LoadEntryIfParser;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Factory machine input/output mappings: server from {@link FactorySourcesRecipe}; client from merged
 * Scans all {@code recipe/} JSON; uses files with {@code type} {@code iska_utils:factory} or {@code iska_utils:factory_sources}.
 */
public final class FactoryLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FACTORY_TYPE = "iska_utils:factory";
    private static final String FACTORY_SOURCES_TYPE = "iska_utils:factory_sources";

    private static final SuspiciousDeliveryStageHost NO_GATE =
            new SuspiciousDeliveryStageHost(
                    List.of(),
                    CommandItemDefinition.StagesLogic.AND,
                    List.of(),
                    CommandItemDefinition.StagesLogic.AND);

    public record Output(ResourceLocation id, int amount, SuspiciousDeliveryStageHost gateHost) {
        public Output(ResourceLocation id, int amount) {
            this(id, amount, NO_GATE);
        }

        public Output {
            gateHost = gateHost != null ? gateHost : NO_GATE;
        }
    }

    public record Source(
            Selector selector,
            int inputAmount,
            List<Output> flatOutputs,
            int energyPerOperation,
            SuspiciousDeliveryStageHost gateHost,
            List<FactoryIfBranch> ifBranches) {

        public Source {
            flatOutputs = flatOutputs != null ? List.copyOf(flatOutputs) : List.of();
            gateHost = gateHost != null ? gateHost : NO_GATE;
            ifBranches = ifBranches != null ? List.copyOf(ifBranches) : List.of();
        }

        /** Legacy accessor. */
        public List<Output> outputs() {
            return flatOutputs;
        }

        public boolean hasIfBranches() {
            return !ifBranches.isEmpty();
        }

        public boolean hasGate() {
            return !gateHost.isEmpty() || hasIfBranches()
                    || flatOutputs.stream().anyMatch(o -> !o.gateHost().isEmpty());
        }

        public List<Output> resolveOutputs(@Nullable ServerPlayer player) {
            if (hasIfBranches()) {
                if (player == null) {
                    return List.of();
                }
                for (FactoryIfBranch branch : ifBranches) {
                    if (branch.matches(player, gateHost)) {
                        return filterOutputGates(branch.outputs(), player);
                    }
                }
                return List.of();
            }
            return filterOutputGates(flatOutputs, player);
        }

        private static List<Output> filterOutputGates(List<Output> outputs, @Nullable ServerPlayer player) {
            if (outputs == null || outputs.isEmpty()) {
                return List.of();
            }
            if (player == null) {
                List<Output> out = new ArrayList<>();
                for (Output o : outputs) {
                    if (o.gateHost().isEmpty()) {
                        out.add(o);
                    }
                }
                return List.copyOf(out);
            }
            List<Output> eligible = new ArrayList<>();
            for (Output o : outputs) {
                if (o.gateHost().isEmpty() || o.gateHost().isFullyEligible(player)) {
                    eligible.add(o);
                }
            }
            return List.copyOf(eligible);
        }
    }

    public sealed interface Selector permits Selector.ItemSelector, Selector.TagSelector {
        boolean matches(ItemStack stack);

        record ItemSelector(Item item) implements Selector {
            @Override
            public boolean matches(ItemStack stack) {
                return stack.is(item);
            }
        }

        record TagSelector(TagKey<Item> tag) implements Selector {
            @Override
            public boolean matches(ItemStack stack) {
                return stack.is(tag);
            }
        }
    }

    private static volatile List<Source> SOURCES = List.of();

    private FactoryLoader() {}

    public static void loadFromRecipeManager(RecipeManager recipeManager, ResourceManager datapackResources) {
        List<Source> merged = new ArrayList<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof FactorySourcesRecipe r) {
                merged.addAll(r.compiledSources());
            }
        }
        if (!merged.isEmpty()) {
            SOURCES = List.copyOf(merged);
            LOGGER.info("Loaded {} Factory sources from recipe holders", SOURCES.size());
            return;
        }
        LOGGER.warn(
                "No Factory sources from RecipeManager ({} holders); falling back to merged recipe JSON",
                recipeManager.getRecipes().size());
        loadFromMergedRecipeResources(Objects.requireNonNull(datapackResources, "datapackResources"));
    }

    /** Client GUI/JEI and server fallback: parses factory recipe JSON from merged datapacks. */
    public static void loadFromMergedRecipeResources(ResourceManager rm) {
        Map<ResourceLocation, JsonElement> merged =
                IskaUtilsLoadJson.collectMergedJsonUnderDirectory(rm, "recipe", IskaUtilsLoadPaths::isJsonUnderRecipeTree);
        List<Source> out = new ArrayList<>();
        for (var entry : IskaUtilsLoadJson.orderedEntries(merged)) {
            parseFactoryRecipeJson(entry.getKey(), entry.getValue(), out);
        }
        SOURCES = List.copyOf(out);
        LOGGER.info("Loaded {} Factory sources from merged recipe datapacks", SOURCES.size());
    }

    public static Optional<Source> tryCompileSource(
            ResourceLocation logId,
            String input,
            int amount,
            List<Output> flatOutputs,
            int energyPerOperation,
            SuspiciousDeliveryStageHost gateHost,
            List<FactoryIfBranch> ifBranches) {
        if ((flatOutputs == null || flatOutputs.isEmpty()) && (ifBranches == null || ifBranches.isEmpty())) {
            LOGGER.warn("Factory source in {} has no outputs for input {}", logId, input);
            return Optional.empty();
        }
        List<Output> presentFlat = filterToExistingOutputItems(logId, input, flatOutputs != null ? flatOutputs : List.of());
        List<FactoryIfBranch> presentBranches = filterBranchesToExisting(logId, input, ifBranches);
        if (presentFlat.isEmpty() && presentBranches.isEmpty()) {
            LOGGER.warn(
                    "Factory source in {} has no registered output items for input {} (all ids missing from registry)",
                    logId,
                    input);
            return Optional.empty();
        }
        Selector selector = parseSelector(logId, input);
        if (selector == null) {
            return Optional.empty();
        }
        return Optional.of(new Source(
                selector,
                Math.max(1, amount),
                List.copyOf(presentFlat),
                Math.max(0, energyPerOperation),
                gateHost != null ? gateHost : NO_GATE,
                List.copyOf(presentBranches)));
    }

    public static Optional<Source> tryCompileSource(
            ResourceLocation logId, String input, int amount, List<Output> colors, int energyPerOperation) {
        return tryCompileSource(logId, input, amount, colors, energyPerOperation, NO_GATE, List.of());
    }

    private static List<Output> filterToExistingOutputItems(ResourceLocation contextId, String inputContext, List<Output> colors) {
        List<Output> out = new ArrayList<>();
        int skipped = 0;
        for (Output c : colors) {
            if (BuiltInRegistries.ITEM.getOptional(c.id()).isEmpty()) {
                skipped++;
                LOGGER.debug("Factory: skip unknown output {} (input {}) [{}]", c.id(), inputContext, contextId);
            } else {
                out.add(c);
            }
        }
        if (skipped > 0) {
            LOGGER.info(
                    "Factory: skipped {} missing output item id(s) for input {} [{}] (optional mods / datapack drift)",
                    skipped,
                    inputContext,
                    contextId);
        }
        return out;
    }

    public static Optional<Source> findSource(ItemStack input) {
        return findSource(input, null);
    }

    public static Optional<Source> findSource(ItemStack input, @Nullable Level level) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        for (Source s : SOURCES) {
            if (s.selector().matches(input)) {
                return Optional.of(s);
            }
        }
        if (level != null) {
            return FactoryStonecutterSupport.resolve(input, level);
        }
        return Optional.empty();
    }

    /** Stable key for remembering the last selected output index per factory source. */
    public static int sourcePreferenceKey(Source s) {
        int h = 31 * s.inputAmount();
        h = 31 * h + Integer.hashCode(s.energyPerOperation());
        for (Output o : s.flatOutputs()) {
            h = 31 * h + o.id().hashCode();
            h = 31 * h + o.amount();
        }
        for (FactoryIfBranch branch : s.ifBranches()) {
            for (Output o : branch.outputs()) {
                h = 31 * h + o.id().hashCode();
                h = 31 * h + o.amount();
            }
        }
        return switch (s.selector()) {
            case Selector.ItemSelector is -> 31 * h + Integer.hashCode(BuiltInRegistries.ITEM.getId(is.item()));
            case Selector.TagSelector ts -> 31 * h + ts.tag().location().hashCode();
        };
    }

    public static List<Source> getSources() {
        return SOURCES;
    }

    public static List<ItemStack> expandInputForJei(Source s) {
        int amt = Math.max(1, s.inputAmount());
        if (s.selector() instanceof Selector.TagSelector ts) {
            List<ItemStack> tagStacks = new ArrayList<>();
            for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(ts.tag())) {
                tagStacks.add(new ItemStack(h.value(), amt));
            }
            return tagStacks;
        }
        if (s.selector() instanceof Selector.ItemSelector is) {
            return List.of(new ItemStack(is.item(), amt));
        }
        return List.of();
    }

    public static List<ItemStack> previewOutputs(ItemStack heldInput) {
        return previewOutputs(heldInput, null);
    }

    public static List<ItemStack> previewOutputs(ItemStack heldInput, @Nullable Level level) {
        return previewOutputs(heldInput, level, null);
    }

    public static List<ItemStack> previewOutputs(
            ItemStack heldInput, @Nullable Level level, @Nullable ServerPlayer player) {
        return findSource(heldInput, level)
                .map(src -> src.resolveOutputs(player).stream()
                        .map(FactoryLoader::resolveOutputStack)
                        .flatMap(Optional::stream)
                        .toList())
                .orElseGet(List::of);
    }

    private static void parseFactoryRecipeJson(ResourceLocation id, JsonElement root, List<Source> out) {
        if (!root.isJsonObject()) {
            return;
        }
        JsonObject obj = root.getAsJsonObject();
        String type = obj.has("type") ? obj.get("type").getAsString() : "";
        if (FACTORY_TYPE.equals(type)) {
            JsonArray recipes =
                    obj.has("recipes") && obj.get("recipes").isJsonArray() ? obj.getAsJsonArray("recipes") : new JsonArray();
            parseRecipesArray(id, recipes, out);
            return;
        }
        if (!FACTORY_SOURCES_TYPE.equals(type)) {
            return;
        }
        JsonArray sources = obj.has("sources") && obj.get("sources").isJsonArray() ? obj.getAsJsonArray("sources") : new JsonArray();
        parseSourcesArray(id, sources, out);
    }

    private static void parseRecipesArray(ResourceLocation fileId, JsonArray recipes, List<Source> out) {
        for (JsonElement e : recipes) {
            if (!e.isJsonObject()) continue;
            JsonObject r = e.getAsJsonObject();
            String input = r.has("input") ? r.get("input").getAsString() : "";
            int amount = r.has("amount") ? Math.max(1, r.get("amount").getAsInt()) : 1;
            int energyOp = 1;
            if (r.has("energy_per_operation")) {
                energyOp = Math.max(0, r.get("energy_per_operation").getAsInt());
            }

            List<Output> outputs = new ArrayList<>();
            List<FactoryIfBranch> ifBranches = List.of();
            SuspiciousDeliveryStageHost gateHost = NO_GATE;

            boolean hasIf = r.has("if") && r.get("if").isJsonArray();
            boolean hasSelect = r.has("select") && r.get("select").isJsonArray();
            if (hasIf && hasSelect) {
                LOGGER.warn("Factory recipe in {} has both select and if[]; using if[] only", fileId);
            }
            if (hasIf) {
                gateHost = LoadEntryIfParser.parseGateHost(r);
                ifBranches = parseIfBranches(r.getAsJsonArray("if"), gateHost, fileId, input);
            } else if (hasSelect) {
                outputs.addAll(parseSelectArray(r.getAsJsonArray("select"), fileId, input));
            }

            if (ifBranches.isEmpty()) {
                List<Output> presentOutputs = filterToExistingOutputItems(fileId, input, outputs);
                if (presentOutputs.isEmpty()) {
                    LOGGER.warn("Factory recipe in {} has no registered outputs for input {}", fileId, input);
                    continue;
                }
                tryCompileSource(fileId, input, amount, presentOutputs, energyOp, gateHost, List.of())
                        .ifPresent(out::add);
            } else {
                tryCompileSource(fileId, input, amount, List.of(), energyOp, gateHost, ifBranches)
                        .ifPresent(out::add);
            }
        }
    }

    private static List<FactoryIfBranch> parseIfBranches(
            JsonArray ifArray,
            SuspiciousDeliveryStageHost gateHost,
            ResourceLocation fileId,
            String input) {
        List<FactoryIfBranch> branches = new ArrayList<>();
        String ctx = fileId + " input=" + input;
        for (JsonElement branchEl : ifArray) {
            var branchOpt = LoadEntryIfParser.parseTopLevelIfBranch(branchEl, ctx);
            var payloadOpt = LoadEntryIfParser.payloadObject(branchEl, ctx);
            if (branchOpt.isEmpty() || payloadOpt.isEmpty()) {
                continue;
            }
            JsonObject payload = payloadOpt.get();
            if (!payload.has("select") || !payload.get("select").isJsonArray()) {
                LOGGER.warn("Factory if branch in {} missing select array", fileId);
                continue;
            }
            List<Output> branchOutputs = parseSelectArray(payload.getAsJsonArray("select"), fileId, input);
            if (branchOutputs.isEmpty()) {
                continue;
            }
            branches.add(new FactoryIfBranch(branchOpt.get(), branchOutputs));
        }
        return List.copyOf(branches);
    }

    private static List<Output> parseSelectArray(JsonArray selectArray, ResourceLocation fileId, String inputContext) {
        List<Output> outputs = new ArrayList<>();
        for (JsonElement se : selectArray) {
            if (!se.isJsonObject()) {
                continue;
            }
            JsonObject s = se.getAsJsonObject();
            String outId = s.has("output") ? s.get("output").getAsString() : s.has("id") ? s.get("id").getAsString() : "";
            int outAmt = s.has("amount") ? Math.max(1, s.get("amount").getAsInt()) : 1;
            ResourceLocation rl = ResourceLocation.tryParse(outId);
            if (rl == null) {
                continue;
            }
            SuspiciousDeliveryStageHost rowGate = LoadEntryIfParser.parseGateHost(s);
            outputs.add(new Output(rl, outAmt, rowGate));
        }
        return outputs;
    }

    private static List<FactoryIfBranch> filterBranchesToExisting(
            ResourceLocation contextId, String inputContext, List<FactoryIfBranch> branches) {
        if (branches == null || branches.isEmpty()) {
            return List.of();
        }
        List<FactoryIfBranch> out = new ArrayList<>();
        for (FactoryIfBranch branch : branches) {
            List<Output> filtered = filterToExistingOutputItems(contextId, inputContext, branch.outputs());
            if (!filtered.isEmpty()) {
                out.add(new FactoryIfBranch(branch.branch(), filtered));
            }
        }
        return List.copyOf(out);
    }

    private static void parseSourcesArray(ResourceLocation fileId, JsonArray sources, List<Source> out) {
        for (JsonElement e : sources) {
            if (!e.isJsonObject()) continue;
            JsonObject s = e.getAsJsonObject();
            String input = s.has("input") ? s.get("input").getAsString() : "";
            int amount = s.has("amount") ? Math.max(1, s.get("amount").getAsInt()) : 1;
            int energyOp = 1;
            if (s.has("energy_per_operation")) {
                energyOp = Math.max(0, s.get("energy_per_operation").getAsInt());
            }

            List<Output> outputs = new ArrayList<>();
            if (s.has("colors") && s.get("colors").isJsonArray()) {
                for (JsonElement ce : s.getAsJsonArray("colors")) {
                    if (!ce.isJsonObject()) continue;
                    JsonObject c = ce.getAsJsonObject();
                    String outId = c.has("id") ? c.get("id").getAsString() : "";
                    int outAmt = c.has("amount") ? Math.max(1, c.get("amount").getAsInt()) : 1;
                    ResourceLocation rl = ResourceLocation.tryParse(outId);
                    if (rl == null) continue;
                    outputs.add(new Output(rl, outAmt));
                }
            }

            List<Output> presentOutputs = filterToExistingOutputItems(fileId, input, outputs);
            if (presentOutputs.isEmpty()) {
                LOGGER.warn("Factory source in {} has no registered outputs for input {}", fileId, input);
                continue;
            }
            tryCompileSource(fileId, input, amount, presentOutputs, energyOp).ifPresent(out::add);
        }
    }

    private static Selector parseSelector(ResourceLocation fileId, String input) {
        if (input == null || input.isBlank()) {
            LOGGER.warn("Factory source in {} has empty input", fileId);
            return null;
        }

        if (input.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(input.substring(1));
            if (tagId == null) {
                LOGGER.warn("Invalid tag selector {} in {}", input, fileId);
                return null;
            }
            TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
            return new Selector.TagSelector(tag);
        }

        ResourceLocation itemId = ResourceLocation.tryParse(input);
        if (itemId == null) {
            LOGGER.warn("Invalid item selector {} in {}", input, fileId);
            return null;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null) {
            LOGGER.warn("Unknown item {} in {}", input, fileId);
            return null;
        }
        return new Selector.ItemSelector(item);
    }

    public static Optional<ItemStack> resolveOutputStack(Output out) {
        Item item = BuiltInRegistries.ITEM.getOptional(out.id()).orElse(null);
        if (item == null) return Optional.empty();
        return Optional.of(new ItemStack(item, out.amount()));
    }
}
