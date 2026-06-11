package net.unfamily.iskautils.client.gui;

/**
 * ARGB colors for filter concat channel letters A–Z (matches Another Dynamics {@code LetterPalette}).
 */
final class DeepDrawerFilterLetterPalette {
    private static final int[] BACKGROUNDS = {
            0xFF404040,
            0xFFFF4040,
            0xFF40FF40,
            0xFF4080FF,
            0xFFFFFF40,
            0xFFFF40FF,
            0xFF40FFFF,
            0xFFFF8020,
            0xFFFF80C0,
            0xFF80FF20,
            0xFFA040FF,
            0xFF804020,
            0xFF80C0FF,
            0xFFFFFFFF,
            0xFF808080,
            0xFF208040,
            0xFFC02020,
            0xFF2020A0,
            0xFFFFC040,
            0xFF40A080,
            0xFFC08040,
            0xFF6040C0,
            0xFFA0A040,
            0xFF406080,
            0xFFD0D0D0,
            0xFF904060,
            0xFF509050,
    };

    private DeepDrawerFilterLetterPalette() {}

    static int backgroundArgb(int letter1to26) {
        if (letter1to26 <= 0) {
            return BACKGROUNDS[0];
        }
        if (letter1to26 < BACKGROUNDS.length) {
            return BACKGROUNDS[letter1to26];
        }
        return BACKGROUNDS[1 + (letter1to26 - 1) % (BACKGROUNDS.length - 1)];
    }

    static int textArgb(int letter1to26) {
        int color = backgroundArgb(letter1to26);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (r + g + b > 384) ? 0xFF202020 : 0xFFFFFFFF;
    }
}
