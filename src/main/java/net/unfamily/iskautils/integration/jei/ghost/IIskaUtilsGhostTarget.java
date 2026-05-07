package net.unfamily.iskautils.integration.jei.ghost;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Implement on a Screen to accept JEI ghost ingredient drops.
 */
public interface IIskaUtilsGhostTarget {

    @Nullable
    IGhostIngredientConsumer getGhostHandler();

    @Nullable
    default Rect2i getGhostTargetArea() {
        return null;
    }

    interface IGhostIngredientConsumer {
        @Nullable
        Object supportedTarget(Object ingredient);

        void accept(Object ingredient);
    }

    interface IGhostItemConsumer extends IGhostIngredientConsumer {
        @Nullable
        @Override
        default ItemStack supportedTarget(Object ingredient) {
            return ingredient instanceof ItemStack stack && !stack.isEmpty() ? stack : null;
        }
    }
}

