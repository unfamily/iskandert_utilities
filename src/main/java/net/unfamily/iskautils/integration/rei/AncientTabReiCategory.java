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
import net.unfamily.iskautils.crafting.AncientTabRecipe;
import net.unfamily.iskautils.crafting.ModAncientTabRecipes;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeMatcher;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRequirement;
import net.unfamily.iskautils.item.ModItems;

public final class AncientTabReiCategory implements DisplayCategory<AncientTabReiCategory.AncientTabDisplay> {
    public static final CategoryIdentifier<AncientTabDisplay> ID =
            CategoryIdentifier.of(IskaUtils.MOD_ID, "ancient_tab");

    @Override
    public CategoryIdentifier<? extends AncientTabDisplay> getCategoryIdentifier() {
        return ID;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.iska_utils.ancient_tablet");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ModItems.ANCIENT_TABLET.get());
    }

    @Override
    public int getDisplayWidth(AncientTabDisplay display) {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 66;
    }

    @Override
    public List<Widget> setupDisplay(AncientTabDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        List<EntryIngredient> ins = display.getInputEntries();
        for (int i = 0; i < Math.min(ins.size(), 6); i++) {
            int x = bounds.x + 8 + (i % 3) * 18;
            int y = bounds.y + 16 + (i / 3) * 18;
            widgets.add(Widgets.createSlot(new Point(x, y)).entries(ins.get(i)).markInput());
        }
        List<EntryIngredient> outs = display.getOutputEntries();
        for (int i = 0; i < Math.min(outs.size(), 4); i++) {
            int x = bounds.x + 80 + (i % 2) * 18;
            int y = bounds.y + 16 + (i / 2) * 18;
            widgets.add(Widgets.createSlot(new Point(x, y)).entries(outs.get(i)).markOutput());
        }
        return widgets;
    }

    public static void registerCategory(CategoryRegistry registry) {
        registry.add(new AncientTabReiCategory());
        registry.addWorkstations(ID, EntryStacks.of(ModItems.ANCIENT_TABLET.get()));
        registry.addWorkstations(ID, EntryStacks.of(ModItems.ANCIENT_TABLE.get()));
    }

    public static void registerDisplays(DisplayRegistry registry) {
        registry.registerRecipeFiller(
                AncientTabRecipe.class, ModAncientTabRecipes.ANCIENT_TAB_TYPE.get(), AncientTabDisplay::fromHolder);
    }

    public static final class AncientTabDisplay extends BasicDisplay {
        private AncientTabDisplay(
                List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<ResourceLocation> location) {
            super(inputs, outputs, location);
        }

        public static AncientTabDisplay fromHolder(RecipeHolder<AncientTabRecipe> holder) {
            List<EntryIngredient> inputs = new ArrayList<>();
            List<EntryIngredient> outputs = new ArrayList<>();
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
                        inputs.add(EntryIngredients.of(stack));
                    }
                }
                for (AncientTabletRecipeMatcher.GroupedRequirement g :
                        AncientTabletRecipeMatcher.groupConsecutive(prod)) {
                    ItemStack stack = example(g.requirement());
                    if (!stack.isEmpty()) {
                        stack.setCount(Math.min(64, g.count()));
                        outputs.add(EntryIngredients.of(stack));
                    }
                }
            }
            if (inputs.isEmpty()) {
                inputs.add(EntryIngredient.empty());
            }
            return new AncientTabDisplay(inputs, outputs, Optional.of(holder.id()));
        }

        private static ItemStack example(AncientTabletRequirement req) {
            return switch (req) {
                case AncientTabletRequirement.ItemRequirement ir -> new ItemStack(ir.item());
                case AncientTabletRequirement.TagRequirement tr ->
                        AncientTabletRecipeMatcher.exampleStackFromTag(tr);
            };
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return ID;
        }
    }
}
