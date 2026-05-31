package net.unfamily.iskautils.arcane;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-tick gate so damage hooks do not apply entropy consume twice. */
public final class ArcaneDictionarySession {
    private static final Map<UUID, Boolean> EFFECTS_ACTIVE = new ConcurrentHashMap<>();

    private ArcaneDictionarySession() {}

    public static void setEffectsActive(UUID playerId, boolean active) {
        if (active) {
            EFFECTS_ACTIVE.put(playerId, Boolean.TRUE);
        } else {
            EFFECTS_ACTIVE.remove(playerId);
        }
    }

    public static boolean isEffectsActive(UUID playerId) {
        return Boolean.TRUE.equals(EFFECTS_ACTIVE.get(playerId));
    }
}
