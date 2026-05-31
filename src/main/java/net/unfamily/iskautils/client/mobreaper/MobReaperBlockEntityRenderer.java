package net.unfamily.iskautils.client.mobreaper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.block.MobReaperBlock;
import net.unfamily.iskautils.block.entity.MobReaperBlockEntity;
import org.joml.Matrix4f;

import java.util.List;

public class MobReaperBlockEntityRenderer
        implements BlockEntityRenderer<MobReaperBlockEntity, MobReaperBlockEntityRenderer.ReaperRenderState> {

    private static final RandomSource MODEL_RANDOM = RandomSource.create(42L);
    private final BlockModelRenderState rotorRenderState = new BlockModelRenderState();

    public MobReaperBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static final class ReaperRenderState extends BlockEntityRenderState {
        boolean renderRotor;
        float angleDegrees;
        boolean vertical;
        Direction facing = Direction.NORTH;
    }

    @Override
    public ReaperRenderState createRenderState() {
        return new ReaperRenderState();
    }

    @Override
    public void extractRenderState(
            MobReaperBlockEntity blockEntity,
            ReaperRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        state.vertical = blockState.getValue(MobReaperBlock.VERTICAL);
        state.facing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        state.renderRotor = blockState.getValue(MobReaperBlock.POWERED);
        if (!state.renderRotor) {
            return;
        }
        MobReaperClientAnimation.poll(blockEntity, partialTicks);
        state.angleDegrees = MobReaperClientAnimation.getAngleDegrees(blockEntity.getBlockPos(), partialTicks);
    }

    @Override
    public void submit(
            ReaperRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        if (!state.renderRotor) {
            return;
        }

        BlockStateModel rotorModel = Minecraft.getInstance().getModelManager().getStandaloneModel(MobReaperClientRegistration.ROTOR_MODEL_KEY);
        if (rotorModel == null) {
            return;
        }

        poseStack.pushPose();
        MobReaperRotorTransforms.applyRotorTransform(poseStack, state.angleDegrees, state.vertical, state.facing);

        submitModel(rotorModel, poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private void submitModel(
            BlockStateModel model,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int light,
            int overlay) {
        List<BlockStateModelPart> parts = rotorRenderState.setupModel(new Matrix4f(), (model.materialFlags() & 1) != 0);
        model.collectParts(MODEL_RANDOM, parts);
        if (parts.isEmpty()) {
            return;
        }
        rotorRenderState.submitMultiLayer(poseStack, submitNodeCollector, light, overlay, 0);
    }
}
