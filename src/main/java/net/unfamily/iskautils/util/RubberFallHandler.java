package net.unfamily.iskautils.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.RubberBootsItem;

/**
 * Handler to intercept and handle the falls of players wearing rubber boots
 */
@EventBusSubscriber
public class RubberFallHandler {

    /**
     * Intercepts the fall event of an entity
     * @param event The fall event
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        // If the entity is not a player, ignore the event
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Get the boots worn by the player
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        
        // If the player is not wearing the rubber boots, ignore the event
        if (boots.getItem() != ModItems.RUBBER_BOOTS.get()) {
            return;
        }
        
        // Handle the fall with the specialized function of the rubber boots
        if (RubberBootsItem.handleFallDamage(boots, player, event.getDistance())) {
            // If the handling function has handled the event correctly, cancel the damage
            event.setCanceled(true);
        }
    }
} 