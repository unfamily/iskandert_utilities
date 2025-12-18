package net.unfamily.iskautils.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.util.DeepDrawerStackSizeContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin to modify ItemStack.getMaxStackSize() when inside a Deep Drawer
 * This allows non-stackable items to become stackable (up to 4096) inside the Deep Drawer
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    
    @ModifyReturnValue(
            method = "getMaxStackSize()I",
            at = @At("RETURN")
    )
    private int modifyMaxStackSizeForDeepDrawer(int original) {
        // Only modify stack size when we're inside a Deep Drawer operation
        if (DeepDrawerStackSizeContext.isInDeepDrawer()) {
            // Allow stacking up to 10000 in Deep Drawer (even for non-stackable items)
            return Math.max(original, 10000);
        }
        return original;
    }
}
