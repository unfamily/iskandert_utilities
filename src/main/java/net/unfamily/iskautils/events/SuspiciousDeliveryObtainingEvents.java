package net.unfamily.iskautils.events;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryTradeUtil;

/**
 * Obtaining logic for Suspicious Delivery:
 * - 50% drop from Wandering Trader
 * - Trade entry on Wandering Trader (datapack uncommon pool, max one offer per trader)
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
        if (entity.getRandom().nextFloat() < 0.50f) {
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
    public static void onTraderJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity().getType() != EntityType.WANDERING_TRADER) {
            return;
        }
        SuspiciousDeliveryTradeUtil.applyTraderTradeLimit((WanderingTrader) event.getEntity());
    }
}

