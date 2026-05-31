package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.arcane.ArcaneDictionaryActivation;
import net.unfamily.iskautils.arcane.ArcaneDictionaryContents;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEntropy;
import net.unfamily.iskautils.arcane.ArcaneDictionaryPassiveCleanup;
import net.unfamily.iskautils.arcane.ArcaneDictionarySession;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitDispatcher;
import net.unfamily.iskautils.arcane.ArcaneDictionaryConsume;
import net.unfamily.iskautils.util.ArtifactTickIntervals;

@EventBusSubscriber
public final class ArcaneDictionaryEffects {

    private ArcaneDictionaryEffects() {}

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArcaneDictionarySession.setEffectsActive(player.getUUID(), false);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (ArtifactTickIntervals.isDue(gameTime, ArtifactTickIntervals.FAST_TICKS)) {
            ArcaneDictionaryEntropy.tickInventoryRefill(player);
        }

        ArcaneDictionaryActivation.Context activation = ArcaneDictionaryActivation.resolve(player);
        if (activation == null || !ArcaneDictionaryContents.hasTraits(activation.dictionary())) {
            ArcaneDictionaryPassiveCleanup.clear(player);
            return;
        }
        boolean curioFull = activation.curioFull();
        ItemStack dictionary = activation.dictionary();
        int periodConsume = ArcaneDictionaryConsume.computeTotalConsume(dictionary, gameTime, curioFull, player);
        if (periodConsume > 0 && !ArcaneDictionaryEntropy.tickEffectConsume(dictionary, periodConsume, gameTime)) {
            ArcaneDictionaryPassiveCleanup.clear(player);
            return;
        }
        if (!curioFull) {
            ArcaneDictionaryPassiveCleanup.clear(player);
        }
        ArcaneDictionarySession.setEffectsActive(player.getUUID(), true);
        if (ArtifactTickIntervals.isDue(gameTime, ArtifactTickIntervals.FAST_TICKS)) {
            ArcaneDictionaryTraitDispatcher.dispatchPlayerTick(player, dictionary, curioFull);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingIncomingDamageVictim(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (!ArcaneDictionarySession.isEffectsActive(victim.getUUID())) {
            return;
        }
        ArcaneDictionaryActivation.Context activation = resolveForEffects(victim);
        if (activation == null || !ArcaneDictionaryContents.hasTraits(activation.dictionary())) {
            return;
        }
        ArcaneDictionaryTraitDispatcher.dispatchVictimDamaged(
                victim, activation.dictionary(), event, activation.curioFull());
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingIncomingDamageAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!ArcaneDictionarySession.isEffectsActive(attacker.getUUID())) {
            return;
        }
        ArcaneDictionaryActivation.Context activation = resolveForEffects(attacker);
        if (activation == null || !ArcaneDictionaryContents.hasTraits(activation.dictionary())) {
            return;
        }
        ArcaneDictionaryTraitDispatcher.dispatchPlayerAttack(
                attacker, activation.dictionary(), event, activation.curioFull());
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ArcaneDictionaryActivation.Context activation = ArcaneDictionaryActivation.resolve(player);
        if (activation == null || !ArcaneDictionaryContents.hasTraits(activation.dictionary())) {
            return;
        }
        if (!canApplyTraitEffects(player, activation)) {
            return;
        }
        var orb = event.getOrb();
        int adjusted = ArcaneDictionaryTraitDispatcher.adjustExperiencePickup(
                player, activation.dictionary(), orb.getValue(), activation.curioFull());
        orb.value = adjusted;
    }

    private static boolean canApplyTraitEffects(
            ServerPlayer player, ArcaneDictionaryActivation.Context activation) {
        if (ArcaneDictionarySession.isEffectsActive(player.getUUID())) {
            return true;
        }
        long gameTime = player.level().getGameTime();
        int periodConsume = ArcaneDictionaryConsume.computeTotalConsume(
                activation.dictionary(), gameTime, activation.curioFull(), player);
        return periodConsume <= 0
                || ArcaneDictionaryEntropy.canAffordConsume(activation.dictionary(), periodConsume, gameTime);
    }

    private static ArcaneDictionaryActivation.Context resolveForEffects(ServerPlayer player) {
        if (!ArcaneDictionarySession.isEffectsActive(player.getUUID())) {
            return null;
        }
        return ArcaneDictionaryActivation.resolve(player);
    }
}
