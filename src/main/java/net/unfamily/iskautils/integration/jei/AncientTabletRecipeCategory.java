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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.GuiTextColors;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AncientTabletRecipeCategory implements IRecipeCategory<AncientTabletJeiRecipe> {

    public static final Identifier UID =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "ancient_tablet");

    public static final IRecipeType<AncientTabletJeiRecipe> RECIPE_TYPE =
            IRecipeType.create(UID, AncientTabletJeiRecipe.class);

    private static final int WIDTH = AncientTabletJeiBackgroundDrawable.OUTPUT_X
            + AncientTabletJeiBackgroundDrawable.COLS * AncientTabletJeiBackgroundDrawable.SLOT_SIZE
            + 6;
    private static final int HEIGHT = AncientTabletJeiBackgroundDrawable.WARN_Y + 28;

    private final IDrawable background;
    private final IDrawable icon;

    public AncientTabletRecipeCategory(IGuiHelper helper) {
        this.background = new AncientTabletJeiBackgroundDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.ANCIENT_TABLET.get()));
    }

    @Override
    public IRecipeType<AncientTabletJeiRecipe> getRecipeType() {
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
        return Component.translatable("jei.iska_utils.ancient_tablet");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(
            AncientTabletJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor guiGraphics,
            double mouseX,
            double mouseY) {
        background.draw(guiGraphics, 0, 0);
        int y = AncientTabletJeiBackgroundDrawable.WARN_Y;
        var font = Minecraft.getInstance().font;
        if (recipe.mustOrdered()) {
            guiGraphics.text(
                    font,
                    Component.translatable("jei.iska_utils.ancient_tablet.warn_ordered").withStyle(ChatFormatting.GOLD),
                    4,
                    y,
                    GuiTextColors.TITLE,
                    false);
            y += 10;
        }
        if (recipe.mustOrdered() && recipe.destroyIfWrong()) {
            guiGraphics.text(
                    font,
                    Component.translatable("jei.iska_utils.ancient_tablet.warn_destroy").withStyle(ChatFormatting.RED),
                    4,
                    y,
                    GuiTextColors.ERROR,
                    false);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AncientTabletJeiRecipe recipe, IFocusGroup focuses) {
        placeSlots(builder, RecipeIngredientRole.INPUT, recipe.inputs(), AncientTabletJeiBackgroundDrawable.INPUT_X);
        placeSlots(builder, RecipeIngredientRole.OUTPUT, recipe.outputs(), AncientTabletJeiBackgroundDrawable.OUTPUT_X);
    }

    private static void placeSlots(
            IRecipeLayoutBuilder builder,
            RecipeIngredientRole role,
            List<ItemStack> stacks,
            int originX) {
        int limit = Math.min(stacks.size(), AncientTabletJeiBackgroundDrawable.MAX_SLOTS);
        for (int i = 0; i < limit; i++) {
            int col = i % AncientTabletJeiBackgroundDrawable.COLS;
            int row = i / AncientTabletJeiBackgroundDrawable.COLS;
            int x = originX + col * AncientTabletJeiBackgroundDrawable.SLOT_SIZE
                    + AncientTabletJeiBackgroundDrawable.ITEM_OFFSET;
            int y = AncientTabletJeiBackgroundDrawable.GRID_Y + row * AncientTabletJeiBackgroundDrawable.SLOT_SIZE
                    + AncientTabletJeiBackgroundDrawable.ITEM_OFFSET;
            builder.addSlot(role, x, y).addItemStack(stacks.get(i));
        }
    }
}
