package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.IskaUtils;

/**
 * Shared Another Dynamics–style scroller: black track fill + {@code scroller.png} thumb
 * + real ▲/▼ {@link Button} widgets (not drawn text).
 * <p>
 * Geometry matches Dynaimics: up at list top, down flush with list bottom,
 * track between with {@link #SCROLL_ARROW_TRACK_GAP} gaps.
 */
public final class GuiScroller {
    public static final int SCROLLER_WIDTH = 12;
    public static final int SCROLLER_HEIGHT = 15;
    public static final int SCROLL_ARROW_SIZE = 12;
    public static final int SCROLL_ARROW_TRACK_GAP = 4;
    public static final int SCROLL_TRACK_COLOR = 0xFF000000;

    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scroller.png");

    private static final Component ARROW_UP = Component.literal("\u25b2");
    private static final Component ARROW_DOWN = Component.literal("\u25bc");

    private GuiScroller() {}

    /** Exclusive bottom Y of a list: {@code listTopY + visibleRows * rowHeight}. */
    public static int listBottomY(int listTopY, int visibleRows, int rowHeight) {
        return listTopY + visibleRows * rowHeight;
    }

    /**
     * Down-arrow Y flush with list bottom (Dynaimics:
     * {@code FILTER_LIST_BOTTOM - SCROLL_ARROW}).
     */
    public static int buttonDownY(int listTopY, int visibleRows, int rowHeight) {
        return listBottomY(listTopY, visibleRows, rowHeight) - SCROLL_ARROW_SIZE;
    }

    /** Down-arrow Y from an exclusive list-bottom coordinate. */
    public static int buttonDownYFromBottom(int listBottomYExclusive) {
        return listBottomYExclusive - SCROLL_ARROW_SIZE;
    }

    public static int trackY(int buttonUpY) {
        return buttonUpY + SCROLL_ARROW_SIZE + SCROLL_ARROW_TRACK_GAP;
    }

    public static int trackHeight(int buttonUpY, int buttonDownY) {
        return Math.max(SCROLLER_HEIGHT, buttonDownY - SCROLL_ARROW_TRACK_GAP - trackY(buttonUpY));
    }

    public static int handleY(int absTrackY, int trackH, int scroll, int maxScroll) {
        int range = Math.max(0, trackH - SCROLLER_HEIGHT);
        if (maxScroll <= 0 || range <= 0) {
            return absTrackY;
        }
        return absTrackY + (int) ((double) scroll / (double) maxScroll * (double) range);
    }

    public static int handleRange(int trackH) {
        return Math.max(0, trackH - SCROLLER_HEIGHT);
    }

    /**
     * Scroll ratio [0,1] for a track click (not handle drag): places the thumb so its
     * center aligns with the click across the full track height.
     */
    public static double scrollRatioFromTrackClick(double mouseY, int trackY, int trackH) {
        double clickTrack = (mouseY - trackY) - (SCROLLER_HEIGHT / 2.0);
        double denom = Math.max(1.0, (double) handleRange(trackH));
        return Math.max(0.0, Math.min(1.0, clickTrack / denom));
    }

    /** Integer scroll offset from a track click; clamps to {@code [0, maxScroll]}. */
    public static int scrollOffsetFromTrackClick(double mouseY, int trackY, int trackH, int maxScroll) {
        if (maxScroll <= 0) {
            return 0;
        }
        return (int) Math.round(scrollRatioFromTrackClick(mouseY, trackY, trackH) * maxScroll);
    }

    /** Track fill + thumb only. Arrows are separate {@link Button} widgets. */
    public static void drawTrackAndHandle(
            GuiGraphicsExtractor graphics, int x, int trackY, int trackH, int scroll, int maxScroll) {
        graphics.fill(x, trackY, x + SCROLLER_WIDTH, trackY + trackH, SCROLL_TRACK_COLOR);
        int hy = handleY(trackY, trackH, scroll, Math.max(0, maxScroll));
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                hy,
                0.0F,
                0.0F,
                SCROLLER_WIDTH,
                SCROLLER_HEIGHT,
                SCROLLER_WIDTH,
                SCROLLER_HEIGHT);
    }

    /**
     * Track + thumb from up/down arrow Ys (absolute screen pixels for {@code x}/{@code *Y}).
     * Does not draw arrows — use {@link #createUpButton}/{@link #createDownButton}.
     */
    public static void draw(
            GuiGraphicsExtractor graphics, int x, int buttonUpY, int buttonDownY, int scroll, int maxScroll) {
        int ty = trackY(buttonUpY);
        int th = trackHeight(buttonUpY, buttonDownY);
        drawTrackAndHandle(graphics, x, ty, th, scroll, maxScroll);
    }

    /**
     * Compatibility overload: font ignored (arrows are Buttons now).
     */
    public static void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int buttonUpY,
            int buttonDownY,
            int scroll,
            int maxScroll) {
        draw(graphics, x, buttonUpY, buttonDownY, scroll, maxScroll);
    }

    public static Button createUpButton(int screenX, int screenY, Runnable onPress) {
        return Button.builder(ARROW_UP, b -> onPress.run())
                .bounds(screenX, screenY, SCROLL_ARROW_SIZE, SCROLL_ARROW_SIZE)
                .build();
    }

    public static Button createDownButton(int screenX, int screenY, Runnable onPress) {
        return Button.builder(ARROW_DOWN, b -> onPress.run())
                .bounds(screenX, screenY, SCROLL_ARROW_SIZE, SCROLL_ARROW_SIZE)
                .build();
    }

    public static void setArrowActive(Button up, Button down, boolean canScroll) {
        if (up != null) {
            up.active = canScroll;
            up.visible = true;
        }
        if (down != null) {
            down.active = canScroll;
            down.visible = true;
        }
    }

    public static boolean hitHandle(
            double mouseX, double mouseY, int x, int trackY, int trackH, int scroll, int maxScroll) {
        int hy = handleY(trackY, trackH, scroll, Math.max(0, maxScroll));
        return mouseX >= x
                && mouseX < x + SCROLLER_WIDTH
                && mouseY >= hy
                && mouseY < hy + SCROLLER_HEIGHT;
    }

    public static boolean hitTrack(double mouseX, double mouseY, int x, int trackY, int trackH) {
        return mouseX >= x
                && mouseX < x + SCROLLER_WIDTH
                && mouseY >= trackY
                && mouseY < trackY + trackH;
    }
}
