package net.unfamily.iskautils.integration.jei.ghost;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

/**
 * JEI ghost ingredient handler for Iska Utils screens.
 * Enables drag-and-drop from JEI into GUI elements implementing {@link IIskaUtilsGhostTarget}.
 */
public class IskaUtilsGhostIngredientHandler<T extends Screen> implements IGhostIngredientHandler<T> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(T gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (gui instanceof IIskaUtilsGhostTarget ghostTarget) {
            tryAddTarget(targets, ghostTarget, ingredient.getIngredient());
        }
        return targets;
    }

    @SuppressWarnings("unchecked")
    private <I> void tryAddTarget(List<Target<I>> targets, IIskaUtilsGhostTarget ghostTarget, I ingredient) {
        IIskaUtilsGhostTarget.IGhostIngredientConsumer consumer = ghostTarget.getGhostHandler();
        if (consumer == null) {
            return;
        }
        Object validated = consumer.supportedTarget(ingredient);
        if (validated == null) {
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
                consumer.accept(validated);
            }
        });
    }

    @Override
    public boolean shouldHighlightTargets() {
        return true;
    }

    @Override
    public void onComplete() {
        // no-op
    }
}

