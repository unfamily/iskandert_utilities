package net.unfamily.iskautils.crafting;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * Full-grid shaped pattern (no shrink, no mirror). See {@link StrictShapedRecipe}.
 */
public final class StrictShapedPattern {
    public static final MapCodec<StrictShapedPattern> MAP_CODEC = ShapedRecipePattern.Data.MAP_CODEC
            .flatXmap(
                    StrictShapedPattern::unpackStrict,
                    pattern -> pattern.data.map(DataResult::success)
                            .orElseGet(() -> DataResult.error(() -> "Cannot encode strict shaped pattern without data")));

    public static final StreamCodec<RegistryFriendlyByteBuf, StrictShapedPattern> STREAM_CODEC = StreamCodec.ofMember(
            StrictShapedPattern::toNetwork, StrictShapedPattern::fromNetwork);

    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final Optional<ShapedRecipePattern.Data> data;
    /** True when a cell in the pattern must be empty (space). */
    private final boolean[] emptyMask;
    private final int ingredientCount;

    private StrictShapedPattern(int width, int height, NonNullList<Ingredient> ingredients, Optional<ShapedRecipePattern.Data> data) {
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.data = data;
        this.emptyMask = new boolean[ingredients.size()];
        int c = 0;
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            // Treat "space" as a strict empty requirement; be robust against codec/network variations.
            boolean empty = ing == Ingredient.EMPTY || ing.isEmpty() || ing.hasNoItems();
            this.emptyMask[i] = empty;
            if (!empty) {
                c++;
            }
        }
        this.ingredientCount = c;
    }

    private static DataResult<StrictShapedPattern> unpackStrict(ShapedRecipePattern.Data data) {
        List<String> rows = data.pattern();
        int height = rows.size();
        int width = rows.getFirst().length();
        if (width != ShapedRecipePattern.getMaxWidth() || height != ShapedRecipePattern.getMaxHeight()) {
            return DataResult.error(
                    () -> "iska_utils:strict_shaped requires a full %dx%d pattern (spaces allowed, no auto-trim)"
                            .formatted(ShapedRecipePattern.getMaxWidth(), ShapedRecipePattern.getMaxHeight()));
        }

        NonNullList<Ingredient> list = NonNullList.withSize(width * height, Ingredient.EMPTY);
        CharSet unusedSymbols = new CharArraySet(data.key().keySet());

        for (int y = 0; y < height; y++) {
            String line = rows.get(y);
            for (int x = 0; x < width; x++) {
                char symbol = line.charAt(x);
                if (symbol == ' ') {
                    list.set(x + y * width, Ingredient.EMPTY);
                } else {
                    Ingredient ingredient = data.key().get(symbol);
                    if (ingredient == null) {
                        return DataResult.error(() -> "Pattern references symbol '" + symbol + "' but it's not defined in the key");
                    }
                    unusedSymbols.remove(symbol);
                    list.set(x + y * width, ingredient);
                }
            }
        }

        if (!unusedSymbols.isEmpty()) {
            return DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + unusedSymbols);
        }

        return DataResult.success(new StrictShapedPattern(width, height, list, Optional.of(data)));
    }

    private void toNetwork(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.width);
        buffer.writeVarInt(this.height);
        for (Ingredient ingredient : this.ingredients) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
        }
    }

    private static StrictShapedPattern fromNetwork(RegistryFriendlyByteBuf buffer) {
        int w = buffer.readVarInt();
        int h = buffer.readVarInt();
        NonNullList<Ingredient> list = NonNullList.withSize(w * h, Ingredient.EMPTY);
        list.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
        return new StrictShapedPattern(w, h, list, Optional.empty());
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public NonNullList<Ingredient> ingredients() {
        return this.ingredients;
    }

    public Optional<ShapedRecipePattern.Data> data() {
        return this.data;
    }

    public boolean matchesAbsolute(CraftingInput input) {
        if (input.ingredientCount() != this.ingredientCount) {
            return false;
        }
        if (input instanceof StrictCraftingInputAccessor acc && acc.iska_utils$hasStrictContext()) {
            for (int i = 0; i < this.height; i++) {
                for (int j = 0; j < this.width; j++) {
                    int idx = j + i * this.width;
                    Ingredient expected = this.ingredients.get(idx);
                    ItemStack actual = acc.iska_utils$getStrictSlot(j, i);
                    if (this.emptyMask[idx]) {
                        if (!actual.isEmpty()) {
                            return false;
                        }
                        continue;
                    }
                    if (!expected.test(actual)) {
                        return false;
                    }
                }
            }
            return true;
        }

        if (input.width() == this.width && input.height() == this.height) {
            for (int i = 0; i < this.height; i++) {
                for (int j = 0; j < this.width; j++) {
                    int idx = j + i * this.width;
                    Ingredient expected = this.ingredients.get(idx);
                    ItemStack actual = input.getItem(j, i);
                    if (this.emptyMask[idx]) {
                        if (!actual.isEmpty()) {
                            return false;
                        }
                        continue;
                    }
                    if (!expected.test(actual)) {
                        return false;
                    }
                }
            }
            return true;
        }

        return false;
    }
}
