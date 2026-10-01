package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Common interface for all nullifier block entities,
 * enabling a single NullifierMenu / NullifierScreen to handle every nullifier type.
 */
public interface INullifierBE {

    enum NullifierType {
        ENDER(0), WANDER(1), SOUL(2), FLIGHT(3), CLIMBING(4);
        private final int id;
        NullifierType(int id) { this.id = id; }
        public int getId() { return id; }
        public static NullifierType fromId(int id) {
            return switch (id) {
                case 1 -> WANDER;
                case 2 -> SOUL;
                case 3 -> FLIGHT;
                case 4 -> CLIMBING;
                default -> ENDER;
            };
        }

        /**
         * Soul / Wander: target button locked to Only mobs.
         * Ender / Flight / Climbing cycle Only mobs → Both → Only players (no Disabled).
         */
        public boolean locksTargetToMobsOnly() {
            return this == SOUL || this == WANDER;
        }

        /** @deprecated use {@link #locksTargetToMobsOnly()} */
        public boolean hasLimitedTargetModes() {
            return locksTargetToMobsOnly();
        }

        /** Reject Disabled; soul/wander always Only mobs. */
        public NullifierTargetMode sanitizeTargetMode(NullifierTargetMode mode) {
            if (locksTargetToMobsOnly()) {
                return NullifierTargetMode.ONLY_MOBS;
            }
            if (mode == null || mode == NullifierTargetMode.DISABLED) {
                return NullifierTargetMode.ONLY_MOBS;
            }
            return mode;
        }
    }

    // --- range ---
    int getRange();
    int getMaxRange();
    void setRange(int r);

    // --- GUI redstone mode (0=Manual, 1=Disabled, 2=Low, 3=High) ---
    int getRedstoneModeGui();
    void setRedstoneModeGui(int guiMode);

    // --- mob/player target ---
    NullifierTargetMode getTargetMode();
    void setTargetMode(NullifierTargetMode mode);

    // --- area preview ---
    boolean isShowAreaEnabled();
    void setShowAreaEnabled(boolean v);

    // --- modules ---
    ItemStackHandler getModuleHandler();

    // --- position ---
    BlockPos getBlockPos();
    Level getLevel();

    // --- type identifier ---
    NullifierType getNullifierType();
}
