package net.unfamily.iskautils.events;

import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.util.ModuleQuickInstall;

/**
 * Shift+right-click with a known upgrade module installs into the targeted machine's module slots.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class ModuleQuickInstallHandler {

    private ModuleQuickInstallHandler() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown()) {
            return;
        }
        if (!ModuleQuickInstall.isKnownModuleItem(event.getItemStack())) {
            return;
        }
        boolean installed = ModuleQuickInstall.tryInstall(
                event.getLevel(),
                event.getPos(),
                event.getEntity(),
                event.getHand());
        if (!installed) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
    }
}
