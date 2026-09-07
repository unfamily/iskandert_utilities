package net.unfamily.iskautils.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.VectorBlock;

/**
 * Corner badge on vector plate items (mobs vs player). Registered only on the ten
 * vector plate BlockItems — not plate base / potion / crystal cage.
 */
public final class VectorPlateIconDecorator implements IItemDecorator {
    public static final VectorPlateIconDecorator INSTANCE = new VectorPlateIconDecorator();

    private static final Identifier MOBS =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "icon/vector_plate/mobfarm");
    private static final Identifier PLAYER =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "icon/vector_plate/player");

    private VectorPlateIconDecorator() {}

    @Override
    public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        if (!(blockItem.getBlock() instanceof VectorBlock vectorBlock)) {
            return false;
        }
        Identifier sprite = vectorBlock.affectsPlayers() ? PLAYER : MOBS;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16);
        return true;
    }
}
