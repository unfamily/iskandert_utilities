package net.unfamily.iskautils.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskalib.stage.StageRegistry;
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

        if (StageRegistry.playerHasStage(player, SHARPENED_BONE_STAGE)) {
            float dmg = event.getAmount() + (float) Config.sharpenedBoneBonusDamage;
            if (player.getRandom().nextDouble() < Config.sharpenedBoneProcChance) {
                dmg += (float) Config.sharpenedBoneProcBonusDamage;
            }
            event.setAmount(dmg);
        }
    }

    @SubscribeEvent
    public static void onPlayerBreakSpeed(PlayerEvent.BreakSpeed event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (!StageRegistry.playerHasStage(player, THE_ROOTS_STAGE)) return;

        float mult = (float) (Config.theRootsBreakSpeedMinMultiplier
                + player.getRandom().nextDouble() * Config.theRootsBreakSpeedMaxBonus);
        event.setNewSpeed(event.getNewSpeed() * mult);
    }
}

