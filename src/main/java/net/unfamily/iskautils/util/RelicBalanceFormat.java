package net.unfamily.iskautils.util;

/**
 * Formats relic balance config values for tooltips (fractions in config, percents in UI).
 */
public final class RelicBalanceFormat {
    private RelicBalanceFormat() {}

    public static int asPercent(double fraction) {
        return (int) Math.round(fraction * 100.0);
    }

    public static String percent(double fraction) {
        return asPercent(fraction) + "%";
    }

    /** Bonus percent from a damage multiplier (e.g. 1.15 → "15%"). */
    public static String percentBonusFromMultiplier(double multiplier) {
        return percent(multiplier - 1.0);
    }

    /** Bonus percent from a break-speed multiplier (e.g. 1.5 → "50%"). */
    public static String speedBonusPercent(double multiplier) {
        return percent(Math.max(0.0, multiplier - 1.0));
    }

    /** Flat numeric bonus for tooltips (+damage, +armor, +HP). */
    public static String flatBonus(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (Math.abs(rounded - Math.round(rounded)) < 1.0E-6) {
            return String.valueOf((int) Math.round(rounded));
        }
        return String.format("%.1f", rounded);
    }
}
