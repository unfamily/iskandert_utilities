package net.unfamily.iskautils.util;

public enum CollectingCrateMode {
    BOTH,
    EXPERIENCE_ONLY,
    ITEMS_ONLY;

    public CollectingCrateMode cycle(boolean backward) {
        CollectingCrateMode[] values = values();
        int next = ordinal() + (backward ? -1 : 1);
        if (next < 0) {
            next = values.length - 1;
        } else if (next >= values.length) {
            next = 0;
        }
        return values[next];
    }

    public static CollectingCrateMode fromId(int id) {
        CollectingCrateMode[] values = values();
        if (id < 0 || id >= values.length) {
            return BOTH;
        }
        return values[id];
    }

    public int getId() {
        return ordinal();
    }

    public boolean collectsExperience() {
        return this == BOTH || this == EXPERIENCE_ONLY;
    }

    public boolean collectsItems() {
        return this == BOTH || this == ITEMS_ONLY;
    }
}
