package net.unfamily.iskautils.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concatenated filter lines: same letter on one list must all match (AND);
 * groups and standalone lines combine with OR when evaluating {@code matchesAny}.
 */
public final class DeepDrawerFilterConcatEvaluator {
    @FunctionalInterface
    public interface LineMatcher {
        boolean matchesLine(int lineIndex, String trimmedLine);
    }

    private DeepDrawerFilterConcatEvaluator() {}

    public static boolean matchesAny(List<String> lines, List<Integer> concatChannels, LineMatcher matcher) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        Set<Integer> consumedConcat = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            int ch = DeepDrawerFilterConcatChannel.channelAt(concatChannels, i);
            if (ch == 0) {
                if (matcher.matchesLine(i, raw.trim())) {
                    return true;
                }
            } else if (!consumedConcat.contains(ch)) {
                if (matchesConcatGroup(lines, concatChannels, ch, matcher)) {
                    consumedConcat.add(ch);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesConcatGroup(
            List<String> lines, List<Integer> concatChannels, int channel, LineMatcher matcher) {
        boolean anyInGroup = false;
        for (int i = 0; i < lines.size(); i++) {
            if (DeepDrawerFilterConcatChannel.channelAt(concatChannels, i) != channel) {
                continue;
            }
            String raw = lines.get(i);
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            anyInGroup = true;
            if (!matcher.matchesLine(i, raw.trim())) {
                return false;
            }
        }
        return anyInGroup;
    }
}
