package net.unfamily.iskautils.events;

import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.unfamily.iskautils.block.TheDeceptionSeatUtil;

@EventBusSubscriber
public final class TheDeceptionSeatEvents {
    private TheDeceptionSeatEvents() {}

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (event.isMounting() || !(event.getEntityBeingMounted() instanceof ArmorStand stand)) {
            return;
        }
        if (TheDeceptionSeatUtil.isDeceptionSeat(stand)) {
            TheDeceptionSeatUtil.discardIfEmpty(stand);
        }
    }
}
