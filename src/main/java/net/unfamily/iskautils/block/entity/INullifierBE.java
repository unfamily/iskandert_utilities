package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Common interface for all three nullifier block entities,
 * enabling a single NullifierMenu / NullifierScreen to handle Ender, Wander and Soul.
 */
public interface INullifierBE {

    enum NullifierType {
        ENDER(0), WANDER(1), SOUL(2);
        private final int id;
        NullifierType(int id) { this.id = id; }
        public int getId() { return id; }
        public static NullifierType fromId(int id) {
            return switch (id) { case 1 -> WANDER; case 2 -> SOUL; default -> ENDER; };
        }
    }

    // --- range ---
    int getRange();
    int getMaxRange();
    void setRange(int r);

    // --- GUI redstone mode (0=Manual, 1=Disabled, 2=Low, 3=High) ---
    int getRedstoneModeGui();
    void setRedstoneModeGui(int guiMode);

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
