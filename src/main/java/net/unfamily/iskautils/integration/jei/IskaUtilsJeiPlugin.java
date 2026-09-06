package net.unfamily.iskautils.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.AutoShopScreen;
import net.unfamily.iskautils.client.gui.DeepDrawerExtractorScreen;
import net.unfamily.iskautils.client.gui.ShopEditScreen;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.client.gui.StructurePlacerMachineScreen;
import net.unfamily.iskautils.integration.jei.ghost.IskaUtilsGhostIngredientHandler;
import net.unfamily.iskautils.item.ModItems;

@JeiPlugin
public final class IskaUtilsJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new FactoryRecipeCategory(helper),
                new SuspiciousDeliveryRecipeCategory(helper),
                new AncientTabletRecipeCategory(helper),
                new ArcaneDictionaryRecipeCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            FactoryJeiRecipes.reloadForClient(mc);
            SuspiciousDeliveryJeiRecipes.reloadForClient(mc);
            AncientTabletJeiRecipes.reloadForClient(mc);
            ArcaneDictionaryJeiRecipes.reloadForClient(mc);
        }
        registration.addRecipes(FactoryRecipeCategory.RECIPE_TYPE, FactoryJeiRecipes.buildAll());
        registration.addRecipes(
                SuspiciousDeliveryRecipeCategory.RECIPE_TYPE,
                SuspiciousDeliveryJeiRecipes.buildAll());
        registration.addRecipes(AncientTabletRecipeCategory.RECIPE_TYPE, AncientTabletJeiRecipes.cached());
        registration.addRecipes(ArcaneDictionaryRecipeCategory.RECIPE_TYPE, ArcaneDictionaryJeiRecipes.cached());
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        IskaUtilsJeiDynamicRefresh.setRuntime(jeiRuntime);
        if (ModList.get().isLoaded("pattern_crafter")) {
            var legacyStacks = BuiltInRegistries.ITEM.entrySet().stream()
                    .filter(entry -> "pattern_crafter".equals(entry.getKey().location().getNamespace()))
                    .map(entry -> new ItemStack(entry.getValue()))
                    .toList();
            jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, legacyStacks);

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                var legacyRecipes = minecraft.level.getRecipeManager()
                        .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
                        .filter(holder -> {
                            ItemStack result = holder.value().getResultItem(minecraft.level.registryAccess());
                            ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
                            return id != null && "pattern_crafter".equals(id.getNamespace());
                        })
                        .toList();
                jeiRuntime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, legacyRecipes);
            }
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (net.unfamily.iskautils.Config.factoryStonecutterEnabled) {
            registration.addRecipeCatalyst(new ItemStack(ModItems.FACTORY.get()), RecipeTypes.STONECUTTING);
        }
        registration.addRecipeCatalyst(new ItemStack(ModItems.FACTORY.get()), FactoryRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.SUSPICIOUS_DELIVERY.get()),
                SuspiciousDeliveryRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.ANCIENT_TABLET.get()),
                AncientTabletRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.ANCIENT_TABLE.get()),
                AncientTabletRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.ARCANE_DICTIONARY.get()),
                ArcaneDictionaryRecipeCategory.RECIPE_TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
                DeepDrawerExtractorScreen.class,
                new IskaUtilsGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(
                AutoShopScreen.class,
                new IskaUtilsGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(
                ShopEditScreen.class,
                new IskaUtilsGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(
                ImprovedPatternCrafterScreen.class,
                new IskaUtilsGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(
                StructurePlacerMachineScreen.class,
                new IskaUtilsGhostIngredientHandler<>());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new PatternCrafterRecipeTransferHandler(registration.getTransferHelper()),
                RecipeTypes.CRAFTING);
    }
}

