package net.unfamily.iskautils.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.DeepDrawerExtractorScreen;
import net.unfamily.iskautils.integration.jei.ghost.IskaUtilsGhostIngredientHandler;

@JeiPlugin
public final class IskaUtilsJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
            DeepDrawerExtractorScreen.class,
            new IskaUtilsGhostIngredientHandler<>()
        );
    }
}

