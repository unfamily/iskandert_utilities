package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactEffectGate;
import net.unfamily.iskautils.util.ArtifactEquipStages;
import net.unfamily.iskautils.util.LastingCandyPlayerState;
import net.unfamily.iskalib.stage.StageRegistry;

/**
 * Lasting Candy — brief invulnerability on incoming damage while equipped in Curios.
 */
public final class LastingCandyEffects {

    private LastingCandyEffects() {}

    /**
     * @return true if damage was cancelled due to active invulnerability
     */
    public static boolean cancelIfInvulnerable(LivingIncomingDamageEvent event, ServerPlayer player) {
        if (!Config.lastingCandyEnabled) {
            return false;
        }
        long now = player.level().getGameTime();
        if (!LastingCandyPlayerState.isInvulnerable(player, now)) {
            return false;
        }
        event.setAmount(0.0f);
        return true;
    }

    /**
     * Activates invulnerability when damage would apply and the curio is off cooldown.
     */
    public static void tryActivate(LivingIncomingDamageEvent event, ServerPlayer player) {
        if (!Config.lastingCandyEnabled || !ArtifactEffectGate.shouldApply(player)) {
            return;
        }
        if (!StageRegistry.playerHasStage(player, ArtifactEquipStages.LASTING_CANDY)) {
            return;
        }
        if (event.getAmount() <= 0.0f) {
            return;
        }
        long now = player.level().getGameTime();
        if (LastingCandyPlayerState.isInvulnerable(player, now)) {
            event.setAmount(0.0f);
            return;
        }
        if (LastingCandyPlayerState.isOnCooldown(player, now)) {
            return;
        }
        event.setAmount(0.0f);
        LastingCandyPlayerState.beginInvulnerability(
                player,
                now,
                Config.lastingCandyInvulnSeconds,
                Config.lastingCandyCooldownSeconds);
    }
}
