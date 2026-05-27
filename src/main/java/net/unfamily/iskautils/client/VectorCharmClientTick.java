package net.unfamily.iskautils.events;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.client.VectorCharmMovement;
import net.unfamily.iskautils.item.custom.VectorCharmItem;

/**
 * Applies vector charm movement when the charm is equipped outside equipment {@code inventoryTick}.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class VectorCharmClientTick {
    private VectorCharmClientTick() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            return;
        }
        ItemStack charm = VectorCharmItem.getActiveVectorCharm(player, 0);
        if (charm != null) {
            VectorCharmMovement.applyMovement(player, charm);
        }
    }
}
