package net.unfamily.iskautils.util;

import java.util.List;

/**
 * Per-line filter concatenation group (AND within group, OR across groups / standalone lines).
 * {@link #NONE} = legacy single-line OR; {@link #A}..{@link #Z} group lines with AND.
 */
public enum DeepDrawerFilterConcatChannel {
    NONE,
    A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z;

    private static final DeepDrawerFilterConcatChannel[] VALUES = values();
    public static final int MAX_LETTER = 26;

    public static DeepDrawerFilterConcatChannel fromOrdinal(int o) {
        if (o < 0 || o >= VALUES.length) {
            return NONE;
        }
        return VALUES[o];
    }

    public boolean isConcatGroup() {
        return this != NONE;
    }

    public DeepDrawerFilterConcatChannel next() {
        return switch (this) {
            case NONE -> A;
            case Z -> NONE;
            default -> VALUES[ordinal() + 1];
        };
    }

    public DeepDrawerFilterConcatChannel previous() {
        return switch (this) {
            case NONE -> Z;
            case A -> NONE;
            default -> VALUES[ordinal() - 1];
        };
    }

    public static void syncToLineSize(List<Integer> concat, int lineCount) {
        while (concat.size() < lineCount) {
            concat.add(0);
        }
        while (concat.size() > lineCount) {
            concat.remove(concat.size() - 1);
        }
    }

    public static int channelAt(List<Integer> concat, int index) {
        if (concat == null || index < 0 || index >= concat.size()) {
            return 0;
        }
        int v = concat.get(index) != null ? concat.get(index) : 0;
        return Math.clamp(v, 0, MAX_LETTER);
    }
}
