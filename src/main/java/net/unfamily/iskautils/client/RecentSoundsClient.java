package net.unfamily.iskautils.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side ring buffer of recently heard sound event ids (newest first).
 * Used by the Sound Muffler filter screen "Recent" section.
 */
public final class RecentSoundsClient {

    private static final int MAX_RECENT = 32;
    private static final ArrayList<String> RECENT = new ArrayList<>();

    private RecentSoundsClient() {}

    /** Records a sound id; most recent stays first. Music callers should skip before calling. */
    public static void record(String soundId) {
        if (soundId == null || soundId.isEmpty()) {
            return;
        }
        synchronized (RECENT) {
            RECENT.remove(soundId);
            RECENT.add(0, soundId);
            while (RECENT.size() > MAX_RECENT) {
                RECENT.remove(RECENT.size() - 1);
            }
        }
    }

    /** Newest-first snapshot of recently heard sound event ids. */
    public static List<String> getRecent() {
        synchronized (RECENT) {
            return List.copyOf(RECENT);
        }
    }

    /** Mutable copy for UI filtering. */
    public static List<String> copyRecent() {
        synchronized (RECENT) {
            return new ArrayList<>(RECENT);
        }
    }
}
