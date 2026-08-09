package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactEffectGate;
import net.unfamily.iskautils.util.ArtifactEquipStages;
import net.unfamily.iskautils.util.ArtifactProcUtil;
import net.unfamily.iskalib.stage.StageRegistry;

/**
 * Handles the Entropic Champagne curio effect:
 * when a BENEFICIAL mob effect expires on the equipped player,
 * 15% chance (configurable) to re-apply it for a random 5–15 second window.
 */
@EventBusSubscriber
public final class EntropicChampagneEffects {

    private EntropicChampagneEffects() {}

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
        MobEffect effect = expiredInst.getEffect().value();
        if (effect.getCategory() != MobEffectCategory.BENEFICIAL) {
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

        sp.addEffect(new MobEffectInstance(
                expiredInst.getEffect(),
                duration,
                expiredInst.getAmplifier(),
                true,
                expiredInst.isVisible(),
                expiredInst.showIcon()));
    }
}
