package net.unfamily.iskautils.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.client.gui.AutoShopScreen;
import net.unfamily.iskautils.client.gui.DeepDrawerExtractorScreen;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.client.gui.ModMenuTypes;
import net.unfamily.iskautils.client.gui.ShopEditScreen;
import net.unfamily.iskautils.client.gui.StructurePlacerMachineScreen;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer;

@EmiEntrypoint
public final class IskaUtilsEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
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
