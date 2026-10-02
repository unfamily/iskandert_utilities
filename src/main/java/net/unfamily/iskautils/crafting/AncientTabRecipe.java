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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabIfVariant;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRequirement;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRequirementParser;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import net.unfamily.iskautils.script.LoadEntryIfParser;

/**
 * Datapack-only Ancient Tablet mapping. One recipe = one entry (after Library bundle split).
 */
public final class AncientTabRecipe implements Recipe<SingleRecipeInput> {
    private static final Codec<JsonElement> JSON = ExtraCodecs.JSON;

    public record EntryFields(
            boolean mustOrdered,
            boolean destroyIfWrong,
            int fuelCost,
            List<JsonElement> require,
            List<JsonElement> produce,
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
                                Codec.BOOL.optionalFieldOf("must_ordered", false).forGetter(EntryFields::mustOrdered),
                                Codec.BOOL
                                        .optionalFieldOf("destroy_if_wrong", false)
                                        .forGetter(EntryFields::destroyIfWrong),
                                Codec.INT
                                        .optionalFieldOf("fuel_cost", AncientTabletRecipeEntry.DEFAULT_FUEL_COST)
                                        .forGetter(EntryFields::fuelCost),
                                JSON.listOf().optionalFieldOf("require", List.of()).forGetter(EntryFields::require),
                                JSON.listOf().optionalFieldOf("produce", List.of()).forGetter(EntryFields::produce),
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

    private record BundleFields(List<EntryFields> entries) {
        static final Codec<BundleFields> CODEC = RecordCodecBuilder.create(
                i -> i.group(EntryFields.CODEC
                                .listOf()
                                .optionalFieldOf("entries", List.of())
                                .forGetter(BundleFields::entries))
                        .apply(i, BundleFields::new));
    }

    private static DataResult<AncientTabRecipe> fromEither(Either<EntryFields, BundleFields> either) {
        return either.map(
                fields -> DataResult.success(new AncientTabRecipe(List.of(fields))),
                bundle -> {
                    if (bundle.entries().isEmpty()) {
                        return DataResult.error(() -> "Ancient tab recipe must define entry fields or non-empty entries");
                    }
                    return DataResult.success(new AncientTabRecipe(bundle.entries()));
                });
    }

    private static final Codec<AncientTabRecipe> DIRECT_CODEC = ExtraCodecs.JSON.flatXmap(
            el -> {
                if (el == null || !el.isJsonObject()) {
                    return DataResult.error(() -> "Ancient tab recipe must be a JSON object");
                }
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("entries") && obj.get("entries").isJsonArray()) {
                    return BundleFields.CODEC
                            .parse(com.mojang.serialization.JsonOps.INSTANCE, obj)
                            .flatMap(b -> fromEither(Either.right(b)));
                }
                return EntryFields.CODEC
                        .parse(com.mojang.serialization.JsonOps.INSTANCE, obj)
                        .flatMap(e -> fromEither(Either.left(e)));
            },
            recipe -> {
                if (recipe.entries.size() == 1) {
                    return EntryFields.CODEC.encodeStart(
                            com.mojang.serialization.JsonOps.INSTANCE, recipe.entries.getFirst());
                }
                return BundleFields.CODEC.encodeStart(
                        com.mojang.serialization.JsonOps.INSTANCE, new BundleFields(recipe.entries));
            });

    public static final MapCodec<AncientTabRecipe> MAP_CODEC = new MapCodec<>() {
        @Override
        public <T> DataResult<AncientTabRecipe> decode(
                com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.MapLike<T> input) {
            return DIRECT_CODEC.parse(ops, ops.createMap(input.entries()));
        }

        @Override
        public <T> com.mojang.serialization.RecordBuilder<T> encode(
                AncientTabRecipe input,
                com.mojang.serialization.DynamicOps<T> ops,
                com.mojang.serialization.RecordBuilder<T> prefix) {
            DIRECT_CODEC.encodeStart(ops, input).result().ifPresent(value -> ops.getMap(value)
                    .result()
                    .ifPresent(map -> map.entries().forEach(e -> prefix.add(e.getFirst(), e.getSecond()))));
            return prefix;
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
            return java.util.stream.Stream.empty();
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, AncientTabRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(DIRECT_CODEC);

    public static final RecipeSerializer<AncientTabRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final List<EntryFields> entries;
    private final List<AncientTabletRecipeEntry> compiledEntries;

    public AncientTabRecipe(List<EntryFields> entries) {
        this.entries = List.copyOf(entries);
        Identifier logId = Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "ancient_tab");
        List<AncientTabletRecipeEntry> built = new ArrayList<>();
        for (EntryFields entry : this.entries) {
            compileEntry(logId, entry).ifPresent(built::add);
        }
        this.compiledEntries = List.copyOf(built);
    }

    public List<AncientTabletRecipeEntry> compiledEntries() {
        return compiledEntries;
    }

    public Optional<AncientTabletRecipeEntry> compiledEntry() {
        return compiledEntries.isEmpty() ? Optional.empty() : Optional.of(compiledEntries.getFirst());
    }

    private static Optional<AncientTabletRecipeEntry> compileEntry(Identifier logId, EntryFields entry) {
        String ctx = logId.toString();
        int fuelCost = Math.max(1, entry.fuelCost());

        JsonObject gateJson = new JsonObject();
        if (!entry.stages().isEmpty()) {
            JsonArray stages = new JsonArray();
            entry.stages().forEach(stages::add);
            gateJson.add("stages", stages);
        }
        gateJson.addProperty("stages_logic", entry.stagesLogic());
        if (!entry.mods().isEmpty()) {
            JsonArray mods = new JsonArray();
            entry.mods().forEach(mods::add);
            gateJson.add("mods", mods);
        }
        gateJson.addProperty("mods_logic", entry.modsLogic());
        SuspiciousDeliveryStageHost gateHost = LoadEntryIfParser.parseGateHost(gateJson);

        List<AncientTabletRequirement> require = List.of();
        List<AncientTabletRequirement> produce = List.of();
        List<AncientTabIfVariant> ifVariants = List.of();

        if (!entry.ifBranches().isEmpty()) {
            JsonObject fake = new JsonObject();
            JsonArray ifArray = new JsonArray();
            entry.ifBranches().forEach(ifArray::add);
            fake.add("if", ifArray);
            ifVariants = parseIfVariants(fake, ctx, logId);
        } else {
            JsonArray reqArr = new JsonArray();
            effectiveRequire(entry).forEach(reqArr::add);
            JsonArray prodArr = new JsonArray();
            effectiveProduce(entry).forEach(prodArr::add);
            require = AncientTabletRequirementParser.parseArray(ctx, reqArr, true);
            produce = AncientTabletRequirementParser.parseArray(ctx, prodArr, false);
        }

        if (!ifVariants.isEmpty() && (!require.isEmpty() || !produce.isEmpty())) {
            require = List.of();
            produce = List.of();
        }
        if (ifVariants.isEmpty() && (require.isEmpty() || produce.isEmpty())) {
            return Optional.empty();
        }
        return Optional.of(new AncientTabletRecipeEntry(
                logId,
                entry.mustOrdered(),
                entry.destroyIfWrong(),
                fuelCost,
                gateHost,
                require,
                produce,
                ifVariants));
    }


    /** Prefer KubeJS mirror {@code results} when present (replaceOutput). */
    private static List<JsonElement> effectiveProduce(EntryFields entry) {
        if (!entry.results().isEmpty()) {
            return List.copyOf(entry.results());
        }
        return entry.produce();
    }

    /** Prefer KubeJS mirror {@code ingredients} when present (replaceInput). */
    private static List<JsonElement> effectiveRequire(EntryFields entry) {
        if (!entry.ingredients().isEmpty()) {
            return List.copyOf(entry.ingredients());
        }
        return entry.require();
    }

    private static List<AncientTabIfVariant> parseIfVariants(JsonObject e, String ctx, Identifier fileId) {
        if (!e.has("if") || !e.get("if").isJsonArray()) {
            return List.of();
        }
        JsonArray ifArray = e.getAsJsonArray("if");
        List<AncientTabIfVariant> variants = new ArrayList<>();
        for (JsonElement branchEl : ifArray) {
            var branchOpt = LoadEntryIfParser.parseTopLevelIfBranch(branchEl, ctx);
            var payloadOpt = LoadEntryIfParser.payloadObject(branchEl, ctx);
            if (branchOpt.isEmpty() || payloadOpt.isEmpty()) {
                continue;
            }
            JsonObject payload = payloadOpt.get();
            List<AncientTabletRequirement> req =
                    AncientTabletRequirementParser.parseArray(ctx, payload.get("require"), true);
            List<AncientTabletRequirement> prod =
                    AncientTabletRequirementParser.parseArray(ctx, payload.get("produce"), false);
            if (req.isEmpty() || prod.isEmpty()) {
                continue;
            }
            variants.add(new AncientTabIfVariant(branchOpt.get(), req, prod));
        }
        return List.copyOf(variants);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return ModAncientTabRecipes.ANCIENT_TAB_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
