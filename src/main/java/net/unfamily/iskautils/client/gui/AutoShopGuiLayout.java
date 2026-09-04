package net.unfamily.iskautils.client.gui;

public final class AutoShopGuiLayout {
    public static final int GUI_WIDTH = 200;
    public static final int GUI_HEIGHT = 170;
    public static final int ENERGY_BAR_X = 132;
    public static final int GAS_BAR_X = 150;
    public static final int LIQUID_BAR_X = 168;
    public static final int BAR_Y = 11;
    public static final int BAR_W = 12;
    public static final int BAR_H = 54;
    public static final int DUMP_W = 14;
    public static final int DUMP_H = 12;
    public static final int DUMP_GAP_BELOW = 3;
    public static final int DUMP_Y = BAR_Y + BAR_H + DUMP_GAP_BELOW;
    public static final int MASK_COLOR = 0xFFC6C6C6;
    public static final int GAS_MASK_INSET = 1;

    private AutoShopGuiLayout() {}

    /** Relative X of disabled-gas cover (inclusive left). */
    public static int gasMaskLeft() {
        return GAS_BAR_X - GAS_MASK_INSET;
    }

    /** Relative Y of disabled-gas cover (inclusive top). */
    public static int gasMaskTop() {
        return BAR_Y - GAS_MASK_INSET;
    }

    /** Relative X of disabled-gas cover (exclusive right; stops before liquid). */
    public static int gasMaskRight() {
        return LIQUID_BAR_X - 1;
    }

    /** Relative Y of disabled-gas cover (exclusive bottom; includes dump button). */
    public static int gasMaskBottom() {
        return DUMP_Y + DUMP_H;
    }
}
