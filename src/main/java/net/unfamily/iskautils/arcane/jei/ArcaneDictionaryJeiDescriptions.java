package net.unfamily.iskautils.arcane.jei;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.unfamily.iskautils.arcane.ArcaneDictionaryDefinition;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectRegistry;
import net.unfamily.iskautils.integration.jei.ArcaneDictionaryJeiBackgroundDrawable;

import java.util.ArrayList;
import java.util.List;

public final class ArcaneDictionaryJeiDescriptions {
    public static final int LINE_HEIGHT = 10;
    public static final int TEXT_X = 4;
    public static final int TEXT_Y = 4;
    public static final int TEXT_PADDING_RIGHT = 4;
    public static final int MIN_HEIGHT = 48;
    public static final int WIDTH = 170;
    public static final int CATALYST_TEXT_GAP = 4;
    public static final int BOTTOM_PADDING = 4;

    private static final FormattedCharSequence EMPTY_LINE = Component.empty().getVisualOrderText();

    public static FormattedCharSequence emptyLine() {
        return EMPTY_LINE;
    }

    private ArcaneDictionaryJeiDescriptions() {}

    public static int textMaxWidth() {
        return WIDTH - TEXT_X - TEXT_PADDING_RIGHT;
    }

    public static List<Component> buildLines(Identifier traitId, ArcaneDictionaryDefinition.Entry entry) {
        ArcaneDictionaryJeiContext ctx = ArcaneDictionaryJeiContext.of(traitId, entry);
        List<Component> lines = new ArrayList<>();
        ArcaneDictionaryJeiLines.appendLevelHeader(ctx, lines);

        ArcaneDictionaryEffect effect = ArcaneDictionaryEffectRegistry.get(traitId);
        if (effect != null) {
            effect.appendJeiDescription(ctx, lines);
        } else {
            ArcaneDictionaryJeiDescriber describer = ArcaneDictionaryJeiRegistry.get(traitId);
            if (describer != null) {
                describer.appendJeiDescription(ctx, lines);
            } else {
                lines.add(Component.translatable("jei.iska_utils.arcane_trait.unknown_effect"));
            }
        }

        lines.add(Component.empty());
        ArcaneDictionaryJeiLines.appendPoolMeta(ctx, lines);
        return List.copyOf(lines);
    }

    public static List<FormattedCharSequence> wrapLines(Font font, List<Component> lines) {
        int maxWidth = textMaxWidth();
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            if (line.getString().isEmpty()) {
                wrapped.add(EMPTY_LINE);
                continue;
            }
            wrapped.addAll(font.split(line, maxWidth));
        }
        return List.copyOf(wrapped);
    }

    public static List<FormattedCharSequence> wrapLinesWithoutFont(List<Component> lines) {
        List<FormattedCharSequence> wrapped = new ArrayList<>(lines.size());
        for (Component line : lines) {
            if (line.getString().isEmpty()) {
                wrapped.add(EMPTY_LINE);
            } else {
                wrapped.add(line.getVisualOrderText());
            }
        }
        return List.copyOf(wrapped);
    }

    public static int measureTextHeight(List<FormattedCharSequence> lines) {
        int y = TEXT_Y;
        for (FormattedCharSequence line : lines) {
            if (line == EMPTY_LINE) {
                y += LINE_HEIGHT / 2;
            } else {
                y += LINE_HEIGHT;
            }
        }
        return y;
    }

    public static int computeCatalystRowY(List<FormattedCharSequence> wrappedLines, boolean hasCatalysts) {
        int y = measureTextHeight(wrappedLines);
        if (hasCatalysts) {
            return y + CATALYST_TEXT_GAP;
        }
        return y;
    }

    public static int computeHeight(List<FormattedCharSequence> wrappedLines, boolean hasCatalysts) {
        int y = computeCatalystRowY(wrappedLines, hasCatalysts);
        if (hasCatalysts) {
            y += ArcaneDictionaryJeiBackgroundDrawable.SLOT_SIZE;
        } else {
            y += LINE_HEIGHT;
        }
        return Math.max(MIN_HEIGHT, y + BOTTOM_PADDING);
    }

    public static int catalystLabelY(int catalystRowY) {
        return catalystRowY + (ArcaneDictionaryJeiBackgroundDrawable.SLOT_SIZE - LINE_HEIGHT) / 2;
    }
}
