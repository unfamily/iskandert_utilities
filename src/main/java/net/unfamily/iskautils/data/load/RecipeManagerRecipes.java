package net.unfamily.iskautils.data.load;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/** MC 26: {@link RecipeManager} exposes {@link RecipeManager#getRecipes()} only (no typed {@code getAllRecipesFor}). */
public final class RecipeManagerRecipes {
    private RecipeManagerRecipes() {}

    public static <T extends Recipe<?>> List<RecipeHolder<T>> holdersOfType(RecipeManager manager, RecipeType<T> type) {
        List<RecipeHolder<T>> out = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.value().getType().equals(type)) {
                @SuppressWarnings("unchecked")
                RecipeHolder<T> typed = (RecipeHolder<T>) holder;
                out.add(typed);
            }
        }
        return out;
    }
}
