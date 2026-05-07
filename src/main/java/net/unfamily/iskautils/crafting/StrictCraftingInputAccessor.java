package net.unfamily.iskautils.crafting;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Attached to {@link net.minecraft.world.item.crafting.CraftingInput} from {@link net.minecraft.world.item.crafting.CraftingInput#ofPositioned}
 * so strict shaped recipes can read the original (un-trimmed) crafting grid.
 */
public interface StrictCraftingInputAccessor {
    void iska_utils$attachStrictContext(@Nullable List<ItemStack> fullStacks, int fullWidth, int fullHeight);

    boolean iska_utils$hasStrictContext();

    ItemStack iska_utils$getStrictSlot(int x, int y);
}
