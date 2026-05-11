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
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.unfamily.iskautils.crafting.FactorySourcesRecipe;
import org.slf4j.Logger;

/**
 * Factory mappings: server from {@link FactorySourcesRecipe}; client from merged recipe JSON
 * (no full recipe holder list on the client).
 */
public final class FactoryLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FACTORY_TYPE = "iska_utils:factory";
    private static final String FACTORY_SOURCES_TYPE = "iska_utils:factory_sources";

    public record Output(ResourceLocation id, int amount) {}

    public record Source(Selector selector, int inputAmount, List<Output> outputs, int energyPerOperation) {}

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

    /** Client GUI/JEI and server fallback: merged JSON under each namespace {@code recipe/factory/}. */
    public static void loadFromMergedRecipeResources(ResourceManager rm) {
        Map<ResourceLocation, JsonElement> merged =
                IskaUtilsLoadJson.collectMergedJsonUnderDirectory(rm, "recipe", IskaUtilsLoadPaths::isFactoryRecipeFile);
        List<Source> out = new ArrayList<>();
        for (var entry : IskaUtilsLoadJson.orderedEntries(merged)) {
            parseFactoryRecipeJson(entry.getKey(), entry.getValue(), out);
        }
        SOURCES = List.copyOf(out);
        LOGGER.info("Loaded {} Factory sources from merged recipe datapacks", SOURCES.size());
    }

    public static Optional<Source> tryCompileSource(ResourceLocation logId, String input, int amount, List<Output> colors, int energyPerOperation) {
        if (colors == null || colors.isEmpty()) {
            LOGGER.warn("Factory source in {} has no outputs for input {}", logId, input);
            return Optional.empty();
        }
        List<Output> presentOnly = filterToExistingOutputItems(logId, input, colors);
        if (presentOnly.isEmpty()) {
            LOGGER.warn("Factory source in {} has no registered output items for input {} (all ids missing from registry)", logId, input);
            return Optional.empty();
        }
        Selector selector = parseSelector(logId, input);
        if (selector == null) {
            return Optional.empty();
        }
        return Optional.of(new Source(selector, Math.max(1, amount), List.copyOf(presentOnly), Math.max(0, energyPerOperation)));
    }

    /**
     * Drops color rows whose item ids are not in the item registry (e.g. optional Dyenamics compat when mod absent).
     */
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
        if (input.isEmpty()) return Optional.empty();
        for (Source s : SOURCES) {
            if (s.selector().matches(input)) return Optional.of(s);
        }
        return Optional.empty();
    }

    /**
     * Stable key for remembering the last selected output index per extractor source (selector, input amount, outputs).
     */
    public static int sourcePreferenceKey(Source s) {
        int h = 31 * s.inputAmount();
        h = 31 * h + Integer.hashCode(s.energyPerOperation());
        for (Output o : s.outputs()) {
            h = 31 * h + o.id().hashCode();
            h = 31 * h + o.amount();
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
            return BuiltInRegistries.ITEM.getTag(ts.tag())
                    .map(named -> named.stream().map(h -> new ItemStack(h.value(), amt)).toList())
                    .orElseGet(List::of);
        }
        if (s.selector() instanceof Selector.ItemSelector is) {
            return List.of(new ItemStack(is.item(), amt));
        }
        return List.of();
    }

    public static List<ItemStack> previewOutputs(ItemStack heldInput) {
        return findSource(heldInput)
                .map(src -> src.outputs().stream()
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
            if (r.has("select") && r.get("select").isJsonArray()) {
                for (JsonElement se : r.getAsJsonArray("select")) {
                    if (!se.isJsonObject()) continue;
                    JsonObject s = se.getAsJsonObject();
                    String outId = s.has("output") ? s.get("output").getAsString() : "";
                    int outAmt = s.has("amount") ? Math.max(1, s.get("amount").getAsInt()) : 1;
                    ResourceLocation rl = ResourceLocation.tryParse(outId);
                    if (rl == null) continue;
                    outputs.add(new Output(rl, outAmt));
                }
            }

            List<Output> presentOutputs = filterToExistingOutputItems(fileId, input, outputs);
            if (presentOutputs.isEmpty()) {
                LOGGER.warn("Factory recipe in {} has no registered outputs for input {}", fileId, input);
                continue;
            }
            tryCompileSource(fileId, input, amount, presentOutputs, energyOp).ifPresent(out::add);
        }
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
