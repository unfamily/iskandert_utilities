package net.unfamily.iskautils.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation MOBS =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "icon/vector_plate/mobfarm");
    private static final ResourceLocation PLAYER =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "icon/vector_plate/player");

    private VectorPlateIconDecorator() {}

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        if (!(blockItem.getBlock() instanceof VectorBlock vectorBlock)) {
            return false;
        }
        ResourceLocation sprite = vectorBlock.affectsPlayers() ? PLAYER : MOBS;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(xOffset, yOffset, 200 - 1);
        guiGraphics.blitSprite(sprite, 0, 0, 16, 16);
        guiGraphics.pose().popPose();
        return true;
    }
}
