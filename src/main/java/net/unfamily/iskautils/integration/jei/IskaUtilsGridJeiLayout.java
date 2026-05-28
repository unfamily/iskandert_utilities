package net.unfamily.iskautils.integration.jei;

/**
 * Shared JEI slot grid metrics (Factory 9×3 grid and single-input layouts).
 */
public final class IskaUtilsGridJeiLayout {

    public static final int SLOT_SIZE = 18;
    public static final int BG_PADDING_V = 5;
    public static final int INPUT_X = 4;
    public static final int GRID_X = 28;
    public static final int GRID_Y = BG_PADDING_V;
    public static final int INPUT_Y = GRID_Y + SLOT_SIZE;
    public static final int GRID_COLS = 9;
    public static final int GRID_ROWS = 3;
    public static final int ITEM_OFFSET = 1;

    private IskaUtilsGridJeiLayout() {}
}
