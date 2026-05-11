package net.unfamily.iskautils.integration.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FactoryRecipeCategory implements IRecipeCategory<FactoryJeiRecipe> {

    public static final Identifier UID = Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory");

    private static final int WIDTH = 200;
    private static final int HEIGHT =
            FactoryJeiBackgroundDrawable.BG_PADDING_V
                    + FactoryJeiBackgroundDrawable.GRID_ROWS * FactoryJeiBackgroundDrawable.SLOT_SIZE
                    + FactoryJeiBackgroundDrawable.BG_PADDING_V;

    public static final IRecipeType<FactoryJeiRecipe> RECIPE_TYPE = IRecipeType.create(UID, FactoryJeiRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public FactoryRecipeCategory(IGuiHelper helper) {
        this.background = new FactoryJeiBackgroundDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.FACTORY.get()));
    }

    @Override
    public IRecipeType<FactoryJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.iska_utils.factory");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FactoryJeiRecipe recipe, IFocusGroup focuses) {
        int ox = FactoryJeiBackgroundDrawable.INPUT_X + FactoryJeiBackgroundDrawable.ITEM_OFFSET;
        int oy = FactoryJeiBackgroundDrawable.INPUT_Y + FactoryJeiBackgroundDrawable.ITEM_OFFSET;
        builder.addSlot(RecipeIngredientRole.INPUT, ox, oy).addItemStacks(recipe.inputs());

        List<ItemStack> outs = recipe.outputs();
        for (int i = 0; i < outs.size(); i++) {
            int col = i % FactoryJeiBackgroundDrawable.GRID_COLS;
            int row = i / FactoryJeiBackgroundDrawable.GRID_COLS;
            int x = FactoryJeiBackgroundDrawable.GRID_X + col * FactoryJeiBackgroundDrawable.SLOT_SIZE
                    + FactoryJeiBackgroundDrawable.ITEM_OFFSET;
            int y = FactoryJeiBackgroundDrawable.GRID_Y + row * FactoryJeiBackgroundDrawable.SLOT_SIZE
                    + FactoryJeiBackgroundDrawable.ITEM_OFFSET;
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).addItemStack(outs.get(i));
        }
    }

    @Override
    public void draw(FactoryJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics, 0, 0);
    }
}
