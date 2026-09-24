package net.unfamily.iskautils.integration.jei;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.RecipeViewerClientBridge;
import net.unfamily.iskautils.client.gui.AncientTableScreen;
import net.unfamily.iskautils.client.gui.FactoryScreen;

@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT)
public final class IskaUtilsJeiClientHooks {

    private IskaUtilsJeiClientHooks() {}

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!RecipeViewerClientBridge.isJeiIntegrationActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (event.getScreen() instanceof FactoryScreen
                || event.getScreen() instanceof AncientTableScreen
                || RecipeViewerClientBridge.isJeiRecipesGui(event.getScreen())) {
            RecipeViewerClientBridge.scheduleJeiRefresh(mc);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        RecipeViewerClientBridge.scheduleJeiRefresh(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(ClientPlayerNetworkEvent.Clone event) {
        RecipeViewerClientBridge.scheduleJeiRefresh(Minecraft.getInstance());
    }
}
