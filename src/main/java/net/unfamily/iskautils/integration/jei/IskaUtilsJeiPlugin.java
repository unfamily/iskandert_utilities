package net.unfamily.iskautils.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.DeepDrawerExtractorScreen;
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
                new AncientTabletRecipeCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            FactoryJeiRecipes.reloadForClient(mc);
            SuspiciousDeliveryJeiRecipes.reloadForClient(mc);
            AncientTabletJeiRecipes.reloadForClient(mc);
        }
        registration.addRecipes(FactoryRecipeCategory.RECIPE_TYPE, FactoryJeiRecipes.buildAll());
        registration.addRecipes(
                SuspiciousDeliveryRecipeCategory.RECIPE_TYPE,
                SuspiciousDeliveryJeiRecipes.buildAll());
        registration.addRecipes(AncientTabletRecipeCategory.RECIPE_TYPE, AncientTabletJeiRecipes.cached());
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
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
            DeepDrawerExtractorScreen.class,
            new IskaUtilsGhostIngredientHandler<>()
        );
    }
}

