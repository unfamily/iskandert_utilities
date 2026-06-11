package net.unfamily.iskautils.integration.anotherdynamics.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.IskaUtils;

/** Settings copier slot frame (visual copied from Another Dynamics). */
public final class DeepDrawerSettingsCopierClient {
    public static final Identifier SLOT_FRAME_TEXTURE = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/single_slot_copy.png");

    private DeepDrawerSettingsCopierClient() {}

    public static void blitSlotFrame(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_TEXTURE, x, y, 0.0F, 0.0F, 18, 18, 18, 18);
    }
}
