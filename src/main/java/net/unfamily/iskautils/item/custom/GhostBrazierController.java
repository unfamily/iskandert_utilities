package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
import net.unfamily.iskautils.util.ModUtils;

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
    /** Action-bar cooldown hint visibility (~2.5s). */
    private static final int COOLDOWN_OVERLAY_TICKS = 50;

    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    /** Game time when cooldown overlay should clear (empty action bar). */
    private static final Map<UUID, Long> COOLDOWN_OVERLAY_CLEAR_AT = new HashMap<>();

    private GhostBrazierController() {}

    public static void requestToggle(ServerPlayer player) {
        if (!GhostBrazierItem.hasGhostBrazier(player)) {
            return;
        }
        CastState state = GhostBrazierData.getCastState(player);

        if (state == CastState.CAST_IN) {
            interruptCast(player);
            return;
        }

        if (state == CastState.ETHEREAL || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            if (isInsideSolid(player)) {
                player.sendOverlayMessage(Component.translatable("message.iska_utils.ghost_brazier.manual_stuck_warning"));
                return;
            }
            revertToPhysical(player, true);
            return;
        }

        // IDLE — gate activation on per-player cooldown
        long now = player.level().getGameTime();
        long cooldownEnd = GhostBrazierData.getLastGlobalEtherealEndTick(player);
        if (now < cooldownEnd) {
            int cooldownSeconds = (int) Math.ceil((cooldownEnd - now) / 20.0);
            if (cooldownSeconds > 0) {
                sendTimedCooldownMessage(player, cooldownSeconds);
            }
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

        clearCooldownOverlayIfDue(sp);

        boolean hasItem = GhostBrazierItem.hasGhostBrazier(sp);
        GhostBrazierData.setHasGhostBrazier(sp, hasItem);

        CastState state = GhostBrazierData.getCastState(sp);
        if (state == CastState.IDLE) {
            LAST_POS.remove(sp.getUUID());
            return;
        }

        if (!hasItem && (state == CastState.ETHEREAL || state == CastState.CAST_IN)) {
            if (state == CastState.ETHEREAL || sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                finishEthereal(sp, true);
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
            int remainingTicks = Math.max(0, castTicks - elapsed);
            GhostBrazierFx.playCastTick(sp, progress);
            displayTimeBar(sp, remainingTicks, progress, progress >= 1.0F);
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

            // Refresh last free position often so decay/stuck TP does not send the player far back.
            if (!isInsideSolid(sp)) {
                GhostBrazierData.setSafePosition(sp, sp.blockPosition());
            }

            int poolMax = Math.max(1, Config.ghostBrazierDurationTicks);
            float pct = (float) Math.max(0.0D, remaining) / (float) poolMax;
            displayPercentBar(sp, pct, remaining <= 0.0D);
            if (remaining <= 0.0D) {
                finishEthereal(sp, true);
            }
        }
    }

    /**
     * End ethereal: if still inside solids, teleport to the saved safe position first.
     */
    private static void finishEthereal(ServerPlayer player, boolean applySlowFalling) {
        if (isInsideSolid(player)) {
            BlockPos safePos = GhostBrazierData.getSafePosition(player);
            if (safePos != null) {
                player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                player.sendOverlayMessage(Component.translatable("message.iska_utils.ghost_brazier.stuck_teleport"));
            }
        }
        revertToPhysical(player, applySlowFalling);
    }

    /**
     * True if any solid (non-replaceable, non-air) block intersects the player's scaled bounding box.
     */
    private static boolean isInsideSolid(Player player) {
        AABB box = player.getBoundingBox();
        double scale = player.getAttributeValue(Attributes.SCALE);
        // Prefer live BB; if spectator BB is unreliable, synthesize from scale.
        if (box.getXsize() < 0.05D || box.getYsize() < 0.05D) {
            double w = 0.6D * scale;
            double h = 1.8D * scale;
            Vec3 pos = player.position();
            box = new AABB(pos.x - w / 2.0, pos.y, pos.z - w / 2.0, pos.x + w / 2.0, pos.y + h, pos.z + w / 2.0);
        }

        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX - 1.0E-4);
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.floor(box.maxY - 1.0E-4);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ - 1.0E-4);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = player.level().getBlockState(cursor);
                    if (state.isAir() || state.canBeReplaced()) {
                        continue;
                    }
                    if (!state.getCollisionShape(player.level(), cursor).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
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
    public static void onAnyDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (GhostBrazierData.getCastState(victim) == CastState.CAST_IN) {
            interruptCast(victim);
        }
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
        player.sendOverlayMessage(Component.translatable("message.iska_utils.ghost_brazier.teleport_blocked"));
    }

    private static void enterEthereal(ServerPlayer player) {
        GameType current = player.gameMode.getGameModeForPlayer();
        if (current != GameType.SPECTATOR) {
            GhostBrazierData.setPreviousGameMode(player, current);
            player.setGameMode(GameType.SPECTATOR);
        }
        GhostBrazierData.setSafePosition(player, player.blockPosition());
        GhostBrazierData.resetConsecutiveBlockTicks(player);

        double pool = Math.max(1, Config.ghostBrazierDurationTicks);
        GhostBrazierData.setRemainingPoolTicks(player, pool);
        GhostBrazierData.setCastState(player, CastState.ETHEREAL);
        LAST_POS.put(player.getUUID(), player.position());
        GhostBrazierFx.playEnter(player);
        player.sendOverlayMessage(Component.translatable("message.iska_utils.ghost_brazier.became_ethereal"));
    }

    private static void revertToPhysical(ServerPlayer player, boolean applySlowFalling) {
        GameType previous = GhostBrazierData.getPreviousGameMode(player);
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.setGameMode(previous);
        }
        GhostBrazierFx.playExit(player);

        long now = player.level().getGameTime();
        double remaining = Math.max(0.0D, GhostBrazierData.getRemainingPoolTicks(player));
        double poolMax = Math.max(1, Config.ghostBrazierDurationTicks);
        double consumed = Math.max(0.0D, poolMax - remaining);
        long cooldownTicks = (long) (consumed / 2.0);
        GhostBrazierData.setLastGlobalEtherealEndTick(player, now + cooldownTicks);

        GhostBrazierData.clearEtherealSession(player);
        LAST_POS.remove(player.getUUID());
        player.sendOverlayMessage(Component.translatable("message.iska_utils.ghost_brazier.became_physical"));
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
        player.sendOverlayMessage(Component.translatable("message.iska_utils.ghost_brazier.cast_interrupted"));
    }

    private static void displayTimeBar(ServerPlayer player, int remainingTicks, float progress, boolean done) {
        float pct = Math.max(0.0F, Math.min(1.0F, progress));
        int filled = Math.round(pct * BAR_LENGTH);
        String timeText = " " + ModUtils.formatTicksHms(remainingTicks) + " ";
        var color = done ? ChatFormatting.GREEN : ChatFormatting.AQUA;
        MutableComponent message = Component.literal("");
        message.append(Component.literal(timeText).withStyle(color));
        appendBar(message, filled, color);
        message.append(Component.literal(timeText).withStyle(color));
        player.sendOverlayMessage(message);
    }

    private static void displayPercentBar(ServerPlayer player, float percentage, boolean done) {
        float pct = Math.max(0.0F, Math.min(1.0F, percentage));
        int filled = Math.round(pct * BAR_LENGTH);
        String percentText = String.format(" %.0f%% ", pct * 100.0F);
        var color = done ? ChatFormatting.GREEN : ChatFormatting.AQUA;
        MutableComponent message = Component.literal("");
        message.append(Component.literal(percentText).withStyle(color));
        appendBar(message, filled, color);
        message.append(Component.literal(percentText).withStyle(color));
        player.sendOverlayMessage(message);
    }

    private static void appendBar(MutableComponent message, int filled, ChatFormatting color) {
        for (int i = 0; i < filled; i++) {
            message.append(Component.literal("█").withStyle(color));
        }
        for (int i = filled; i < BAR_LENGTH; i++) {
            message.append(Component.literal("█").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void sendTimedCooldownMessage(ServerPlayer player, int cooldownSeconds) {
        player.sendOverlayMessage(Component.translatable(
                "message.iska_utils.ghost_brazier.cooldown_remaining", cooldownSeconds));
        COOLDOWN_OVERLAY_CLEAR_AT.put(player.getUUID(), player.level().getGameTime() + COOLDOWN_OVERLAY_TICKS);
    }

    private static void clearCooldownOverlayIfDue(ServerPlayer player) {
        Long clearAt = COOLDOWN_OVERLAY_CLEAR_AT.get(player.getUUID());
        if (clearAt == null) {
            return;
        }
        if (player.level().getGameTime() >= clearAt) {
            COOLDOWN_OVERLAY_CLEAR_AT.remove(player.getUUID());
            // Only clear if still idle (avoid wiping cast/ethereal bars)
            if (GhostBrazierData.getCastState(player) == CastState.IDLE) {
                player.sendOverlayMessage(Component.empty());
            }
        }
    }
}
