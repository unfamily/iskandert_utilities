package net.unfamily.iskautils.util;

/**
 * Normalizes strings entered in GUI edit boxes for filters, ids, and resource selectors
 * (Another Dynamics parity).
 */
public final class FilterLineTextUtil {
    private FilterLineTextUtil() {}

    /**
     * Trim leading/trailing whitespace and strip wrapping quotes/apostrophes for EditBox commit.
     *
     * <p>Order: trim → strip wrapping {@code '}, {@code "}, {@code `} (repeated) → trim again.
     * Inner quotes (e.g. NBT substrings) are preserved.
     */
    public static String normalizeForCommit(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return stripWrappingQuotes(raw.trim()).trim();
    }

    /**
     * Removes repeated wrapping {@code '}, {@code "}, and {@code `} at the start and end only.
     * Inner quotes (e.g. NBT substrings) are preserved.
     */
    public static String stripWrappingQuotes(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw == null ? "" : raw;
        }
        int start = 0;
        int end = raw.length();
        while (start < end && isWrappingQuote(raw.charAt(start))) {
            start++;
        }
        while (end > start && isWrappingQuote(raw.charAt(end - 1))) {
            end--;
        }
        if (start == 0 && end == raw.length()) {
            return raw;
        }
        return raw.substring(start, end);
    }

    private static boolean isWrappingQuote(char c) {
        return c == '\'' || c == '"' || c == '`';
    }
}
