package net.unfamily.iskautils.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small first-pass implementation of relic effects from reliquie.txt.
 * More complex cursed behaviors can be layered later without breaking saves.
 */
@EventBusSubscriber
public class RelicEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelicEvents.class);
    private static final String SHARPENED_BONE_STAGE = "iska_utils_internal-sharpened_bone_equip";
    private static final String THE_ROOTS_STAGE = "iska_utils_internal-the_roots_equip";

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Sharpened Bone: +1 damage, plus a 25% chance to deal extra damage (approximation of "ignore armor").
        if (StageRegistry.playerHasStage(player, SHARPENED_BONE_STAGE)) {
            float dmg = event.getAmount() + 1.0f;
            if (player.getRandom().nextFloat() < 0.25f) {
                dmg += 2.0f;
            }
            event.setAmount(dmg);
        }
    }

    @SubscribeEvent
    public static void onPlayerBreakSpeed(PlayerEvent.BreakSpeed event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (!StageRegistry.playerHasStage(player, THE_ROOTS_STAGE)) return;

        // The Roots: random mining speed boost. Keep it bounded.
        float mult = 1.0f + (player.getRandom().nextFloat() * 1.0f); // 1.0 .. 2.0
        event.setNewSpeed(event.getNewSpeed() * mult);
    }
}

