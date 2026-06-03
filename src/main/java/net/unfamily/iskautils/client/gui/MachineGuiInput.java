package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;

/**
 * Shared input handling for machine GUIs: suppress inventory close while using scrollbars or EditBoxes.
 */
public final class MachineGuiInput {
    private static boolean scrollbarPointerDown;

    private MachineGuiInput() {
    }

    public static void markScrollbarPressed() {
        scrollbarPointerDown = true;
    }

    public static void clearScrollbarPressed() {
        scrollbarPointerDown = false;
    }

    public static boolean isScrollbarInteractionActive(boolean isDraggingHandle) {
        return isDraggingHandle || scrollbarPointerDown;
    }

    public static boolean shouldSuppressInventoryClose(boolean isDraggingHandle, EditBox... editBoxes) {
        if (isScrollbarInteractionActive(isDraggingHandle)) {
            return true;
        }
        if (editBoxes != null) {
            for (EditBox box : editBoxes) {
                if (box != null && box.isFocused()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Routes keys to focused EditBoxes and blocks inventory close when appropriate.
     *
     * @return true if the event was consumed
     */
    public static boolean handleContainerKeyPressed(
            AbstractContainerScreen<?> screen,
            KeyEvent event,
            boolean isDraggingHandle,
            EditBox... editBoxes) {
        if (editBoxes != null) {
            for (EditBox box : editBoxes) {
                if (box != null && box.isFocused()) {
                    if (box.keyPressed(event)) {
                        return true;
                    }
                    Minecraft mc = screen.getMinecraft();
                    if (mc != null && mc.options.keyInventory.matches(event)) {
                        return true;
                    }
                }
            }
        }

        if (shouldSuppressInventoryClose(isDraggingHandle, editBoxes)) {
            Minecraft mc = screen.getMinecraft();
            if (mc != null && mc.options.keyInventory.matches(event)) {
                return true;
            }
            if (isScrollbarInteractionActive(isDraggingHandle) && event.key() == 256) {
                return true;
            }
        }

        return false;
    }
}
