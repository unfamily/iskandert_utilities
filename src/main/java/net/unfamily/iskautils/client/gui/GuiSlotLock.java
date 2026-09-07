package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.unfamily.iskautils.IskaUtils;

/**
 * Padlock overlay for module/upgrade slots disabled by config ({@code max == 0}).
 * Same texture as locked Pattern Crafter variables ({@code slot_lock.png}).
 */
public final class GuiSlotLock {

    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/slot_lock.png");

    /** Slight darkening under the lock icon. */
    public static final int DIM_OVERLAY = 0x40000000;

    private GuiSlotLock() {}

    /** True when the slot rejects inserts because its handler limit is 0. */
    public static boolean isLocked(Slot slot) {
        return slot != null && slot.getMaxStackSize() <= 0;
    }

    public static Component lockedTooltip() {
        return Component.translatable("gui.iska_utils.module_slot_locked");
    }

    public static void renderIfLocked(GuiGraphicsExtractor graphics, int leftPos, int topPos, Slot slot) {
        if (!isLocked(slot)) {
            return;
        }
        renderAt(graphics, leftPos + slot.x, topPos + slot.y);
    }

    public static void renderAt(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 16, y + 16, DIM_OVERLAY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
    }
}
