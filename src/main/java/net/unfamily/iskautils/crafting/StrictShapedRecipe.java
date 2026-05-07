package net.unfamily.iskautils.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class StrictShapedRecipe implements CraftingRecipe {
    private final StrictShapedPattern pattern;
    private final ItemStack result;
    private final String group;
    private final CraftingBookCategory category;
    private final boolean showNotification;

    public StrictShapedRecipe(String group, CraftingBookCategory category, StrictShapedPattern pattern, ItemStack result) {
        this(group, category, pattern, result, true);
    }

    public StrictShapedRecipe(
            String group, CraftingBookCategory category, StrictShapedPattern pattern, ItemStack result, boolean showNotification) {
        this.group = group;
        this.category = category;
        this.pattern = pattern;
        this.result = result;
        this.showNotification = showNotification;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return StrictShapedRecipe.Serializer.INSTANCE;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.pattern.ingredients();
    }

    @Override
    public boolean showNotification() {
        return this.showNotification;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.pattern.matchesAbsolute(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return this.getResultItem(registries).copy();
    }

    @Override
    public boolean isIncomplete() {
        NonNullList<Ingredient> list = this.getIngredients();
        return list.isEmpty() || list.stream().anyMatch(Ingredient::hasNoItems);
    }

    public static class Serializer implements RecipeSerializer<StrictShapedRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<StrictShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(
                p -> p.group(
                                Codec.STRING.optionalFieldOf("group", "").forGetter(StrictShapedRecipe::getGroup),
                                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(StrictShapedRecipe::category),
                                StrictShapedPattern.MAP_CODEC.forGetter(r -> r.pattern),
                                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                                Codec.BOOL.optionalFieldOf("show_notification", Boolean.TRUE).forGetter(StrictShapedRecipe::showNotification))
                        .apply(p, StrictShapedRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, StrictShapedRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<StrictShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StrictShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static StrictShapedRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory cat = buffer.readEnum(CraftingBookCategory.class);
            StrictShapedPattern pat = StrictShapedPattern.STREAM_CODEC.decode(buffer);
            ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);
            boolean notif = buffer.readBoolean();
            return new StrictShapedRecipe(group, cat, pat, stack, notif);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, StrictShapedRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            StrictShapedPattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeBoolean(recipe.showNotification);
        }
    }
}
