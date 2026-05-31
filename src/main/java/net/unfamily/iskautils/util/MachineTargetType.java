package net.unfamily.iskautils.util;

public enum MachineTargetType {
    MOBS_ONLY(0, "mobs_only"),
    MOBS_AND_PLAYERS(1, "mobs_and_players"),
    PLAYERS_ONLY(2, "players_only");

    private final int id;
    private final String name;

    MachineTargetType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static MachineTargetType fromId(int id) {
        for (MachineTargetType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return MOBS_ONLY;
    }

    public static MachineTargetType fromName(String name) {
        for (MachineTargetType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return MOBS_ONLY;
    }

    public MachineTargetType cycle(boolean backward) {
        MachineTargetType[] all = values();
        int index = ordinal();
        if (backward) {
            index = (index - 1 + all.length) % all.length;
        } else {
            index = (index + 1) % all.length;
        }
        return all[index];
    }
}
