package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.entity.EntropicCreeper;

/**
 * Replaces the vanilla Entropic Creeper detonation with ExplosionSystem.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class EntropicCreeperExplosionHandler {
    private EntropicCreeperExplosionHandler() {}

    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        Entity source = event.getExplosion().getDirectSourceEntity();
        if (!(source instanceof EntropicCreeper creeper)) {
            return;
        }
        event.setCanceled(true);
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            creeper.createEntropyExplosion(serverLevel);
        }
    }
}
