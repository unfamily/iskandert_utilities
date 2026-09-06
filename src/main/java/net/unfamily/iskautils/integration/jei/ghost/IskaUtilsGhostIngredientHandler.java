package net.unfamily.iskautils.integration.jei.ghost;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

/**
 * JEI ghost ingredient handler for Iska Utils screens.
 * Mirrors Another-Dynamics: register drop areas only; leave highlight rendering to JEI.
 */
public class IskaUtilsGhostIngredientHandler<T extends Screen> implements IGhostIngredientHandler<T> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(T gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (!(gui instanceof IIskaUtilsGhostTarget ghostTarget)) {
            return targets;
        }

        List<IIskaUtilsGhostTarget.GhostDropTarget> multi = ghostTarget.getGhostDropTargets();
        if (multi != null && !multi.isEmpty()) {
            for (IIskaUtilsGhostTarget.GhostDropTarget drop : multi) {
                if (drop == null || drop.area() == null || drop.accept() == null) {
                    continue;
                }
                tryAddMultiTarget(targets, drop, ingredient.getIngredient());
            }
            return targets;
        }

        tryAddLegacySingleTarget(targets, ghostTarget, ingredient.getIngredient());
        return targets;
    }

    @SuppressWarnings("unchecked")
    private <I> void tryAddMultiTarget(
            List<Target<I>> targets,
            IIskaUtilsGhostTarget.GhostDropTarget drop,
            I ingredient) {
        if (!(ingredient instanceof net.minecraft.world.item.ItemStack stack) || stack.isEmpty()) {
            return;
        }
        targets.add(new Target<>() {
            @Override
            public Rect2i getArea() {
                return drop.area();
            }

            @Override
            public void accept(I ingredientDropped) {
                if (ingredientDropped instanceof net.minecraft.world.item.ItemStack dropped && !dropped.isEmpty()) {
                    drop.accept().accept(dropped.copyWithCount(1));
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <I> void tryAddLegacySingleTarget(List<Target<I>> targets, IIskaUtilsGhostTarget ghostTarget, I ingredient) {
        IIskaUtilsGhostTarget.IGhostIngredientConsumer consumer = ghostTarget.getGhostHandler();
        if (consumer == null) {
            return;
        }
        Object validatedIngredient = consumer.supportedTarget(ingredient);
        if (validatedIngredient == null) {
            return;
        }
        Rect2i area = ghostTarget.getGhostTargetArea();
        if (area == null) {
            return;
        }
        targets.add(new Target<>() {
            @Override
            public Rect2i getArea() {
                return area;
            }

            @Override
            public void accept(I ingredientDropped) {
                Object validatedDropped = consumer.supportedTarget(ingredientDropped);
                consumer.accept(validatedDropped != null ? validatedDropped : ingredientDropped);
            }
        });
    }

    @Override
    public void onComplete() {
        // No custom overlay to clear — JEI owns highlight rendering.
    }

    @Override
    public boolean shouldHighlightTargets() {
        return true;
    }
}
