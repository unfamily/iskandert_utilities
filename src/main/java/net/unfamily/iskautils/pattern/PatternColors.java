package net.unfamily.iskautils.pattern;

/**
 * Color mapping for pattern letters A-R.
 * Each letter has a unique color for visual distinction in the pattern grid.
 */
public class PatternColors {

    // Colors for letters A-R (index 1-18, index 0 is unused/empty)
    private static final int[] COLORS = {
            0xFF404040, //  0 - Empty (dark gray)
            0xFFFF4040, //  1 - A: Red
            0xFF40FF40, //  2 - B: Green
            0xFF4080FF, //  3 - C: Blue
            0xFFFFFF40, //  4 - D: Yellow
            0xFFFF40FF, //  5 - E: Magenta
            0xFF40FFFF, //  6 - F: Cyan
            0xFFFF8020, //  7 - G: Orange
            0xFFFF80C0, //  8 - H: Pink
            0xFF80FF20, //  9 - I: Lime
            0xFFA040FF, // 10 - J: Purple
            0xFF804020, // 11 - K: Brown
            0xFF80C0FF, // 12 - L: Light Blue
            0xFFFFFFFF, // 13 - M: White
            0xFF808080, // 14 - N: Gray
            0xFF208040, // 15 - O: Dark Green
            0xFFC02020, // 16 - P: Dark Red
            0xFF2020A0, // 17 - Q: Dark Blue
            0xFFFFC040, // 18 - R: Gold
    };

    /**
     * Returns the color for a pattern cell value.
     * @param value 0 for empty, 1-18 for A-R; 19+ cycle through same palette
     * @return ARGB color
     */
    public static int getColor(int value) {
        if (value <= 0) return COLORS[0];
        if (value < COLORS.length) return COLORS[value];
        return COLORS[1 + (value - 1) % (COLORS.length - 1)]; // 19+ reuse A-R colors
    }

    /**
     * Returns the text color for rendering the letter on top of the background.
     * Uses white for dark backgrounds, black for light backgrounds.
     */
    public static int getTextColor(int value) {
        if (value == 0) return 0xFFA0A0A0; // Gray text for empty
        int color = getColor(value);
        // Simple brightness check: if R+G+B > 384 (midpoint), use dark text
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (r + g + b > 384) ? 0xFF202020 : 0xFFFFFFFF;
    }
}
