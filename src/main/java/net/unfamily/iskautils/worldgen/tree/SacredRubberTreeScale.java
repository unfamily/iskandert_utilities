package net.unfamily.iskautils.worldgen.tree;

/**
 * Dimension profile for sacred rubber tree growth.
 */
public enum SacredRubberTreeScale {
    NORMAL(3, 30, 45, 45),
    MEGA(6, 60, 90, 90);

    public final int trunkRadius;
    public final int trunkHeight;
    public final int leavesRadius;
    public final int leavesHeight;

    SacredRubberTreeScale(int trunkRadius, int trunkHeight, int leavesRadius, int leavesHeight) {
        this.trunkRadius = trunkRadius;
        this.trunkHeight = trunkHeight;
        this.leavesRadius = leavesRadius;
        this.leavesHeight = leavesHeight;
    }
}
