package net.unfamily.iskautils.events;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ClickDelayManager {
    private static final long CLICK_DELAY_MS = 500;
    
    private static final Map<UUID, Long> lastClickMap = new HashMap<>();
    
    /**
     * Verify if the player can click
     * @param playerUUID UUID of the player
     * @return true if the player can click, false if the delay is not passed
     */
    public static boolean canClick(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        if (!lastClickMap.containsKey(playerUUID)) {
            return true;
        }
        
        long lastClick = lastClickMap.get(playerUUID);
        return (currentTime - lastClick) >= CLICK_DELAY_MS;
    }
    
    /**
     * Update the timestamp of the last click
     * @param playerUUID UUID of the player
     */
    public static void updateClickTime(UUID playerUUID) {
        lastClickMap.put(playerUUID, System.currentTimeMillis());
    }
} 