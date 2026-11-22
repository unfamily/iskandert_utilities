package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.GhostBrazierData;
import net.unfamily.iskautils.data.GhostBrazierData.CastState;
import net.unfamily.iskautils.effect.ModMobEffects;
import net.unfamily.iskautils.util.GhostBrazierFx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side Ghost Brazier cast / ethereal pool / combat interrupt / anti-TP.
 * Spectator movement is tracked via position delta (deltaMovement is unreliable in spectator).
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class GhostBrazierController {
    private static final int BAR_LENGTH = 20;
    /** Blocks per tick at "standard" spectator cruise ≈ 1× pool drain. */
    private static final double STANDARD_SPECTATOR_SPEED = 0.35D;
    private static final double STILL_THRESHOLD = 0.02D;

    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();

    private GhostBrazierController() {}

    public static void requestToggle(ServerPlayer player) {
        if (!GhostBrazierItem.hasGhostBrazier(player)) {
            return;
        }
        CastState state = GhostBrazierData.getCastState(player);
        if (state == CastState.CAST_IN) {
            return;
        }
        if (state == CastState.ETHEREAL || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            revertToPhysical(player, true);
            return;
        }
        int castTicks = Math.max(0, Config.ghostBrazierCastTicks);
        if (castTicks <= 0) {
            enterEthereal(player);
            return;
        }
        GhostBrazierData.setCastState(player, CastState.CAST_IN);
        GhostBrazierData.setCastStartTick(player, player.level().getGameTime());
        GhostBrazierFx.playCastStart(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) {
            return;
        }

        boolean hasItem = GhostBrazierItem.hasGhostBrazier(sp);
        GhostBrazierData.setHasGhostBrazier(sp, hasItem);

        CastState state = GhostBrazierData.getCastState(sp);
        if (state == CastState.IDLE) {
            LAST_POS.remove(sp.getUUID());
            return;
        }

        if (!hasItem && (state == CastState.ETHEREAL || state == CastState.CAST_IN)) {
            if (state == CastState.ETHEREAL || sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                revertToPhysical(sp, true);
            } else {
                interruptCast(sp);
            }
            return;
        }

        long now = sp.level().getGameTime();

        if (state == CastState.CAST_IN) {
            int castTicks = Math.max(0, Config.ghostBrazierCastTicks);
            long start = GhostBrazierData.getCastStartTick(sp);
            int elapsed = (int) Math.max(0L, now - start);
            float progress = castTicks <= 0 ? 1.0F : Math.min(1.0F, elapsed / (float) castTicks);
            GhostBrazierFx.playCastTick(sp, progress);
            displayPercentBar(sp, 1.0F - progress, progress >= 1.0F);
            if (elapsed >= castTicks) {
                enterEthereal(sp);
            }
            return;
        }

        if (state == CastState.ETHEREAL) {
            GhostBrazierFx.playLoop(sp);
            double remaining = GhostBrazierData.getRemainingPoolTicks(sp);
            double speed = spectatorSpeed(sp);
            if (speed >= STILL_THRESHOLD) {
                double factor = Math.max(0.5D, speed / STANDARD_SPECTATOR_SPEED);
                remaining -= factor;
                GhostBrazierData.setRemainingPoolTicks(sp, remaining);
            }
            int poolMax = Math.max(1, Config.ghostBrazierDurationTicks);
            float pct = poolMax <= 0 ? 0.0F : (float) Math.max(0.0D, remaining) / (float) poolMax;
            displayPercentBar(sp, pct, remaining <= 0.0D);
            if (remaining <= 0.0D) {
                revertToPhysical(sp, true);
            }
        }
    }

    private static double spectatorSpeed(ServerPlayer player) {
        UUID id = player.getUUID();
        Vec3 current = player.position();
        Vec3 last = LAST_POS.put(id, current);
        if (last == null) {
            return 0.0D;
        }
        return current.distanceTo(last);
    }

    @SubscribeEvent
    public static void onPvpInterrupt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player attacker) || attacker == victim) {
            return;
        }
        if (GhostBrazierData.getCastState(victim) != CastState.CAST_IN) {
            return;
        }
        interruptCast(victim);
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!GhostBrazierData.isEthereal(player)) {
            return;
        }
        event.setCanceled(true);
        player.displayClientMessage(Component.translatable("message.iska_utils.ghost_brazier.teleport_blocked"), true);
    }

    private static void enterEthereal(ServerPlayer player) {
        GameType current = player.gameMode.getGameModeForPlayer();
        if (current != GameType.SPECTATOR) {
            GhostBrazierData.setPreviousGameMode(player, current);
            player.setGameMode(GameType.SPECTATOR);
        }
        double pool = Math.max(1, Config.ghostBrazierDurationTicks);
        GhostBrazierData.setRemainingPoolTicks(player, pool);
        GhostBrazierData.setCastState(player, CastState.ETHEREAL);
        LAST_POS.put(player.getUUID(), player.position());
        GhostBrazierFx.playEnter(player);
        player.displayClientMessage(Component.translatable("message.iska_utils.ghost_brazier.became_ethereal"), true);
    }

    private static void revertToPhysical(ServerPlayer player, boolean applySlowFalling) {
        GameType previous = GhostBrazierData.getPreviousGameMode(player);
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.setGameMode(previous);
        }
        GhostBrazierFx.playExit(player);
        GhostBrazierData.clearEtherealSession(player);
        LAST_POS.remove(player.getUUID());
        player.displayClientMessage(Component.translatable("message.iska_utils.ghost_brazier.became_physical"), true);
        if (applySlowFalling && Config.ghostBrazierExitSlowFallingTicks > 0) {
            player.addEffect(new MobEffectInstance(
                    ModMobEffects.ETHEREAL_SLOW_FALLING,
                    Config.ghostBrazierExitSlowFallingTicks,
                    0,
                    false,
                    true,
                    true));
        }
    }

    private static void interruptCast(ServerPlayer player) {
        GhostBrazierFx.playCastInterrupt(player);
        GhostBrazierData.setCastState(player, CastState.IDLE);
        LAST_POS.remove(player.getUUID());
        player.displayClientMessage(Component.translatable("message.iska_utils.ghost_brazier.cast_interrupted"), true);
    }

    private static void displayPercentBar(ServerPlayer player, float percentage, boolean done) {
        float pct = Math.max(0.0F, Math.min(1.0F, percentage));
        int filled = Math.round(pct * BAR_LENGTH);
        String percentText = String.format(" %.0f%% ", pct * 100.0F);
        var color = done ? ChatFormatting.GREEN : ChatFormatting.AQUA;
        MutableComponent message = Component.literal("");
        message.append(Component.literal(percentText).withStyle(color));
        for (int i = 0; i < filled; i++) {
            message.append(Component.literal("█").withStyle(color));
        }
        for (int i = filled; i < BAR_LENGTH; i++) {
            message.append(Component.literal("█").withStyle(ChatFormatting.DARK_GRAY));
        }
        message.append(Component.literal(percentText).withStyle(color));
        player.displayClientMessage(message, true);
    }
}
