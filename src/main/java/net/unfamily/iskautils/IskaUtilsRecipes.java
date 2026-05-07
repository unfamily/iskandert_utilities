package net.unfamily.iskautils;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.crafting.StrictShapedRecipe;

public final class IskaUtilsRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, IskaUtils.MOD_ID);

    static {
        SERIALIZERS.register("strict_shaped", () -> StrictShapedRecipe.Serializer.INSTANCE);
    }

    private IskaUtilsRecipes() {}

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
