package net.unfamily.iskautils.crafting;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;

public final class ModFactoryRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, IskaUtils.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, IskaUtils.MOD_ID);

    /** Single serializer instance (NeoForge rejects registering the same serializer twice). */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FactorySourcesRecipe>> FACTORY_SERIALIZER =
            SERIALIZERS.register("factory", () -> FactorySourcesRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FactorySourcesRecipe>> FACTORY_SOURCES_TYPE =
            RECIPE_TYPES.register(
                    "factory_sources",
                    () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory_sources")));

    private ModFactoryRecipes() {}

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        SERIALIZERS.register(modEventBus);
    }
}
