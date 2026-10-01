package net.unfamily.iskautils.block.entity;

/**
 * Who an active nullifier applies its effect to.
 * {@link #DISABLED} is legacy; {@link #fromId(int)} and sanitize map it to {@link #ONLY_MOBS}.
 */
public enum NullifierTargetMode {
    DISABLED(0),
    ONLY_MOBS(1),
    ONLY_PLAYERS(2),
    MOBS_AND_PLAYERS(3);

    private final int id;

    NullifierTargetMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static NullifierTargetMode fromId(int id) {
        return switch (id) {
            case 2 -> ONLY_PLAYERS;
            case 3 -> MOBS_AND_PLAYERS;
            default -> ONLY_MOBS; // includes legacy DISABLED(0) and ONLY_MOBS(1)
        };
    }

    public boolean affectsMobs() {
        return this == ONLY_MOBS || this == MOBS_AND_PLAYERS;
    }

    public boolean affectsPlayers() {
        return this == ONLY_PLAYERS || this == MOBS_AND_PLAYERS;
    }

    /** Flight nullifier: block player flight when the mode affects players. */
    public boolean blocksPlayerFlightInZone() {
        return affectsPlayers();
    }
}
