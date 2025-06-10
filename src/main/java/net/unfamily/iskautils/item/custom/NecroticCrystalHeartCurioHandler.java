package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Class that handles Curios integration for the Necrotic Crystal Heart.
 * This is a proxy that avoids direct dependencies on Curios, so it's safe to load
 * even when Curios is not present.
 */
public class NecroticCrystalHeartCurioHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NecroticCrystalHeartCurioHandler.class);
   
    /**
     * Registers the Necrotic Crystal Heart as a curio.
     * Called during mod initialization if Curios is present.
     */
    public static void register() {
        if (!ModUtils.isCuriosLoaded()) return;
        
        try {
            // Registration happens through Curios APIs, but is handled
            // automatically by JSON tags, so nothing special needs to be done here
        } catch (Exception e) {
            LOGGER.error("Could not register Necrotic Crystal Heart as Curio", e);
        }
    }
} 