package net.unfamily.iskautils.integration.anotherdynamics.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.IskaUtils;

/** Settings copier slot frame (visual copied from Another Dynamics). */
public final class DeepDrawerSettingsCopierClient {
    public static final ResourceLocation SLOT_FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/single_slot_copy.png");

    private DeepDrawerSettingsCopierClient() {}

    public static void blitSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.blit(SLOT_FRAME_TEXTURE, x, y, 0, 0, 18, 18, 18, 18);
    }
}
