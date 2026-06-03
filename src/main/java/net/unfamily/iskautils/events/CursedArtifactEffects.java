package net.unfamily.iskautils.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.effect.ModMobEffects;
import net.unfamily.iskautils.integration.apotheosis.ApotheosisCompat;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.util.ArtifactTickIntervals;
import net.unfamily.iskautils.util.AttributeSyncGrace;
import net.unfamily.iskautils.util.CurioEquipUtil;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber
public final class CursedArtifactEffects {
    private static final Logger LOGGER = LoggerFactory.getLogger(CursedArtifactEffects.class);

    private static final ResourceLocation BUSTED_CROWN_HP_ID = ResourceLocation.fromNamespaceAndPath("iska_utils", "busted_crown_hp");
    private static final String TOTEM_OF_PAIN_STAGE = "iska_utils_internal-totem_of_pain_equip";
    private static final String RITUAL_GAUNTLET_STAGE = "iska_utils_internal-ritual_gauntlet_equip";
    private static final String THE_DECEPTION_STAGE = "iska_utils_internal-the_deception_equip";
    private static final String ENTROPIC_RING_STAGE = "iska_utils_internal-entropic_ring_equip";

    private CursedArtifactEffects() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;

        // Busted Crown: bonus HP per cursed artifact (Curios); Cursed Candle also counts in inventory/hands.
        if (!hasBustedCrownEquipped(sp)) {
            if (!AttributeSyncGrace.shouldDeferRemoval(sp)) {
                removeBustedCrownModifier(sp);
            }
            return;
        }

        if (!ArtifactTickIntervals.isDue(sp.level().getGameTime(), ArtifactTickIntervals.FAST_TICKS)) {
            return;
        }

        int cursedCount = countEquippedCursedArtifacts(sp);
        applyBustedCrownModifier(sp, cursedCount);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        // Curse of Pain increases incoming damage per level.
        MobEffectInstance curseInst = null;
        for (MobEffectInstance inst : target.getActiveEffects()) {
            if (inst.getEffect().value() == ModMobEffects.CURSE_OF_PAIN.get()) {
                curseInst = inst;
                break;
            }
        }
        if (curseInst != null) {
            int amp = curseInst.getAmplifier();
            float mult = 1.0f + (float) Config.curseOfPainDamagePerLevel * (amp + 1);
            event.setAmount(event.getAmount() * mult);
        }

        Entity src = event.getSource().getEntity();
        if (!(src instanceof Player player)) return;

        // Totem of Pain: apply stacking Curse of Pain to the hit target with chance.
        if (StageRegistry.playerHasStage(player, TOTEM_OF_PAIN_STAGE)) {
            if (player.getRandom().nextDouble() < Config.totemOfPainProcChance) {
                int amp = 0;
                if (curseInst != null) {
                    amp = Math.min(4, curseInst.getAmplifier() + 1);
                }
                target.addEffect(new MobEffectInstance(
                        ModMobEffects.CURSE_OF_PAIN.getDelegate(),
                        20 * Config.totemOfPainCurseDurationSeconds,
                        amp,
                        true,
                        true,
                        true));
            }
        }

        if (StageRegistry.playerHasStage(player, RITUAL_GAUNTLET_STAGE)) {
            Float critMultiplier = resolveRitualGauntletCritMultiplier(player);
            if (critMultiplier != null && player.getRandom().nextDouble() < Config.ritualGauntletCritChance) {
                event.setAmount(event.getAmount() * critMultiplier);
            }
        }

        applyEntropicRingBonus(event, player, target);
    }

    private static void applyEntropicRingBonus(LivingIncomingDamageEvent event, Player player, LivingEntity target) {
        if (Config.entropicRingDamagePer100Hp <= 0.0D) {
            return;
        }
        if (!StageRegistry.playerHasStage(player, ENTROPIC_RING_STAGE)) {
            return;
        }
        if (player.getHealth() >= target.getHealth()) {
            return;
        }
        double hpGap = target.getHealth() - player.getHealth();
        double flatBonus = Math.floor(hpGap / 100.0D) * Config.entropicRingDamagePer100Hp;
        if (flatBonus <= 0.0D) {
            return;
        }
        float finalBonus = (float) (flatBonus * ApotheosisCompat.getWorldTierMultiplier(player));
        event.setAmount(event.getAmount() + finalBonus);
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide) return;

        // The Deception: after eating, gain Deceived (1 golden heart at level I).
        if (!StageRegistry.playerHasStage(player, THE_DECEPTION_STAGE)) return;
        ItemStack used = event.getItem();
        if (used == null || used.isEmpty()) return;
        if (!used.has(DataComponents.FOOD)) return;

        player.addEffect(new MobEffectInstance(
                ModMobEffects.DECEIVED.getDelegate(),
                20 * Config.theDeceptionAbsorptionDurationSeconds,
                0,
                true,
                true,
                true));
    }

    private static Float resolveRitualGauntletCritMultiplier(Player player) {
        boolean harmful = false;
        boolean beneficialOrNeutral = false;
        for (MobEffectInstance inst : player.getActiveEffects()) {
            switch (inst.getEffect().value().getCategory()) {
                case HARMFUL -> harmful = true;
                case BENEFICIAL, NEUTRAL -> beneficialOrNeutral = true;
            }
        }
        if (harmful) {
            return (float) Config.ritualGauntletCritDamageHarmful;
        }
        if (beneficialOrNeutral) {
            return (float) Config.ritualGauntletCritDamageBeneficialNeutral;
        }
        return null;
    }

    private static boolean hasBustedCrownEquipped(ServerPlayer player) {
        if (!ModUtils.isCuriosLoaded()) {
            return false;
        }
        boolean[] found = {false};
        CurioEquipUtil.forEachEquippedCurioStack(player, stack -> {
            if (stack.is(ModItems.BUSTED_CROWN.get())) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static int countEquippedCursedArtifacts(ServerPlayer player) {
        return CurioEquipUtil.countEquippedCursedArtifacts(player);
    }

    private static void applyBustedCrownModifier(ServerPlayer player, int cursedCount) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        AttributeModifier existing = maxHealth.getModifier(BUSTED_CROWN_HP_ID);
        double amount = Config.bustedCrownHpPerCursedArtifact * Math.max(0, cursedCount);

        if (existing != null) {
            // Replace if amount changed.
            if (existing.amount() == amount) return;
            maxHealth.removeModifier(BUSTED_CROWN_HP_ID);
        }
        maxHealth.addTransientModifier(new AttributeModifier(BUSTED_CROWN_HP_ID, amount, AttributeModifier.Operation.ADD_VALUE));
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void removeBustedCrownModifier(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        if (maxHealth.getModifier(BUSTED_CROWN_HP_ID) != null) {
            maxHealth.removeModifier(BUSTED_CROWN_HP_ID);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    // Cursed artifact equip stages are synced each tick by CurioEquipStageSync.
}

