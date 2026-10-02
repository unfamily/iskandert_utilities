package net.unfamily.iskautils.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.FactoryIfBranch;
import net.unfamily.iskautils.data.load.FactoryLoader;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import net.unfamily.iskautils.script.LoadEntryIfParser;

/**
 * Datapack-only Factory mapping. One recipe = one input mapping (after Library bundle split).
 * Never matches in-world crafting.
 */
public final class FactorySourcesRecipe extends CustomRecipe {
    private static final Codec<JsonElement> JSON = ExtraCodecs.JSON;

    /** Decoded fields for a single entry (post-split) or one element of a legacy bundle. */
    public record EntryFields(
            String input,
            int amount,
            int energyPerOperation,
            List<JsonElement> select,
            List<JsonElement> colors,
            List<JsonElement> ifBranches,
            List<JsonElement> stages,
            String stagesLogic,
            List<JsonElement> mods,
            String modsLogic,
            Optional<String> id,
            List<JsonElement> results,
            List<JsonElement> ingredients) {

        public static final Codec<EntryFields> CODEC = RecordCodecBuilder.create(
                i -> i.group(
                                Codec.STRING.fieldOf("input").forGetter(EntryFields::input),
                                Codec.INT.optionalFieldOf("amount", 1).forGetter(EntryFields::amount),
                                Codec.INT
                                        .optionalFieldOf("energy_per_operation", 1)
                                        .forGetter(EntryFields::energyPerOperation),
                                JSON.listOf().optionalFieldOf("select", List.of()).forGetter(EntryFields::select),
                                JSON.listOf().optionalFieldOf("colors", List.of()).forGetter(EntryFields::colors),
                                JSON.listOf().optionalFieldOf("if", List.of()).forGetter(EntryFields::ifBranches),
                                JSON.listOf().optionalFieldOf("stages", List.of()).forGetter(EntryFields::stages),
                                Codec.STRING.optionalFieldOf("stages_logic", "AND").forGetter(EntryFields::stagesLogic),
                                JSON.listOf().optionalFieldOf("mods", List.of()).forGetter(EntryFields::mods),
                                Codec.STRING.optionalFieldOf("mods_logic", "AND").forGetter(EntryFields::modsLogic),
                                Codec.STRING.optionalFieldOf("id").forGetter(EntryFields::id),
                                JSON.listOf().optionalFieldOf("results", List.of()).forGetter(EntryFields::results),
                                JSON.listOf()
                                        .optionalFieldOf("ingredients", List.of())
                                        .forGetter(EntryFields::ingredients))
                        .apply(i, EntryFields::new));
    }

    private static DataResult<FactorySourcesRecipe> fromEither(
            Either<EntryFields, BundleFields> either) {
        return either.map(
                fields -> DataResult.success(new FactorySourcesRecipe(List.of(fields))),
                bundle -> {
                    List<EntryFields> rows = new ArrayList<>();
                    rows.addAll(bundle.recipes());
                    rows.addAll(bundle.sources());
                    if (rows.isEmpty()) {
                        return DataResult.error(() -> "Factory recipe must define entry fields or non-empty recipes/sources");
                    }
                    return DataResult.success(new FactorySourcesRecipe(rows));
                });
    }

    private record BundleFields(List<EntryFields> recipes, List<EntryFields> sources) {
        static final Codec<BundleFields> CODEC = RecordCodecBuilder.create(
                i -> i.group(
                                EntryFields.CODEC
                                        .listOf()
                                        .optionalFieldOf("recipes", List.of())
                                        .forGetter(BundleFields::recipes),
                                EntryFields.CODEC
                                        .listOf()
                                        .optionalFieldOf("sources", List.of())
                                        .forGetter(BundleFields::sources))
                        .apply(i, BundleFields::new));
    }

    private static final Codec<FactorySourcesRecipe> DIRECT_CODEC = Codec.either(EntryFields.CODEC, BundleFields.CODEC)
            .flatXmap(FactorySourcesRecipe::fromEither, recipe -> {
                if (recipe.entries.size() == 1) {
                    return DataResult.success(Either.left(recipe.entries.getFirst()));
                }
                return DataResult.success(Either.right(new BundleFields(recipe.entries, List.of())));
            });

    public static final StreamCodec<RegistryFriendlyByteBuf, FactorySourcesRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(DIRECT_CODEC);

    /**
     * MapCodec that accepts either a flat single entry or a legacy {@code recipes}/{@code sources} bundle
     * (bundles are normally split by Library before parse).
     */
    public static final MapCodec<FactorySourcesRecipe> FIELD_CODEC = new MapCodec<>() {
        @Override
        public <T> DataResult<FactorySourcesRecipe> decode(
                com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.MapLike<T> input) {
            return DIRECT_CODEC.parse(ops, ops.createMap(input.entries()));
        }

        @Override
        public <T> com.mojang.serialization.RecordBuilder<T> encode(
                FactorySourcesRecipe input,
                com.mojang.serialization.DynamicOps<T> ops,
                com.mojang.serialization.RecordBuilder<T> prefix) {
            DataResult<T> encoded = DIRECT_CODEC.encodeStart(ops, input);
            encoded.result().ifPresent(value -> ops.getMap(value).result().ifPresent(map -> map.entries()
                    .forEach(e -> prefix.add(e.getFirst(), e.getSecond()))));
            return prefix;
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
            return java.util.stream.Stream.empty();
        }
    };

    private final List<EntryFields> entries;
    private final List<FactoryLoader.Source> compiledSources;
    private final Ingredient inputIngredient;
    private final ItemStack resultPreview;

    public FactorySourcesRecipe(List<EntryFields> entries) {
        super(CraftingBookCategory.MISC);
        this.entries = List.copyOf(entries);
        ResourceLocation logId = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory");
        List<FactoryLoader.Source> built = new ArrayList<>();
        for (EntryFields entry : this.entries) {
            compileEntry(logId, entry).ifPresent(built::add);
        }
        this.compiledSources = List.copyOf(built);
        this.inputIngredient = built.isEmpty() ? Ingredient.EMPTY : ingredientFromSource(built.getFirst());
        this.resultPreview = built.isEmpty() ? ItemStack.EMPTY : firstOutputStack(built.getFirst());
    }

    public List<FactoryLoader.Source> compiledSources() {
        return compiledSources;
    }

    public Optional<FactoryLoader.Source> compiledSource() {
        return compiledSources.isEmpty() ? Optional.empty() : Optional.of(compiledSources.getFirst());
    }

    private static Optional<FactoryLoader.Source> compileEntry(ResourceLocation logId, EntryFields entry) {
        JsonObject fake = toGateJson(entry);
        SuspiciousDeliveryStageHost gateHost = LoadEntryIfParser.parseGateHost(fake);
        String input = effectiveInput(entry);

        List<FactoryLoader.Output> flat = new ArrayList<>();
        List<FactoryIfBranch> ifBranches = List.of();

        if (!entry.ifBranches().isEmpty()) {
            JsonArray ifArray = new JsonArray();
            for (JsonElement el : entry.ifBranches()) {
                ifArray.add(el);
            }
            ifBranches = FactoryLoader.parseIfBranchesPublic(ifArray, gateHost, logId, input);
        } else {
            List<JsonElement> selectRows = effectiveSelect(entry);
            if (!selectRows.isEmpty()) {
                JsonArray select = new JsonArray();
                for (JsonElement el : selectRows) {
                    select.add(el);
                }
                flat.addAll(FactoryLoader.parseSelectArrayPublic(select, logId, input));
            } else if (!entry.colors().isEmpty()) {
                for (JsonElement el : entry.colors()) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject c = el.getAsJsonObject();
                    String outId = c.has("id") ? c.get("id").getAsString() : "";
                    int outAmt = c.has("amount") ? Math.max(1, c.get("amount").getAsInt()) : 1;
                    ResourceLocation rl = ResourceLocation.tryParse(outId);
                    if (rl != null) {
                        flat.add(new FactoryLoader.Output(rl, outAmt));
                    }
                }
            }
        }

        return FactoryLoader.tryCompileSource(
                logId,
                input,
                entry.amount(),
                flat,
                entry.energyPerOperation(),
                gateHost,
                ifBranches);
    }

    /** Prefer KubeJS mirror {@code results} when present (replaceOutput). */
    private static List<JsonElement> effectiveSelect(EntryFields entry) {
        if (!entry.results().isEmpty()) {
            List<JsonElement> rows = new ArrayList<>();
            for (JsonElement el : entry.results()) {
                String id = mirrorItemId(el);
                if (id == null || id.isBlank()) {
                    continue;
                }
                JsonObject row = new JsonObject();
                row.addProperty("output", id);
                row.addProperty("amount", 1);
                rows.add(row);
            }
            return rows;
        }
        return entry.select();
    }

    /** Prefer KubeJS mirror {@code ingredients} when present (replaceInput). */
    private static String effectiveInput(EntryFields entry) {
        if (!entry.ingredients().isEmpty()) {
            String id = mirrorItemId(entry.ingredients().getFirst());
            if (id != null && !id.isBlank()) {
                return id;
            }
        }
        return entry.input();
    }

    private static String mirrorItemId(JsonElement el) {
        if (el == null) {
            return null;
        }
        if (el.isJsonPrimitive()) {
            return el.getAsString().trim();
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("id") && obj.get("id").isJsonPrimitive()) {
                return obj.get("id").getAsString().trim();
            }
            if (obj.has("item") && obj.get("item").isJsonPrimitive()) {
                return obj.get("item").getAsString().trim();
            }
            if (obj.has("output") && obj.get("output").isJsonPrimitive()) {
                return obj.get("output").getAsString().trim();
            }
        }
        return null;
    }

    private static JsonObject toGateJson(EntryFields entry) {
        JsonObject obj = new JsonObject();
        if (!entry.stages().isEmpty()) {
            JsonArray stages = new JsonArray();
            for (JsonElement el : entry.stages()) {
                stages.add(el);
            }
            obj.add("stages", stages);
        }
        obj.addProperty("stages_logic", entry.stagesLogic());
        if (!entry.mods().isEmpty()) {
            JsonArray mods = new JsonArray();
            for (JsonElement el : entry.mods()) {
                mods.add(el);
            }
            obj.add("mods", mods);
        }
        obj.addProperty("mods_logic", entry.modsLogic());
        return obj;
    }

    private static Ingredient ingredientFromSource(FactoryLoader.Source source) {
        if (source.selector() instanceof FactoryLoader.Selector.ItemSelector is) {
            return Ingredient.of(is.item());
        }
        if (source.selector() instanceof FactoryLoader.Selector.TagSelector ts) {
            return Ingredient.of(ts.tag());
        }
        return Ingredient.EMPTY;
    }

    private static ItemStack firstOutputStack(FactoryLoader.Source source) {
        List<FactoryLoader.Output> outs =
                !source.flatOutputs().isEmpty()
                        ? source.flatOutputs()
                        : source.ifBranches().isEmpty()
                                ? List.of()
                                : source.ifBranches().getFirst().outputs();
        if (outs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return FactoryLoader.resolveOutputStack(outs.getFirst()).orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, inputIngredient);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return resultPreview.copy();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeType<? extends CraftingRecipe> getType() {
        return (RecipeType<? extends CraftingRecipe>) (RecipeType<?>) ModFactoryRecipes.FACTORY_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<FactorySourcesRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public MapCodec<FactorySourcesRecipe> codec() {
            return FIELD_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FactorySourcesRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
