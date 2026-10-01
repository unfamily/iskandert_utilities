package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.util.NullifierEntityIgnore;
import net.unfamily.iskautils.world.NullifierSpatialIndex;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class FlightNullifierEvents {
    private FlightNullifierEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) {
            return;
        }
        if (!NullifierSpatialIndex.shouldBlockPlayerFlight(player.level().dimension(), player.position())) {
            return;
        }
        if (player.getAbilities().flying || player.getAbilities().mayfly) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        // FlyingMob covers Ghast/Phantom; FlyingMoveControl covers many aerial entities; Blaze floats without either.
        if (!(mob instanceof FlyingMob)
                && !(mob.getMoveControl() instanceof FlyingMoveControl)
                && mob.getType() != EntityType.BLAZE) {
            return;
        }
        if (NullifierEntityIgnore.isIgnored(mob)) {
            return;
        }
        if (!NullifierSpatialIndex.shouldBlockMobFlight(mob.level().dimension(), mob.position())) {
            return;
        }
        Vec3 motion = mob.getDeltaMovement();
        if (motion.y > 0.0D) {
            mob.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
    }
}
