package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;

/**
 * Single Entropic Clock floating above the Temporal Overclocker, spinning on Y.
 */
public class TemporalOverclockerRenderer implements BlockEntityRenderer<TemporalOverclockerBlockEntity> {

    public TemporalOverclockerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TemporalOverclockerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!SpecialRenderCulling.shouldRenderDetailed(blockEntity.getBlockPos())) {
            return;
        }
        ItemStack clock = blockEntity.getMachineItems().getItem(TemporalOverclockerBlockEntity.UPGRADE_SLOT_INDEX);
        EntropicClockItemRenderHelper.renderSingleSpinning(
                clock,
                blockEntity.getBlockPos(),
                partialTick,
                poseStack,
                buffer,
                packedLight,
                packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(TemporalOverclockerBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5,
                pos.getX() + 1.5, pos.getY() + 2.0, pos.getZ() + 1.5);
    }

    @Override
    public int getViewDistance() {
        return SpecialRenderCulling.viewDistanceBlocks();
    }
}
