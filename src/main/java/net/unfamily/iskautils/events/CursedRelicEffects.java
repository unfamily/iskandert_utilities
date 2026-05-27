package net.unfamily.iskautils.events;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
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
import net.unfamily.iskautils.effect.ModMobEffects;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskalib.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber
public final class CursedRelicEffects {
    private static final Logger LOGGER = LoggerFactory.getLogger(CursedRelicEffects.class);

    private static final Identifier BUSTED_CROWN_HP_ID = Identifier.fromNamespaceAndPath("iska_utils", "busted_crown_hp");
    private static final String TOTEM_OF_PAIN_STAGE = "iska_utils_internal-totem_of_pain_equip";
    private static final String CURSED_CANDLE_STAGE = "iska_utils_internal-cursed_candle_equip";
    private static final String BUSTED_CROWN_STAGE = "iska_utils_internal-busted_crown_equip";
    private static final String RITUAL_GAUNTLET_STAGE = "iska_utils_internal-ritual_gauntlet_equip";
    private static final String THE_DECEPTION_STAGE = "iska_utils_internal-the_deception_equip";

    private CursedRelicEffects() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        if (!StageRegistry.playerHasStage(sp, BUSTED_CROWN_STAGE)) {
            removeBustedCrownModifier(sp);
            return;
        }

        int cursedCount = countCursedRelicsEquipped(sp);
        applyBustedCrownModifier(sp, cursedCount);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        MobEffectInstance curseInst = null;
        for (MobEffectInstance inst : target.getActiveEffects()) {
            if (inst.getEffect().value() == ModMobEffects.CURSE_OF_PAIN.get()) {
                curseInst = inst;
                break;
            }
        }
        if (curseInst != null) {
            int amp = curseInst.getAmplifier();
            float mult = 1.0f + 0.10f * (amp + 1);
            event.setAmount(event.getAmount() * mult);
        }

        Entity src = event.getSource().getEntity();
        if (!(src instanceof Player player)) return;

        if (StageRegistry.playerHasStage(player, TOTEM_OF_PAIN_STAGE)) {
            if (player.getRandom().nextFloat() < 0.25f) {
                int amp = 0;
                if (curseInst != null) {
                    amp = Math.min(4, curseInst.getAmplifier() + 1);
                }
                target.addEffect(new MobEffectInstance(ModMobEffects.CURSE_OF_PAIN.getDelegate(), 20 * 30, amp, true, true, true));
            }
        }

        if (StageRegistry.playerHasStage(player, RITUAL_GAUNTLET_STAGE) && hasAnyBeneficialEffect(player)) {
            if (player.getRandom().nextFloat() < 0.15f) {
                event.setAmount(event.getAmount() * 1.15f);
            }
        }
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        if (!StageRegistry.playerHasStage(player, THE_DECEPTION_STAGE)) return;
        ItemStack used = event.getItem();
        if (used == null || used.isEmpty()) return;
        if (!used.has(DataComponents.FOOD)) return;

        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 0, true, true, true));
    }

    private static boolean hasAnyBeneficialEffect(Player player) {
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                return true;
            }
        }
        return false;
    }

    private static int countCursedRelicsEquipped(Player player) {
        int count = 0;
        if (StageRegistry.playerHasStage(player, TOTEM_OF_PAIN_STAGE)) count++;
        if (StageRegistry.playerHasStage(player, CURSED_CANDLE_STAGE)) count++;
        if (StageRegistry.playerHasStage(player, BUSTED_CROWN_STAGE)) count++;
        if (StageRegistry.playerHasStage(player, RITUAL_GAUNTLET_STAGE)) count++;
        if (StageRegistry.playerHasStage(player, THE_DECEPTION_STAGE)) count++;
        return count;
    }

    private static void applyBustedCrownModifier(ServerPlayer player, int cursedCount) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        AttributeModifier existing = maxHealth.getModifier(BUSTED_CROWN_HP_ID);
        double amount = 2.0 * Math.max(0, cursedCount);

        if (existing != null) {
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

    // Activation is tracked via stages (synced from inventoryTick using Curios-only semantics).
}

