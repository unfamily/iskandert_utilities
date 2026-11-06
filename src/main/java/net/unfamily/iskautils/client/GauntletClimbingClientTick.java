package net.unfamily.iskautils.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.GauntletOfClimbingItem;
import net.unfamily.iskautils.util.CurioEquipUtil;
import net.unfamily.iskautils.world.NullifierSpatialIndex;

/**
 * Client-side wall climb: movement must run locally every tick (server-only setDeltaMovement is overwritten).
 * Climbing Nullifier zones are mirrored into {@link NullifierSpatialIndex} on the client so prediction matches.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class GauntletClimbingClientTick {
    private GauntletClimbingClientTick() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            return;
        }
        ItemStack gauntlet = CurioEquipUtil.findActiveStack(player, ModItems.GAUNTLET_OF_CLIMBING.get());
        if (!gauntlet.isEmpty()) {
            GauntletOfClimbingItem.tickEquipped(player);
        }
        // Vanilla ladder/vines: suppress upward motion inside player-affecting climbing nullifiers.
        if (player.onClimbable()
                && NullifierSpatialIndex.shouldBlockPlayerGauntletClimb(
                        player.level().dimension(), player.position())) {
            Vec3 motion = player.getDeltaMovement();
            if (motion.y > 0.0D) {
                player.setDeltaMovement(motion.x, 0.0D, motion.z);
            }
        }
    }
}
