package net.unfamily.iskautils.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1.21.1 stand-in for vanilla {@code PlacementInfo} (added in 26.x).
 */
public final class RecipePlacementInfo {
    public static final RecipePlacementInfo NOT_PLACEABLE = new RecipePlacementInfo(List.of());

    private final List<Ingredient> ingredients;

    private RecipePlacementInfo(List<Ingredient> ingredients) {
        this.ingredients = List.copyOf(ingredients);
    }

    public static RecipePlacementInfo create(List<Ingredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return NOT_PLACEABLE;
        }
        List<Ingredient> present = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient != null && !ingredient.isEmpty()) {
                present.add(ingredient);
            }
        }
        return present.isEmpty() ? NOT_PLACEABLE : new RecipePlacementInfo(present);
    }

    public List<Ingredient> ingredients() {
        return ingredients;
    }
}
