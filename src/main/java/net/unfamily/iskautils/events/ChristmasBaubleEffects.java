package net.unfamily.iskautils.events;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.util.ArtifactEffectGate;
import net.unfamily.iskautils.util.ArtifactEquipStages;
import net.unfamily.iskautils.util.ArtifactTickIntervals;
import net.unfamily.iskautils.util.AttributeSyncGrace;

/**
 * Curio-stage effects for Christmas baubles.
 */
@EventBusSubscriber
public final class ChristmasBaubleEffects {
    private static final Identifier CLASSIC_ARMOR_ID =
            Identifier.fromNamespaceAndPath("iska_utils", "classic_christmas_bauble_armor");
    private static final Identifier CLASSIC_TOUGHNESS_ID =
            Identifier.fromNamespaceAndPath("iska_utils", "classic_christmas_bauble_toughness");

    private static final double CLASSIC_BONUS = 2.0D;
    private static final double GREEDY_CHANCE = 0.15D;
    private static final int GREEDY_LOOTING_BONUS = 2;

    private ChristmasBaubleEffects() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!ArtifactEffectGate.shouldApply(sp)) {
            return;
        }
        long gameTime = sp.level().getGameTime();
        if (AttributeSyncGrace.shouldRunAttributePass(sp, gameTime, ArtifactTickIntervals.FAST_TICKS)) {
            applyClassic(sp);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntropicDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (player instanceof ServerPlayer sp && !ArtifactEffectGate.shouldApply(sp)) {
            return;
        }
        if (!StageRegistry.playerHasStage(player, ArtifactEquipStages.ENTROPIC_CHRISTMAS_BAUBLE)) {
            return;
        }
        // Uniform roll in [1, 5] flat extra damage on each hit.
        int bonus = 1 + player.getRandom().nextInt(5);
        event.setAmount(event.getAmount() + (float) bonus);
    }

    /**
     * Approximate +2 Looting at 15% on kill by growing dropped stacks.
     */
    @SubscribeEvent
    public static void onGreedyDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (player instanceof ServerPlayer sp && !ArtifactEffectGate.shouldApply(sp)) {
            return;
        }
        if (!StageRegistry.playerHasStage(player, ArtifactEquipStages.GREEDY_CHRISTMAS_BAUBLE)) {
            return;
        }
        if (player.getRandom().nextDouble() >= GREEDY_CHANCE) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            int bonus = player.getRandom().nextInt(GREEDY_LOOTING_BONUS + 1);
            if (bonus <= 0) {
                continue;
            }
            int room = stack.getMaxStackSize() - stack.getCount();
            if (room > 0) {
                stack.grow(Math.min(bonus, room));
            }
        }
    }

    private static void applyClassic(ServerPlayer player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor == null || toughness == null) {
            return;
        }
        boolean equipped = StageRegistry.playerHasStage(player, ArtifactEquipStages.CLASSIC_CHRISTMAS_BAUBLE);
        applyTransient(armor, CLASSIC_ARMOR_ID, CLASSIC_BONUS, equipped, player);
        applyTransient(toughness, CLASSIC_TOUGHNESS_ID, CLASSIC_BONUS, equipped, player);
    }

    private static void applyTransient(
            AttributeInstance attr,
            Identifier id,
            double amount,
            boolean equipped,
            ServerPlayer player) {
        AttributeModifier existing = attr.getModifier(id);
        if (equipped) {
            if (existing == null) {
                attr.addTransientModifier(new AttributeModifier(
                        id, amount, AttributeModifier.Operation.ADD_VALUE));
            }
            AttributeSyncGrace.shouldRemoveEquippedBonus(player, id, true);
        } else if (existing != null && AttributeSyncGrace.shouldRemoveEquippedBonus(player, id, false)) {
            attr.removeModifier(id);
        }
    }
}
