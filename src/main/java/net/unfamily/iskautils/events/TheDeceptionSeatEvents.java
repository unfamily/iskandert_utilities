package net.unfamily.iskautils.events;

import net.unfamily.iskautils.entity.DeceptionSeatEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.block.TheDeceptionSeatUtil;

@EventBusSubscriber
public final class TheDeceptionSeatEvents {
    private TheDeceptionSeatEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        TheDeceptionSeatUtil.tickSit(event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (event.isMounting() || !(event.getEntityBeingMounted() instanceof DeceptionSeatEntity seat)) {
            return;
        }
        TheDeceptionSeatUtil.onDismount(seat);
    }
}
