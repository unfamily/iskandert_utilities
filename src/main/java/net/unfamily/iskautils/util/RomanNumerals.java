package net.unfamily.iskautils.util;

public final class RomanNumerals {
    private static final String[] VALUES = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private RomanNumerals() {}

    public static String toRoman(int value) {
        if (value <= 0) {
            return "I";
        }
        if (value < VALUES.length) {
            return VALUES[value];
        }
        return String.valueOf(value);
    }
}
