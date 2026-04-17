package net.unfamily.iskautils.client.gui;

/**
 * Opaque ARGB colors for {@link net.minecraft.client.gui.GuiGraphics#text} in Minecraft 26+.
 * 24-bit values like {@code 0x404040} are interpreted as ARGB with alpha 0 and render invisible.
 */
public final class GuiTextColors {
    public static final int TITLE = 0xFF404040;
    public static final int BODY = 0xFF404040;
    public static final int SECONDARY = 0xFF666666;
    public static final int MUTED = 0xFF808080;
    /** Dim red/brown for negative balance etc. */
    public static final int NEGATIVE = 0xFF804040;
    public static final int ERROR = 0xFFFF0000;
    public static final int STRUCTURE_MISSING = 0xFFFF4040;
    public static final int STRUCTURE_OK = 0xFF4040FF;
    public static final int LINK = 0xFF0066CC;
    public static final int LINK_HOVER = 0xFF0066FF;
    public static final int FEEDBACK_DEFAULT = 0xFFFFFFFF;

    private GuiTextColors() {}
}
