package net.unfamily.iskautils.integration.emi;

import java.util.List;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.crafting.AncientTabRecipe;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeMatcher;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRequirement;
import net.unfamily.iskautils.item.ModItems;

public final class AncientTabEmiRecipe extends BasicEmiRecipe {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "ancient_tab"),
            EmiStack.of(ModItems.ANCIENT_TABLET.get()));

    public AncientTabEmiRecipe(RecipeHolder<AncientTabRecipe> holder) {
        super(CATEGORY, holder.id(), 140, 60);
        for (AncientTabletRecipeEntry entry : holder.value().compiledEntries()) {
            List<AncientTabletRequirement> req =
                    entry.require().isEmpty() && entry.hasIfVariants()
                            ? entry.ifVariants().getFirst().require()
                            : entry.require();
            List<AncientTabletRequirement> prod =
                    entry.produce().isEmpty() && entry.hasIfVariants()
                            ? entry.ifVariants().getFirst().produce()
                            : entry.produce();
            for (AncientTabletRecipeMatcher.GroupedRequirement g :
                    AncientTabletRecipeMatcher.groupConsecutive(req)) {
                ItemStack stack = example(g.requirement());
                if (!stack.isEmpty()) {
                    stack.setCount(Math.min(64, g.count()));
                    this.inputs.add(EmiStack.of(stack));
                }
            }
            for (AncientTabletRecipeMatcher.GroupedRequirement g :
                    AncientTabletRecipeMatcher.groupConsecutive(prod)) {
                ItemStack stack = example(g.requirement());
                if (!stack.isEmpty()) {
                    stack.setCount(Math.min(64, g.count()));
                    this.outputs.add(EmiStack.of(stack));
                }
            }
        }
    }

    private static ItemStack example(AncientTabletRequirement req) {
        return switch (req) {
            case AncientTabletRequirement.ItemRequirement ir -> new ItemStack(ir.item());
            case AncientTabletRequirement.TagRequirement tr ->
                    AncientTabletRecipeMatcher.exampleStackFromTag(tr);
        };
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < Math.min(inputs.size(), 6); i++) {
            widgets.addSlot(inputs.get(i), 8 + (i % 3) * 18, 12 + (i / 3) * 18);
        }
        for (int i = 0; i < Math.min(outputs.size(), 4); i++) {
            widgets.addSlot(outputs.get(i), 80 + (i % 2) * 18, 12 + (i / 2) * 18).recipeContext(this);
        }
    }

    @Override
    public boolean supportsRecipeTree() {
        return true;
    }
}
