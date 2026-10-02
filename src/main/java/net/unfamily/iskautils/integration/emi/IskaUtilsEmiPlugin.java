package net.unfamily.iskautils.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.client.gui.AutoShopScreen;
import net.unfamily.iskautils.client.gui.DeepDrawerExtractorScreen;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.client.gui.ModMenuTypes;
import net.unfamily.iskautils.client.gui.ShopEditScreen;
import net.unfamily.iskautils.client.gui.StructurePlacerMachineScreen;
import net.unfamily.iskautils.crafting.AncientTabRecipe;
import net.unfamily.iskautils.crafting.FactorySourcesRecipe;
import net.unfamily.iskautils.crafting.ModAncientTabRecipes;
import net.unfamily.iskautils.crafting.ModFactoryRecipes;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer;
import net.unfamily.iskautils.item.ModItems;

@EmiEntrypoint
public final class IskaUtilsEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(FactoryEmiRecipe.CATEGORY);
        registry.addCategory(AncientTabEmiRecipe.CATEGORY);
        registry.addWorkstation(FactoryEmiRecipe.CATEGORY, EmiStack.of(ModItems.FACTORY.get()));
        registry.addWorkstation(AncientTabEmiRecipe.CATEGORY, EmiStack.of(ModItems.ANCIENT_TABLET.get()));
        registry.addWorkstation(AncientTabEmiRecipe.CATEGORY, EmiStack.of(ModItems.ANCIENT_TABLE.get()));

        for (RecipeHolder<FactorySourcesRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(ModFactoryRecipes.FACTORY_TYPE.get())) {
            if (!holder.value().compiledSources().isEmpty()) {
                registry.addRecipe(new FactoryEmiRecipe(holder));
            }
        }
        for (RecipeHolder<AncientTabRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(ModAncientTabRecipes.ANCIENT_TAB_TYPE.get())) {
            if (!holder.value().compiledEntries().isEmpty()) {
                registry.addRecipe(new AncientTabEmiRecipe(holder));
            }
        }

        if (!ModList.get().isLoaded("emi") || !PatternCrafterRecipeViewerTransfer.isTransferEnabled()) {
            return;
        }
        registry.addRecipeHandler(
                ModMenuTypes.IMPROVED_PATTERN_CRAFTER_MENU.get(),
                new PatternCrafterEmiRecipeHandler());
        registry.addDragDropHandler(ImprovedPatternCrafterScreen.class, new IskaUtilsEmiDragDropHandler<>());
        registry.addDragDropHandler(DeepDrawerExtractorScreen.class, new IskaUtilsEmiDragDropHandler<>());
        registry.addDragDropHandler(ShopEditScreen.class, new IskaUtilsEmiDragDropHandler<>());
        registry.addDragDropHandler(AutoShopScreen.class, new IskaUtilsEmiDragDropHandler<>());
        registry.addDragDropHandler(StructurePlacerMachineScreen.class, new IskaUtilsEmiDragDropHandler<>());
    }
}
