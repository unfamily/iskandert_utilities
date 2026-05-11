package net.unfamily.iskautils.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
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
import net.unfamily.iskautils.data.load.FactoryLoader;

/**
 * Datapack-only recipe: Factory machine mappings. Never matches in-world crafting.
 *
 * <p>JSON supports two shapes (can be combined): canonical {@code recipes} + {@code select} + {@code output},
 * and legacy {@code sources} + {@code colors} + {@code id}.
 */
public final class FactorySourcesRecipe implements Recipe<SingleRecipeInput> {
    private record TemplateRow(String input, int amount, List<FactoryLoader.Output> colors, int energyPerOperation) {}

    private static final Codec<FactoryLoader.Output> SELECT_ENTRY_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Identifier.CODEC.fieldOf("output").forGetter(FactoryLoader.Output::id),
                            Codec.INT.optionalFieldOf("amount", 1).forGetter(FactoryLoader.Output::amount))
                    .apply(i, FactoryLoader.Output::new));

    private static final Codec<TemplateRow> NEW_ROW_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Codec.STRING.fieldOf("input").forGetter(TemplateRow::input),
                            Codec.INT.optionalFieldOf("amount", 1).forGetter(TemplateRow::amount),
                            Codec.list(SELECT_ENTRY_CODEC).fieldOf("select").forGetter(TemplateRow::colors),
                            Codec.INT.optionalFieldOf("energy_per_operation", 1).forGetter(TemplateRow::energyPerOperation))
                    .apply(i, TemplateRow::new));

    private static final Codec<FactoryLoader.Output> LEGACY_COLOR_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Identifier.CODEC.fieldOf("id").forGetter(FactoryLoader.Output::id),
                            Codec.INT.optionalFieldOf("amount", 1).forGetter(FactoryLoader.Output::amount))
                    .apply(i, FactoryLoader.Output::new));

    private static final Codec<TemplateRow> LEGACY_ROW_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Codec.STRING.fieldOf("input").forGetter(TemplateRow::input),
                            Codec.INT.optionalFieldOf("amount", 1).forGetter(TemplateRow::amount),
                            Codec.list(LEGACY_COLOR_CODEC).fieldOf("colors").forGetter(TemplateRow::colors),
                            Codec.INT.optionalFieldOf("energy_per_operation", 1).forGetter(TemplateRow::energyPerOperation))
                    .apply(i, TemplateRow::new));

    private static FactorySourcesRecipe fromDecodedRows(List<TemplateRow> recipes, List<TemplateRow> sources) {
        List<TemplateRow> merged = new ArrayList<>(recipes != null ? recipes : List.of());
        merged.addAll(sources != null ? sources : List.of());
        if (merged.isEmpty()) {
            throw new IllegalArgumentException("Factory recipe must define non-empty \"recipes\" and/or \"sources\"");
        }
        return new FactorySourcesRecipe(merged);
    }

    private static final Codec<FactorySourcesRecipe> DIRECT_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Codec.list(NEW_ROW_CODEC)
                                    .optionalFieldOf("recipes", List.of())
                                    .forGetter(r -> r.templateRows),
                            Codec.list(LEGACY_ROW_CODEC)
                                    .optionalFieldOf("sources", List.of())
                                    .forGetter(r -> List.of()))
                    .apply(i, FactorySourcesRecipe::fromDecodedRows));

    public static final MapCodec<FactorySourcesRecipe> MAP_CODEC =
            RecordCodecBuilder.mapCodec(i -> i.group(
                            Codec.list(NEW_ROW_CODEC)
                                    .optionalFieldOf("recipes", List.of())
                                    .forGetter(r -> r.templateRows),
                            Codec.list(LEGACY_ROW_CODEC)
                                    .optionalFieldOf("sources", List.of())
                                    .forGetter(r -> List.of()))
                    .apply(i, FactorySourcesRecipe::fromDecodedRows));

    public static final StreamCodec<RegistryFriendlyByteBuf, FactorySourcesRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(DIRECT_CODEC);

    public static final RecipeSerializer<FactorySourcesRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final List<TemplateRow> templateRows;
    private final List<FactoryLoader.Source> compiledSources;

    public FactorySourcesRecipe(List<TemplateRow> templateRows) {
        this.templateRows = List.copyOf(templateRows);
        Identifier logId = Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory_sources");
        List<FactoryLoader.Source> built = new ArrayList<>();
        for (TemplateRow row : this.templateRows) {
            FactoryLoader.tryCompileSource(logId, row.input(), row.amount(), row.colors(), row.energyPerOperation()).ifPresent(built::add);
        }
        this.compiledSources = List.copyOf(built);
    }

    public List<FactoryLoader.Source> compiledSources() {
        return compiledSources;
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
        return ModFactoryRecipes.FACTORY_SOURCES_TYPE.get();
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
