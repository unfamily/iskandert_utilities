package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.custom.SaboteurScrewdriverItem;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class SaboteurScrewdriverEvents {
    private SaboteurScrewdriverEvents() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof SaboteurScrewdriverItem)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Player player = event.getEntity();

        if (event.getTarget() instanceof PrimedTnt primed) {
            if (SaboteurScrewdriverItem.tryDefuse(serverLevel, player, primed, event.getItemStack(), event.getHand())) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        if (event.getTarget() instanceof MinecartTNT cart) {
            if (SaboteurScrewdriverItem.tryDefuseMinecart(serverLevel, player, cart, event.getItemStack(), event.getHand())) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }
}
