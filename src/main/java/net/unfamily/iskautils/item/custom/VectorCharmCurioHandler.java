package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.client.VectorCharmMovement;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that handles Curios integration for the Vector Charm.
 * This is a proxy that avoids direct dependencies to Curios, so it's safe to load
 * even when Curios is not present.
 */
public class VectorCharmCurioHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmCurioHandler.class);
    
    /**
     * Registers the Vector Charm as a curio through reflection.
     * Called during mod initialization if Curios is present.
     */
    public static void register() {
        if (!ModUtils.isCuriosLoaded()) return;
        
        try {
            // LOGGER.info("Registering Vector Charm as Curio");
            
            // Registration happens through Curios API, but it's handled
            // automatically by JSON tags, so nothing special is needed here
            
        } catch (Exception e) {
            LOGGER.error("Failed to register Vector Charm as Curio", e);
        }
    }
    
    /**
     * This method is called when the curio is equipped.
     * Can be called through reflection by the Curios API.
     * @param stack The Vector Charm item stack
     * @param entity The entity wearing the curio
     */
    public static void onEquip(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && entity.level().isClientSide) {
            // LOGGER.debug("Vector Charm equipped as Curio by {}", player.getScoreboardName());
        }
    }
    
    /**
     * This method is called when the curio is removed.
     * Can be called through reflection by the Curios API.
     * @param stack The Vector Charm item stack
     * @param entity The entity that was wearing the curio
     */
    public static void onUnequip(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && entity.level().isClientSide) {
            // LOGGER.debug("Vector Charm unequipped as Curio by {}", player.getScoreboardName());
        }
    }
    
    /**
     * This method is called every tick when the curio is equipped.
     * Can be called through reflection by the Curios API.
     * @param stack The Vector Charm item stack
     * @param entity The entity wearing the curio
     */
    public static void curioTick(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            // Apply movement (includes energy consumption)
            VectorCharmMovement.applyMovement(player, stack);
        }
    }
    

} 