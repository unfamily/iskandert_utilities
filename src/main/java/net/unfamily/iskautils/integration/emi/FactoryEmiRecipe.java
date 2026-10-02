package net.unfamily.iskautils.integration.emi;

import java.util.ArrayList;
import java.util.List;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.crafting.FactorySourcesRecipe;
import net.unfamily.iskautils.data.load.FactoryLoader;
import net.unfamily.iskautils.item.ModItems;

public final class FactoryEmiRecipe extends BasicEmiRecipe {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory"),
            EmiStack.of(ModItems.FACTORY.get()));

    public FactoryEmiRecipe(RecipeHolder<FactorySourcesRecipe> holder) {
        super(CATEGORY, holder.id(), 140, 60);
        for (FactoryLoader.Source src : holder.value().compiledSources()) {
            List<ItemStack> inputs = FactoryLoader.expandInputForJei(src);
            if (!inputs.isEmpty()) {
                this.inputs.add(EmiIngredient.of(inputs.stream().map(EmiStack::of).toList()));
            }
            List<FactoryLoader.Output> outs =
                    !src.flatOutputs().isEmpty()
                            ? src.flatOutputs()
                            : src.ifBranches().isEmpty()
                                    ? List.of()
                                    : src.ifBranches().getFirst().outputs();
            for (FactoryLoader.Output out : outs) {
                FactoryLoader.resolveOutputStack(out).ifPresent(stack -> this.outputs.add(EmiStack.of(stack)));
            }
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        if (!inputs.isEmpty()) {
            widgets.addSlot(inputs.getFirst(), 8, 20);
        }
        for (int i = 0; i < Math.min(outputs.size(), 8); i++) {
            widgets.addSlot(outputs.get(i), 40 + (i % 4) * 18, 12 + (i / 4) * 18).recipeContext(this);
        }
    }

    @Override
    public boolean supportsRecipeTree() {
        return true;
    }
}
