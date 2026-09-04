package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Nullable;

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
            int keyCode,
            int scanCode,
            int modifiers,
            boolean isDraggingHandle,
            EditBox... editBoxes) {
        if (editBoxes != null) {
            for (EditBox box : editBoxes) {
                if (box != null && box.isFocused()) {
                    if (box.keyPressed(keyCode, scanCode, modifiers)) {
                        return true;
                    }
                    Minecraft mc = screen.getMinecraft();
                    if (mc != null && mc.options.keyInventory.matches(keyCode, scanCode)) {
                        return true;
                    }
                }
            }
        }

        if (shouldSuppressInventoryClose(isDraggingHandle, editBoxes)) {
            Minecraft mc = screen.getMinecraft();
            if (mc != null && mc.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
            if (isScrollbarInteractionActive(isDraggingHandle) && keyCode == 256) {
                return true;
            }
        }

        return false;
    }

    /**
     * Clears EditBox content and resets the cursor (Deep Drawer Extractor / Structure Saver style).
     */
    public static void clearEditBox(@Nullable EditBox box) {
        if (box == null) {
            return;
        }
        box.setValue("");
        box.setCursorPosition(0);
        box.setHighlightPos(0);
    }

    /**
     * Right-click on a visible EditBox clears its content.
     *
     * @return true if an EditBox was cleared
     */
    public static boolean clearEditBoxOnRightClick(double mouseX, double mouseY, int button, EditBox... editBoxes) {
        if (button != 1 || editBoxes == null) {
            return false;
        }
        for (EditBox box : editBoxes) {
            if (box == null || !box.visible || !box.active) {
                continue;
            }
            if (box.isMouseOver(mouseX, mouseY)) {
                clearEditBox(box);
                return true;
            }
        }
        return false;
    }
}
