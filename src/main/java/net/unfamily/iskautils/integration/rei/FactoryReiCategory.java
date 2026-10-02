package net.unfamily.iskautils.integration.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.crafting.FactorySourcesRecipe;
import net.unfamily.iskautils.crafting.ModFactoryRecipes;
import net.unfamily.iskautils.data.load.FactoryLoader;
import net.unfamily.iskautils.item.ModItems;

public final class FactoryReiCategory implements DisplayCategory<FactoryReiCategory.FactoryDisplay> {
    public static final CategoryIdentifier<FactoryDisplay> ID =
            CategoryIdentifier.of(IskaUtils.MOD_ID, "factory");

    @Override
    public CategoryIdentifier<? extends FactoryDisplay> getCategoryIdentifier() {
        return ID;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.iska_utils.factory");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ModItems.FACTORY.get());
    }

    @Override
    public int getDisplayWidth(FactoryDisplay display) {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 66;
    }

    @Override
    public List<Widget> setupDisplay(FactoryDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        Point start = new Point(bounds.x + 8, bounds.y + 24);
        widgets.add(Widgets.createSlot(start).entries(display.getInputEntries().getFirst()).markInput());
        List<EntryIngredient> outs = display.getOutputEntries();
        for (int i = 0; i < Math.min(outs.size(), 8); i++) {
            int x = bounds.x + 40 + (i % 4) * 18;
            int y = bounds.y + 16 + (i / 4) * 18;
            widgets.add(Widgets.createSlot(new Point(x, y)).entries(outs.get(i)).markOutput());
        }
        return widgets;
    }

    public static void registerCategory(CategoryRegistry registry) {
        registry.add(new FactoryReiCategory());
        registry.addWorkstations(ID, EntryStacks.of(ModItems.FACTORY.get()));
    }

    public static void registerDisplays(DisplayRegistry registry) {
        registry.registerRecipeFiller(
                FactorySourcesRecipe.class,
                ModFactoryRecipes.FACTORY_TYPE.get(),
                FactoryDisplay::fromHolder);
    }

    public static final class FactoryDisplay extends BasicDisplay {
        private FactoryDisplay(
                List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<ResourceLocation> location) {
            super(inputs, outputs, location);
        }

        public static FactoryDisplay fromHolder(RecipeHolder<FactorySourcesRecipe> holder) {
            List<EntryIngredient> inputs = new ArrayList<>();
            List<EntryIngredient> outputs = new ArrayList<>();
            for (FactoryLoader.Source src : holder.value().compiledSources()) {
                List<ItemStack> inStacks = FactoryLoader.expandInputForJei(src);
                if (!inStacks.isEmpty()) {
                    inputs.add(EntryIngredients.ofItemStacks(inStacks));
                }
                for (FactoryLoader.Output out : src.flatOutputs().isEmpty()
                        ? src.ifBranches().isEmpty()
                                ? List.<FactoryLoader.Output>of()
                                : src.ifBranches().getFirst().outputs()
                        : src.flatOutputs()) {
                    FactoryLoader.resolveOutputStack(out)
                            .ifPresent(stack -> outputs.add(EntryIngredients.of(stack)));
                }
            }
            if (inputs.isEmpty()) {
                inputs.add(EntryIngredient.empty());
            }
            return new FactoryDisplay(inputs, outputs, Optional.of(holder.id()));
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return ID;
        }
    }
}
