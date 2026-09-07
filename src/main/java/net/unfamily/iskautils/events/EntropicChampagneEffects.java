package net.unfamily.iskautils.events;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactEffectGate;
import net.unfamily.iskautils.util.ArtifactEquipStages;
import net.unfamily.iskautils.util.ArtifactProcUtil;
import net.unfamily.iskalib.stage.StageRegistry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Entropic Champagne curio: when a BENEFICIAL effect expires, configurable chance
 * to re-apply the same effect after a 1-tick delay (Expired runs before vanilla remove;
 * same-tick re-add is unreliable).
 */
@EventBusSubscriber
public final class EntropicChampagneEffects {

    private static final long APPLY_DELAY_TICKS = 1L;
    private static final List<PendingReapply> PENDING = new ArrayList<>();

    private EntropicChampagneEffects() {}

    private record PendingReapply(
            UUID playerId,
            Holder<MobEffect> effect,
            int amplifier,
            boolean ambient,
            boolean visible,
            boolean showIcon,
            int durationTicks,
            long applyAtGameTime) {}

    @SubscribeEvent
    public static void onMobEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer sp)) {
            return;
        }
        if (!ArtifactEffectGate.shouldApply(sp)) {
            return;
        }
        if (!StageRegistry.playerHasStage(sp, ArtifactEquipStages.ENTROPIC_CHAMPAGNE)) {
            return;
        }

        MobEffectInstance expiredInst = event.getEffectInstance();
        if (expiredInst == null) {
            return;
        }
        Holder<MobEffect> effectHolder = expiredInst.getEffect();
        if (effectHolder.value().getCategory() != MobEffectCategory.BENEFICIAL) {
            return;
        }

        if (!ArtifactProcUtil.rollProc(sp, Config.entropicChampagneProcChance)) {
            return;
        }

        int minTicks = Config.entropicChampagneMinDurationSeconds * 20;
        int maxTicks = Config.entropicChampagneMaxDurationSeconds * 20;
        if (maxTicks < minTicks) {
            maxTicks = minTicks;
        }
        int duration = minTicks + sp.getRandom().nextInt(Math.max(1, maxTicks - minTicks + 1));

        PENDING.add(new PendingReapply(
                sp.getUUID(),
                effectHolder,
                expiredInst.getAmplifier(),
                expiredInst.isAmbient(),
                expiredInst.isVisible(),
                expiredInst.showIcon(),
                duration,
                sp.level().getGameTime() + APPLY_DELAY_TICKS));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) {
            return;
        }
        if (PENDING.isEmpty()) {
            return;
        }

        long now = sp.level().getGameTime();
        UUID id = sp.getUUID();
        Iterator<PendingReapply> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingReapply pending = it.next();
            if (!pending.playerId().equals(id)) {
                continue;
            }
            if (now < pending.applyAtGameTime()) {
                continue;
            }
            it.remove();
            if (!sp.isAlive()) {
                continue;
            }
            if (!ArtifactEffectGate.shouldApply(sp)) {
                continue;
            }
            if (!StageRegistry.playerHasStage(sp, ArtifactEquipStages.ENTROPIC_CHAMPAGNE)) {
                continue;
            }
            sp.addEffect(new MobEffectInstance(
                    pending.effect(),
                    pending.durationTicks(),
                    pending.amplifier(),
                    pending.ambient(),
                    pending.visible(),
                    pending.showIcon()));
        }
    }
}
