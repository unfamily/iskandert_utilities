package net.unfamily.iskautils.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.unfamily.iskautils.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds multi-segment styled display names / lore lines for the Labeling Machine.
 */
public final class LabelingNameStyle {

    public static final int MAX_SEGMENTS = 16;
    public static final int MAX_SEGMENT_LENGTH = 32;
    public static final int DEFAULT_MAX_LINE_LENGTH = 60;
    public static final int DEFAULT_MAX_LORE_LINES = 8;
    public static final int HARD_MAX_LORE_LINES = 32;
    /** @deprecated use {@link #maxLineLength()} */
    public static final int MAX_TOTAL_LENGTH = DEFAULT_MAX_LINE_LENGTH;
    /** @deprecated use {@link #maxLineLength()} / {@link #MAX_SEGMENT_LENGTH} */
    public static final int MAX_NAME_LENGTH = DEFAULT_MAX_LINE_LENGTH;
    public static final int DEFAULT_COLOR_RGB = 0xFFFFFF;
    public static final int DEFAULT_LORE_COLOR_RGB = 0xFF55FF;

    public static final ChatFormatting[] VANILLA_PALETTE = {
            ChatFormatting.BLACK,
            ChatFormatting.DARK_BLUE,
            ChatFormatting.DARK_GREEN,
            ChatFormatting.DARK_AQUA,
            ChatFormatting.DARK_RED,
            ChatFormatting.DARK_PURPLE,
            ChatFormatting.GOLD,
            ChatFormatting.GRAY,
            ChatFormatting.DARK_GRAY,
            ChatFormatting.BLUE,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.RED,
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.YELLOW,
            ChatFormatting.WHITE
    };

    private LabelingNameStyle() {}

    /** Max characters for a name or a single lore line (from config, fallback 60). */
    public static int maxLineLength() {
        int v = Config.labelingMaxLineLength;
        return v > 0 ? Math.min(256, v) : DEFAULT_MAX_LINE_LENGTH;
    }

    /** Max lore lines editable (from config, fallback 8). */
    public static int maxLoreLines() {
        int v = Config.labelingMaxLoreLines;
        return v > 0 ? Math.min(HARD_MAX_LORE_LINES, v) : DEFAULT_MAX_LORE_LINES;
    }

    /**
     * When config is enabled, non-operators cannot remove italic from custom names
     * (below COMMANDS_GAMEMASTER permission).
     */
    public static boolean shouldForceItalicFor(net.minecraft.world.entity.player.Player player) {
        return Config.labelingForceItalicNonOps
                && player != null
                && !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    /** Force italic on every name segment (used for non-ops when config is on). */
    public static void forceItalicOnSegments(List<Segment> segments) {
        if (segments == null) {
            return;
        }
        for (Segment s : segments) {
            if (s != null) {
                s.italic = true;
            }
        }
    }


    /** Mutable name piece: text + formatting + RGB color. */
    public static final class Segment {
        public String text = "";
        public boolean bold;
        public boolean italic;
        public boolean underline;
        public boolean strikethrough;
        public boolean obfuscated;
        public int colorRgb = DEFAULT_COLOR_RGB;

        public Segment() {}

        public Segment(String text, boolean bold, boolean italic, boolean underline,
                       boolean strikethrough, boolean obfuscated, int colorRgb) {
            this.text = text == null ? "" : text;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikethrough = strikethrough;
            this.obfuscated = obfuscated;
            this.colorRgb = colorRgb & 0xFFFFFF;
        }

        public static Segment blank() {
            // Vanilla anvil-style custom names start italic.
            Segment s = new Segment();
            s.italic = true;
            return s;
        }

        public static Segment blankLore() {
            Segment s = new Segment();
            s.colorRgb = DEFAULT_LORE_COLOR_RGB;
            return s;
        }

        public Segment copy() {
            return new Segment(text, bold, italic, underline, strikethrough, obfuscated, colorRgb);
        }

        public Style toStyle() {
            return Style.EMPTY
                    .withBold(bold)
                    .withItalic(italic)
                    .withUnderlined(underline)
                    .withStrikethrough(strikethrough)
                    .withObfuscated(obfuscated)
                    .withColor(TextColor.fromRgb(colorRgb & 0xFFFFFF));
        }

        public static Segment fromStyle(String text, Style style) {
            Segment s = new Segment();
            s.text = text == null ? "" : text;
            s.bold = Boolean.TRUE.equals(style.isBold());
            s.italic = Boolean.TRUE.equals(style.isItalic());
            s.underline = Boolean.TRUE.equals(style.isUnderlined());
            s.strikethrough = Boolean.TRUE.equals(style.isStrikethrough());
            s.obfuscated = Boolean.TRUE.equals(style.isObfuscated());
            TextColor tc = style.getColor();
            s.colorRgb = tc != null ? tc.getValue() & 0xFFFFFF : DEFAULT_COLOR_RGB;
            return s;
        }
    }

    /** Hue 0..360, saturation/value 0..1. */
    public static final class Hsv {
        public float h;
        public float s;
        public float v;

        public Hsv(float h, float s, float v) {
            this.h = h;
            this.s = s;
            this.v = v;
        }
    }

    public static Component buildName(List<Segment> segments) {
        List<Segment> cleaned = sanitizeForApply(segments);
        if (cleaned.isEmpty()) {
            return Component.empty();
        }
        MutableComponent root = null;
        for (Segment seg : cleaned) {
            String text = clampSegmentText(seg.text);
            if (text.isEmpty()) {
                continue;
            }
            MutableComponent piece = Component.literal(text).withStyle(seg.toStyle());
            if (root == null) {
                root = piece;
            } else {
                root.append(piece);
            }
        }
        return root == null ? Component.empty() : root;
    }

    public static MutableComponent preview(List<Segment> segments) {
        Component built = buildName(segments);
        if (built.getString().isEmpty()) {
            return Component.empty();
        }
        return built.copy();
    }

    /** Plain concatenated text of segments (for lore line button preview). */
    public static String plainText(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Segment s : segments) {
            if (s != null && s.text != null && !s.text.isEmpty()) {
                sb.append(s.text);
            }
        }
        return sb.toString();
    }

    /** Legacy single-segment helpers kept for compatibility. */
    public static Component buildName(String text, boolean bold, boolean italic, boolean underline,
                                      boolean strikethrough, boolean obfuscated, int colorRgb) {
        return buildName(List.of(new Segment(text, bold, italic, underline, strikethrough, obfuscated, colorRgb)));
    }

    public static MutableComponent preview(String text, boolean bold, boolean italic, boolean underline,
                                           boolean strikethrough, boolean obfuscated, int colorRgb) {
        return preview(List.of(new Segment(text, bold, italic, underline, strikethrough, obfuscated, colorRgb)));
    }

    public static List<Segment> sanitizeForApply(List<Segment> segments) {
        List<Segment> copy = new ArrayList<>();
        if (segments != null) {
            for (Segment s : segments) {
                if (s != null) {
                    copy.add(s.copy());
                }
            }
        }
        while (!copy.isEmpty() && clampSegmentText(copy.get(copy.size() - 1).text).isEmpty()) {
            copy.remove(copy.size() - 1);
        }
        if (copy.size() > MAX_SEGMENTS) {
            copy = new ArrayList<>(copy.subList(0, MAX_SEGMENTS));
        }
        int budget = maxLineLength();
        int total = 0;
        List<Segment> limited = new ArrayList<>();
        for (Segment s : copy) {
            String text = clampSegmentText(s.text);
            int remaining = budget - total;
            if (remaining <= 0) {
                break;
            }
            if (text.length() > remaining) {
                text = text.substring(0, remaining);
            }
            Segment clipped = s.copy();
            clipped.text = text;
            limited.add(clipped);
            total += text.length();
        }
        return limited;
    }

    /** Sanitize lore lines: clamp count, drop only trailing empty lines, keep middle blanks. */
    public static List<List<Segment>> sanitizeLoreForApply(List<List<Segment>> lines) {
        List<List<Segment>> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        int max = maxLoreLines();
        int count = Math.min(lines.size(), max);
        for (int i = 0; i < count; i++) {
            out.add(sanitizeForApply(lines.get(i)));
        }
        while (!out.isEmpty() && plainText(out.get(out.size() - 1)).isEmpty()) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    public static List<Component> buildLoreComponents(List<List<Segment>> lines) {
        List<Component> components = new ArrayList<>();
        for (List<Segment> line : sanitizeLoreForApply(lines)) {
            Component built = buildName(line);
            // Keep blank lines (e.g. line 1 + empty line 2 + line 3).
            components.add(built.getString().isEmpty() ? Component.literal("") : built);
        }
        return components;
    }

    public static String clampSegmentText(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() > MAX_SEGMENT_LENGTH) {
            return text.substring(0, MAX_SEGMENT_LENGTH);
        }
        return text;
    }

    public static List<Segment> fromComponent(Component component) {
        return fromComponent(component, false);
    }

    public static List<Segment> fromComponent(Component component, boolean loreDefaultColor) {
        List<Segment> out = new ArrayList<>();
        if (component != null) {
            collectSegments(component, Style.EMPTY, out);
        }
        if (out.isEmpty()) {
            out.add(loreDefaultColor ? Segment.blankLore() : Segment.blank());
        }
        if (out.size() > MAX_SEGMENTS) {
            return new ArrayList<>(out.subList(0, MAX_SEGMENTS));
        }
        return out;
    }

    private static void collectSegments(Component component, Style parent, List<Segment> out) {
        Style style = component.getStyle().applyTo(parent);
        if (component.getContents() instanceof PlainTextContents plain) {
            String text = plain.text();
            if (!text.isEmpty()) {
                out.add(Segment.fromStyle(clampSegmentText(text), style));
            }
        }
        for (Component sibling : component.getSiblings()) {
            collectSegments(sibling, style, out);
        }
    }

    /** Parses {@code #RRGGBB} or {@code RRGGBB}; returns null if invalid. */
    public static Integer parseHexColor(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toHexString(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    public static Hsv rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta == 0f) {
            h = 0f;
        } else if (max == r) {
            h = 60f * (((g - b) / delta) % 6f);
        } else if (max == g) {
            h = 60f * (((b - r) / delta) + 2f);
        } else {
            h = 60f * (((r - g) / delta) + 4f);
        }
        if (h < 0f) {
            h += 360f;
        }
        float s = max == 0f ? 0f : delta / max;
        return new Hsv(h, s, max);
    }

    public static int hsvToRgb(float h, float s, float v) {
        h = ((h % 360f) + 360f) % 360f;
        s = clamp01(s);
        v = clamp01(v);
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float r1;
        float g1;
        float b1;
        if (h < 60f) {
            r1 = c; g1 = x; b1 = 0f;
        } else if (h < 120f) {
            r1 = x; g1 = c; b1 = 0f;
        } else if (h < 180f) {
            r1 = 0f; g1 = c; b1 = x;
        } else if (h < 240f) {
            r1 = 0f; g1 = x; b1 = c;
        } else if (h < 300f) {
            r1 = x; g1 = 0f; b1 = c;
        } else {
            r1 = c; g1 = 0f; b1 = x;
        }
        int r = Math.round((r1 + m) * 255f);
        int g = Math.round((g1 + m) * 255f);
        int b = Math.round((b1 + m) * 255f);
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
