package net.unfamily.iskautils.integration.rei;

import java.util.List;
import java.util.stream.Stream;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.client.gui.AutoShopScreen;
import net.unfamily.iskautils.client.gui.DeepDrawerExtractorScreen;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.client.gui.ShopEditScreen;
import net.unfamily.iskautils.client.gui.StructurePlacerMachineScreen;
import net.unfamily.iskautils.integration.jei.ghost.IIskaUtilsGhostTarget;

/**
 * REI ghost drop for Iska Utils screens exposing {@link IIskaUtilsGhostTarget}.
 */
public final class IskaUtilsReiGhostVisitor implements DraggableStackVisitor<Screen> {

    @Override
    public <R extends Screen> boolean isHandingScreen(R screen) {
        return screen instanceof ImprovedPatternCrafterScreen
                || screen instanceof DeepDrawerExtractorScreen
                || screen instanceof ShopEditScreen
                || screen instanceof AutoShopScreen
                || screen instanceof StructurePlacerMachineScreen;
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(DraggingContext<Screen> context, DraggableStack stack) {
        Screen screen = context.getScreen();
        if (!(screen instanceof IIskaUtilsGhostTarget ghostTarget)) {
            return DraggedAcceptorResult.PASS;
        }
        ItemStack item = asItem(stack);
        if (item.isEmpty()) {
            return DraggedAcceptorResult.PASS;
        }
        double mouseX = context.getCurrentPosition() != null ? context.getCurrentPosition().x : -1;
        double mouseY = context.getCurrentPosition() != null ? context.getCurrentPosition().y : -1;

        List<IIskaUtilsGhostTarget.GhostDropTarget> multi = ghostTarget.getGhostDropTargets();
        if (multi != null && !multi.isEmpty()) {
            for (IIskaUtilsGhostTarget.GhostDropTarget drop : multi) {
                if (drop == null || drop.area() == null || drop.accept() == null) {
                    continue;
                }
                if (contains(drop.area(), mouseX, mouseY)) {
                    drop.accept().accept(item.copyWithCount(1));
                    return DraggedAcceptorResult.ACCEPTED;
                }
            }
            return DraggedAcceptorResult.PASS;
        }

        IIskaUtilsGhostTarget.IGhostIngredientConsumer consumer = ghostTarget.getGhostHandler();
        Rect2i area = ghostTarget.getGhostTargetArea();
        if (consumer == null || area == null || !contains(area, mouseX, mouseY)) {
            return DraggedAcceptorResult.PASS;
        }
        Object validated = consumer.supportedTarget(item);
        consumer.accept(validated != null ? validated : item);
        return DraggedAcceptorResult.ACCEPTED;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(
            DraggingContext<Screen> context, DraggableStack stack) {
        Screen screen = context.getScreen();
        if (!(screen instanceof IIskaUtilsGhostTarget ghostTarget) || asItem(stack).isEmpty()) {
            return Stream.empty();
        }
        List<IIskaUtilsGhostTarget.GhostDropTarget> multi = ghostTarget.getGhostDropTargets();
        if (multi != null && !multi.isEmpty()) {
            return multi.stream()
                    .filter(d -> d != null && d.area() != null)
                    .map(d -> BoundsProvider.ofRectangle(toRectangle(d.area())));
        }
        Rect2i area = ghostTarget.getGhostTargetArea();
        if (area == null) {
            return Stream.empty();
        }
        return Stream.of(BoundsProvider.ofRectangle(toRectangle(area)));
    }

    private static Rectangle toRectangle(Rect2i area) {
        return new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight());
    }

    private static boolean contains(Rect2i area, double x, double y) {
        return x >= area.getX()
                && y >= area.getY()
                && x < area.getX() + area.getWidth()
                && y < area.getY() + area.getHeight();
    }

    private static ItemStack asItem(DraggableStack stack) {
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        EntryStack<?> entry = stack.getStack();
        Object value = entry.getValue();
        if (value instanceof ItemStack item && !item.isEmpty()) {
            return item;
        }
        return ItemStack.EMPTY;
    }
}
