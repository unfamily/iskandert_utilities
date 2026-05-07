package net.unfamily.iskautils.mixin;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.unfamily.iskautils.crafting.StrictCraftingInputAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingInput.class)
public abstract class CraftingInputMixin implements StrictCraftingInputAccessor {
    @Unique
    private static final ThreadLocal<CraftingInputMixin.FullGrid> iska_utils$PENDING = new ThreadLocal<>();

    @Unique
    private @Nullable List<ItemStack> iska_utils$strictFullStacks;

    @Unique
    private int iska_utils$strictFullW;

    @Unique
    private int iska_utils$strictFullH;

    @Override
    public void iska_utils$attachStrictContext(@Nullable List<ItemStack> fullStacks, int fullWidth, int fullHeight) {
        this.iska_utils$strictFullStacks = fullStacks;
        this.iska_utils$strictFullW = fullWidth;
        this.iska_utils$strictFullH = fullHeight;
    }

    @Override
    public boolean iska_utils$hasStrictContext() {
        return this.iska_utils$strictFullStacks != null;
    }

    @Override
    public ItemStack iska_utils$getStrictSlot(int x, int y) {
        if (this.iska_utils$strictFullStacks == null || x < 0 || y < 0 || x >= this.iska_utils$strictFullW || y >= this.iska_utils$strictFullH) {
            return ItemStack.EMPTY;
        }
        return this.iska_utils$strictFullStacks.get(x + y * this.iska_utils$strictFullW);
    }

    @Inject(method = "of", remap = false, at = @At("HEAD"))
    private static void iska_utils$captureOfHead(int width, int height, List<ItemStack> items, CallbackInfoReturnable<CraftingInput> cir) {
        // Prefer capturing the full crafting grid from CraftingInput.of(...)
        // (callers often pass the real container dimensions here, before any shrinking happens).
        iska_utils$PENDING.set(new FullGrid(width, height, List.copyOf(items)));
    }

    @Inject(method = "of", remap = false, at = @At("RETURN"))
    private static void iska_utils$attachOfOnReturn(int width, int height, List<ItemStack> items, CallbackInfoReturnable<CraftingInput> cir) {
        try {
            FullGrid ctx = iska_utils$PENDING.get();
            if (ctx == null) {
                return;
            }
            CraftingInput input = cir.getReturnValue();
            if (input == null || input == CraftingInput.EMPTY) {
                return;
            }
            ((StrictCraftingInputAccessor) (Object) input).iska_utils$attachStrictContext(ctx.stacks(), ctx.width(), ctx.height());
        } finally {
            iska_utils$PENDING.remove();
        }
    }

    @Inject(method = "ofPositioned", remap = false, at = @At("HEAD"))
    private static void iska_utils$captureHead(int width, int height, List<ItemStack> items, CallbackInfoReturnable<CraftingInput.Positioned> cir) {
        // If we already captured a full grid from CraftingInput.of(...), keep it.
        // Otherwise, fall back to whatever the caller provided to ofPositioned(...).
        if (iska_utils$PENDING.get() == null) {
            iska_utils$PENDING.set(new FullGrid(width, height, List.copyOf(items)));
        }
    }

    @Inject(method = "ofPositioned", remap = false, at = @At("RETURN"))
    private static void iska_utils$attachOnReturn(int width, int height, List<ItemStack> items, CallbackInfoReturnable<CraftingInput.Positioned> cir) {
        try {
            FullGrid ctx = iska_utils$PENDING.get();
            if (ctx == null) {
                return;
            }
            CraftingInput.Positioned positioned = cir.getReturnValue();
            if (positioned == CraftingInput.Positioned.EMPTY) {
                return;
            }
            CraftingInput input = positioned.input();
            ((StrictCraftingInputAccessor) (Object) input).iska_utils$attachStrictContext(ctx.stacks(), ctx.width(), ctx.height());
        } finally {
            iska_utils$PENDING.remove();
        }
    }

    private record FullGrid(int width, int height, List<ItemStack> stacks) {}
}
