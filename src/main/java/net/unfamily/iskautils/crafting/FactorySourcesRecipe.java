package net.unfamily.iskautils.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.FactoryLoader;

/**
 * Datapack-only recipe: Factory machine mappings. Never matches in-world crafting.
 *
 * <p>JSON supports two shapes (can be combined): canonical {@code recipes} + {@code select} + {@code output},
 * and legacy {@code sources} + {@code colors} + {@code id}.
 */
public final class FactorySourcesRecipe extends CustomRecipe {
    private record TemplateRow(String input, int amount, List<FactoryLoader.Output> colors, int energyPerOperation) {}

    private static final Codec<FactoryLoader.Output> SELECT_ENTRY_CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            ResourceLocation.CODEC.fieldOf("output").forGetter(FactoryLoader.Output::id),
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
                            ResourceLocation.CODEC.fieldOf("id").forGetter(FactoryLoader.Output::id),
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

    private final List<TemplateRow> templateRows;
    private final List<FactoryLoader.Source> compiledSources;

    public FactorySourcesRecipe(List<TemplateRow> templateRows) {
        super(CraftingBookCategory.MISC);
        this.templateRows = List.copyOf(templateRows);
        ResourceLocation logId = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory_sources");
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
        return (RecipeType<? extends CraftingRecipe>) (RecipeType<?>) ModFactoryRecipes.FACTORY_SOURCES_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<FactorySourcesRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public MapCodec<FactorySourcesRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FactorySourcesRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
