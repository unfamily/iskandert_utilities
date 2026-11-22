package net.unfamily.iskautils.util;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Utility class for the mod
 */
public class ModUtils {
    private static final ModLogger LOGGER = ModLogger.of(ModUtils.class);
    
    /**
     * Curios mod ID for integration
     */
    private static final String CURIOS_MOD_ID = "curios";
    
    /**
     * Nature's Compass mod ID for integration
     */
    private static final String NATURES_COMPASS_MOD_ID = "naturescompass";
    
    /**
     * Explorer's Compass mod ID for integration
     */
    private static final String EXPLORERS_COMPASS_MOD_ID = "explorerscompass";
    
    /**
     * Checks if the Curios mod is loaded
     * @return true if Curios is loaded
     */
    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }
    
    /**
     * Checks if the Nature's Compass mod is loaded
     * @return true if Nature's Compass is loaded
     */
    public static boolean isNaturesCompassLoaded() {
        boolean loaded = ModList.get().isLoaded(NATURES_COMPASS_MOD_ID);
        if (loaded) {
            // LOGGER.debug("Nature's Compass mod is loaded");
        } else {
            // LOGGER.debug("Nature's Compass mod is not loaded");
        }
        return loaded;
    }
    
    /**
     * Checks if the Explorer's Compass mod is loaded
     * @return true if Explorer's Compass is loaded
     */
    public static boolean isExplorersCompassLoaded() {
        boolean loaded = ModList.get().isLoaded(EXPLORERS_COMPASS_MOD_ID);
        if (loaded) {
            // LOGGER.debug("Explorer's Compass mod is loaded");
        } else {
            // LOGGER.debug("Explorer's Compass mod is not loaded");
        }
        return loaded;
    }
    
    /**
     * Converts ticks to seconds (approximately).
     * @param ticks Number of ticks
     * @return Seconds (approximately)
     */
    public static float ticksToSeconds(int ticks) {
        return ticks / 20f;
    }
    
    /**
     * Converts seconds to ticks.
     * @param seconds Seconds
     * @return Equivalent number of ticks
     */
    public static int secondsToTicks(float seconds) {
        return (int) (seconds * 20);
    }

    /**
     * Compact duration for tooltips: contracts ticks into h/m/s (+ remaining t when needed).
     * Examples: {@code 60t}, {@code 3s}, {@code 1m 30s}, {@code 2h 5m}.
     */
    public static String formatTicksCompact(int ticks) {
        if (ticks <= 0) {
            return "0t";
        }
        int hours = ticks / 72000;
        ticks %= 72000;
        int minutes = ticks / 1200;
        ticks %= 1200;
        int seconds = ticks / 20;
        int remTicks = ticks % 20;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append('h');
        }
        if (minutes > 0) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(minutes).append('m');
        }
        if (seconds > 0) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(seconds).append('s');
        }
        if (remTicks > 0 || sb.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(remTicks).append('t');
        }
        return sb.toString();
    }

    /**
     * True if this stack reference sits in the player's vanilla inventory (hotbar/storage/offhand lists).
     * Curios-only stacks are not part of this list.
     */
    public static boolean isStackInVanillaPlayerInventory(Player player, ItemStack stack) {
        if (player == null || stack == null) return false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) == stack) {
                return true;
            }
        }
        return false;
    }
} 