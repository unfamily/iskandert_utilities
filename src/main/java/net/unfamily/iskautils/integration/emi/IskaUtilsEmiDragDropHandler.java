package net.unfamily.iskautils.integration.emi;

import java.util.List;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.integration.jei.ghost.IIskaUtilsGhostTarget;

/**
 * EMI ghost drop for screens exposing {@link IIskaUtilsGhostTarget}.
 */
public final class IskaUtilsEmiDragDropHandler<T extends Screen> implements EmiDragDropHandler<T> {

    @Override
    public boolean dropStack(T screen, EmiIngredient ingredient, int x, int y) {
        if (!(screen instanceof IIskaUtilsGhostTarget ghostTarget)) {
            return false;
        }
        ItemStack stack = firstItem(ingredient);
        if (stack.isEmpty()) {
            return false;
        }

        List<IIskaUtilsGhostTarget.GhostDropTarget> multi = ghostTarget.getGhostDropTargets();
        if (multi != null && !multi.isEmpty()) {
            for (IIskaUtilsGhostTarget.GhostDropTarget drop : multi) {
                if (drop == null || drop.area() == null || drop.accept() == null) {
                    continue;
                }
                if (contains(drop.area(), x, y)) {
                    drop.accept().accept(stack.copyWithCount(1));
                    return true;
                }
            }
            return false;
        }

        IIskaUtilsGhostTarget.IGhostIngredientConsumer consumer = ghostTarget.getGhostHandler();
        Rect2i area = ghostTarget.getGhostTargetArea();
        if (consumer == null || area == null || !contains(area, x, y)) {
            return false;
        }
        Object validated = consumer.supportedTarget(stack);
        consumer.accept(validated != null ? validated : stack);
        return true;
    }

    private static boolean contains(Rect2i area, int x, int y) {
        return x >= area.getX()
                && y >= area.getY()
                && x < area.getX() + area.getWidth()
                && y < area.getY() + area.getHeight();
    }

    private static ItemStack firstItem(EmiIngredient ingredient) {
        if (ingredient == null) {
            return ItemStack.EMPTY;
        }
        for (EmiStack emiStack : ingredient.getEmiStacks()) {
            ItemStack stack = emiStack.getItemStack();
            if (stack != null && !stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
