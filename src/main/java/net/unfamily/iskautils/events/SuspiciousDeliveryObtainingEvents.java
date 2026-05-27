package net.unfamily.iskautils.events;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.common.BasicItemListing;
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
        if (entity.level().isClientSide) {
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

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        // Basic trade: 16 emeralds -> 1 suspicious_delivery
        event.getGenericTrades().add(new BasicItemListing(
                new ItemStack(net.minecraft.world.item.Items.EMERALD, 16),
                new ItemStack(ModItems.SUSPICIOUS_DELIVERY.get(), 1),
                8,
                1,
                0.05f));
    }
}

