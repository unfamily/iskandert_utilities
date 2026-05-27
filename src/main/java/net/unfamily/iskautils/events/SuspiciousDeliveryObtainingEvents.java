package net.unfamily.iskautils.events;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.unfamily.iskautils.item.ModItems;

/**
 * Obtaining logic for Suspicious Delivery:
 * - 30% drop from Wandering Trader
 * - Trade entry on Wandering Trader
 */
@EventBusSubscriber
public final class SuspiciousDeliveryObtainingEvents {
    private SuspiciousDeliveryObtainingEvents() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (entity.getType() != net.minecraft.world.entity.EntityType.WANDERING_TRADER) {
            return;
        }
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity.getRandom().nextFloat() < 0.30f) {
            ItemStack stack = new ItemStack(ModItems.SUSPICIOUS_DELIVERY.get());
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                    entity.level(),
                    entity.getX(),
                    entity.getY() + 0.5,
                    entity.getZ(),
                    stack));
        }
    }
}

