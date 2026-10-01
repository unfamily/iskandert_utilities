package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.custom.GauntletOfClimbingItem;
import net.unfamily.iskautils.util.NullifierEntityIgnore;
import net.unfamily.iskautils.world.NullifierSpatialIndex;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class ClimbingNullifierEvents {
    private ClimbingNullifierEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Vec3 pos = event.getEntity().position();
        ServerLevel level = (ServerLevel) event.getEntity().level();

        if (event.getEntity() instanceof Mob mob) {
            if (!mob.onClimbable()) {
                return;
            }
            if (NullifierEntityIgnore.isIgnored(mob)) {
                return;
            }
            if (!NullifierSpatialIndex.shouldBlockMobClimbing(level.dimension(), pos)) {
                return;
            }
            Vec3 motion = mob.getDeltaMovement();
            if (motion.y > 0.0D) {
                mob.setDeltaMovement(motion.x, 0.0D, motion.z);
            }
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            if (!player.horizontalCollision || !GauntletOfClimbingItem.isClimbingEnabled(player)) {
                return;
            }
            if (!NullifierSpatialIndex.shouldBlockPlayerGauntletClimb(level.dimension(), pos)) {
                return;
            }
            Vec3 motion = player.getDeltaMovement();
            if (motion.y > 0.0D) {
                player.setDeltaMovement(motion.x, 0.0D, motion.z);
            }
        }
    }
}
