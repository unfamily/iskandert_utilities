package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
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

        if (event.getEntity() instanceof Mob mob) {
            if (!mob.onClimbable()) {
                return;
            }
            if (NullifierEntityIgnore.isIgnored(mob)) {
                return;
            }
            if (!NullifierSpatialIndex.shouldBlockMobClimbing(mob.level().dimension(), pos)) {
                return;
            }
            clampUpwardMotion(mob);
        }
    }

    /**
     * Runs after curios gauntlet tick so nullifier clamp wins over wall-climb reapplication.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        suppressPlayerClimb(player);
    }

    public static void suppressPlayerClimb(Player player) {
        boolean gauntletClimb =
                player.horizontalCollision && GauntletOfClimbingItem.isClimbingEnabled(player);
        boolean vanillaClimb = player.onClimbable();
        if (!gauntletClimb && !vanillaClimb) {
            return;
        }
        if (!NullifierSpatialIndex.shouldBlockPlayerGauntletClimb(player.level().dimension(), player.position())) {
            return;
        }
        clampUpwardMotion(player);
    }

    private static void clampUpwardMotion(net.minecraft.world.entity.Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y > 0.0D) {
            entity.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
    }
}
