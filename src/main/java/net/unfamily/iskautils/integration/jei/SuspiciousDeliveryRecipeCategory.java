package net.unfamily.iskautils.integration.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class SuspiciousDeliveryRecipeCategory implements IRecipeCategory<SuspiciousDeliveryJeiRecipe> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "suspicious_delivery");

    private static final int WIDTH = 200;
    private static final int HEIGHT =
            FactoryJeiBackgroundDrawable.BG_PADDING_V
                    + FactoryJeiBackgroundDrawable.GRID_ROWS * FactoryJeiBackgroundDrawable.SLOT_SIZE
                    + FactoryJeiBackgroundDrawable.BG_PADDING_V;

    public static final RecipeType<SuspiciousDeliveryJeiRecipe> RECIPE_TYPE =
            new RecipeType<>(UID, SuspiciousDeliveryJeiRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public SuspiciousDeliveryRecipeCategory(IGuiHelper helper) {
        this.background = new FactoryJeiBackgroundDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModItems.SUSPICIOUS_DELIVERY.get()));
    }

    @Override
    public RecipeType<SuspiciousDeliveryJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.iska_utils.suspicious_delivery");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SuspiciousDeliveryJeiRecipe recipe, IFocusGroup focuses) {
        int ox = FactoryJeiBackgroundDrawable.INPUT_X + FactoryJeiBackgroundDrawable.ITEM_OFFSET;
        int oy = FactoryJeiBackgroundDrawable.INPUT_Y + FactoryJeiBackgroundDrawable.ITEM_OFFSET;
        builder.addSlot(RecipeIngredientRole.INPUT, ox, oy)
                .addItemStack(new ItemStack(ModItems.SUSPICIOUS_DELIVERY.get()));

        List<SuspiciousDeliveryJeiEntry> entries = recipe.entries();
        for (int i = 0; i < entries.size(); i++) {
            SuspiciousDeliveryJeiEntry entry = entries.get(i);
            int col = i % FactoryJeiBackgroundDrawable.GRID_COLS;
            int row = i / FactoryJeiBackgroundDrawable.GRID_COLS;
            int x = FactoryJeiBackgroundDrawable.GRID_X + col * FactoryJeiBackgroundDrawable.SLOT_SIZE
                    + FactoryJeiBackgroundDrawable.ITEM_OFFSET;
            int y = FactoryJeiBackgroundDrawable.GRID_Y + row * FactoryJeiBackgroundDrawable.SLOT_SIZE
                    + FactoryJeiBackgroundDrawable.ITEM_OFFSET;

            var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y);
            List<ItemStack> stacks = entry.displayStacks();
            if (stacks.size() == 1) {
                slot.addItemStack(stacks.get(0));
            } else {
                slot.addItemStacks(stacks);
            }
            String pct = String.format(Locale.ROOT, "%.1f%%", entry.weightPercent());
            slot.addRichTooltipCallback((view, tooltip) -> {
                tooltip.add(Component.literal(pct));
                if (entry.entryLuck() != 0) {
                    tooltip.add(Component.literal("luck: " + entry.entryLuck()));
                }
            });
        }
    }
}
