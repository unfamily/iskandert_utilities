package net.unfamily.iskautils.integration.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiDescriptions;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.client.gui.GuiTextColors;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArcaneDictionaryRecipeCategory implements IRecipeCategory<ArcaneDictionaryJeiRecipe> {

    public static final Identifier UID =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "arcane_dictionary");

    public static final IRecipeType<ArcaneDictionaryJeiRecipe> RECIPE_TYPE =
            IRecipeType.create(UID, ArcaneDictionaryJeiRecipe.class);

    private static final ItemStack DICTIONARY_STACK = new ItemStack(ModItems.ARCANE_DICTIONARY.get());

    private final IDrawable icon;

    public ArcaneDictionaryRecipeCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, DICTIONARY_STACK);
    }

    @Override
    public IRecipeType<ArcaneDictionaryJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.iska_utils.arcane_dictionary");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return ArcaneDictionaryJeiDescriptions.WIDTH;
    }

    @Override
    public int getHeight() {
        return ArcaneDictionaryJeiRecipes.maxHeight();
    }

    @Override
    public void draw(
            ArcaneDictionaryJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor graphics,
            double mouseX,
            double mouseY) {
        int y = ArcaneDictionaryJeiDescriptions.TEXT_Y;
        var font = Minecraft.getInstance().font;
        for (FormattedCharSequence line : recipe.displayLines()) {
            if (line == ArcaneDictionaryJeiDescriptions.emptyLine()) {
                y += ArcaneDictionaryJeiDescriptions.LINE_HEIGHT / 2;
                continue;
            }
            graphics.text(font, line, ArcaneDictionaryJeiDescriptions.TEXT_X, y, GuiTextColors.BODY, false);
            y += ArcaneDictionaryJeiDescriptions.LINE_HEIGHT;
        }

        if (recipe.hasCatalysts()) {
            int labelY = ArcaneDictionaryJeiDescriptions.catalystLabelY(recipe.catalystRowY());
            graphics.text(
                    font,
                    Component.translatable("jei.iska_utils.arcane_trait.meta.catalyst_label"),
                    ArcaneDictionaryJeiDescriptions.TEXT_X,
                    labelY,
                    GuiTextColors.BODY,
                    false);
        } else {
            graphics.text(
                    font,
                    Component.translatable("jei.iska_utils.arcane_trait.meta.catalyst_none"),
                    ArcaneDictionaryJeiDescriptions.TEXT_X,
                    recipe.catalystRowY(),
                    GuiTextColors.BODY,
                    false);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArcaneDictionaryJeiRecipe recipe, IFocusGroup focuses) {
        if (!recipe.hasCatalysts()) {
            return;
        }
        var font = Minecraft.getInstance().font;
        Component label = Component.translatable("jei.iska_utils.arcane_trait.meta.catalyst_label");
        int slotStartX = ArcaneDictionaryJeiDescriptions.TEXT_X
                + font.width(label)
                + ArcaneDictionaryJeiBackgroundDrawable.CATALYST_LABEL_GAP;

        List<ArcaneDictionaryJeiLines.ResolvedCatalyst> catalysts = recipe.catalysts();
        for (int i = 0; i < catalysts.size(); i++) {
            int x = slotStartX
                    + i * (ArcaneDictionaryJeiBackgroundDrawable.SLOT_SIZE
                            + ArcaneDictionaryJeiBackgroundDrawable.CATALYST_GAP)
                    + ArcaneDictionaryJeiBackgroundDrawable.ITEM_OFFSET;
            int y = recipe.catalystRowY() + ArcaneDictionaryJeiBackgroundDrawable.ITEM_OFFSET;
            ArcaneDictionaryJeiLines.ResolvedCatalyst catalyst = catalysts.get(i);
            List<ItemStack> stacks = catalyst.stacks();
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, x, y);
            if (stacks.size() == 1) {
                slot.addItemStack(stacks.getFirst());
            } else {
                slot.addItemStacks(stacks);
            }
            slot.addRichTooltipCallback((view, tooltip) -> {
                tooltip.clear();
                tooltip.addAll(ArcaneDictionaryJeiLines.catalystTooltipLines(recipe.entry(), catalyst));
            });
        }
    }
}
