package net.unfamily.iskautils.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class to manage session variables
 * Variables stored here are cleared when the server/world stops
 */
public class SessionVariables {
    private static final Map<String, Object> sessionVariables = new HashMap<>();
    
    // Session ID for scanner markers
    private static final String SCANNER_SESSION_ID = "scanner_session_id";
    
    /**
     * Gets the current scanner session ID, generating a new one if it doesn't exist
     * @return The current scanner session ID
     */
    public static UUID getScannerSessionId() {
        if (!sessionVariables.containsKey(SCANNER_SESSION_ID)) {
            sessionVariables.put(SCANNER_SESSION_ID, UUID.randomUUID());
        }
        return (UUID) sessionVariables.get(SCANNER_SESSION_ID);
    }
    
    /**
     * Resets the scanner session ID
     */
    public static void resetScannerSessionId() {
        sessionVariables.put(SCANNER_SESSION_ID, UUID.randomUUID());
    }
    
    /**
     * Clears all session variables
     * Should be called when the server stops
     */
    public static void clearAll() {
        sessionVariables.clear();
    }
}