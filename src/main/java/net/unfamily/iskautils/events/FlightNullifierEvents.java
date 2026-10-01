package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
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
        if (!isAerialFlyer(mob)) {
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

    /**
     * NeoForge 26 removed {@code FlyingMob}; detect aerial flyers via move control / navigation /
     * flying-speed attribute, plus Blaze/Phantom which float without those hooks.
     */
    private static boolean isAerialFlyer(Mob mob) {
        if (mob.getMoveControl() instanceof FlyingMoveControl) {
            return true;
        }
        if (mob.getNavigation() instanceof FlyingPathNavigation) {
            return true;
        }
        if (mob instanceof FlyingAnimal) {
            return true;
        }
        if (mob.getAttributes().hasAttribute(Attributes.FLYING_SPEED)) {
            return true;
        }
        EntityType<?> type = mob.getType();
        return type == EntityType.BLAZE || type == EntityType.PHANTOM || type == EntityType.BREEZE;
    }
}
