package net.unfamily.iskautils.mixin;

import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.util.DeepDrawerStackSizeContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Raises {@link ItemStack#getMaxStackSize()} while Deep Drawer code is on the stack,
 * so non-stackable items can stack inside drawers.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true, remap = false)
    private void modifyMaxStackSizeForDeepDrawer(CallbackInfoReturnable<Integer> cir) {
        if (DeepDrawerStackSizeContext.isInDeepDrawer()) {
            Integer original = cir.getReturnValue();
            int base = original != null ? original : 1;
            cir.setReturnValue(Math.max(base, 10000));
        }
    }
}
