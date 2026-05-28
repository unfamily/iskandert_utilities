package net.unfamily.iskautils.data.load.ancienttablet;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record AncientTabletRecipeEntry(
        Identifier sourceId,
        boolean mustOrdered,
        boolean destroyIfWrong,
        List<AncientTabletRequirement> require,
        List<AncientTabletRequirement> produce) {

    public List<ItemStack> previewProduceStacks() {
        return AncientTabletRecipeMatcher.expandToExampleStacks(produce);
    }
}
