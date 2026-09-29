package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;

/**
 * Single Entropic Clock floating above the Temporal Overclocker, spinning on Y.
 */
public class TemporalOverclockerRenderer
        implements BlockEntityRenderer<TemporalOverclockerBlockEntity, TemporalOverclockerRenderer.State> {

    public TemporalOverclockerRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static final class State extends BlockEntityRenderState {
        boolean renderClock;
        float spinDegrees;
        final ItemStackRenderState clockItem = new ItemStackRenderState();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            TemporalOverclockerBlockEntity blockEntity,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);

        ItemStack clock = blockEntity.getMachineItems().getItem(TemporalOverclockerBlockEntity.UPGRADE_SLOT_INDEX);
        state.renderClock = !clock.isEmpty()
                && SpecialRenderCulling.shouldRenderDetailed(blockEntity.getBlockPos(), cameraPosition);
        state.spinDegrees = EntropicClockItemRenderHelper.orbitDegrees(blockEntity.getLevel(), partialTicks);
        if (state.renderClock) {
            // BER light is sampled inside the solid cube; use light above for the floating item
            state.lightCoords = EntropicClockItemRenderHelper.lightAbove(blockEntity.getLevel(), blockEntity.getBlockPos());
            EntropicClockItemRenderHelper.updateItemState(state.clockItem, clock, blockEntity.getLevel());
        } else {
            state.clockItem.clear();
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        if (!state.renderClock) {
            return;
        }
        EntropicClockItemRenderHelper.submitSingleSpinning(
                state.clockItem,
                state.spinDegrees,
                poseStack,
                submitNodeCollector,
                state.lightCoords);
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
