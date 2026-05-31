package net.unfamily.iskautils.client.mobreaper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.unfamily.iskautils.block.MobReaperBlock;
import net.unfamily.iskautils.block.entity.MobReaperBlockEntity;

/**
 * Renders the Mob Reaper rotor with Y-axis rotation when powered.
 */
public class MobReaperRenderer implements BlockEntityRenderer<MobReaperBlockEntity> {

    private final BakedModel rotorModel;

    public MobReaperRenderer(BlockEntityRendererProvider.Context context) {
        this.rotorModel = context.getBlockRenderDispatcher().getBlockModelShaper().getModelManager().getModel(MobReaperClientRegistration.ROTOR_MODEL);
    }

    @Override
    public void render(MobReaperBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        if (!state.getValue(MobReaperBlock.POWERED)) {
            return;
        }

        MobReaperClientAnimation.poll(blockEntity, partialTick);
        float angle = MobReaperClientAnimation.getAngleDegrees(blockEntity.getBlockPos(), partialTick);
        boolean vertical = state.getValue(MobReaperBlock.VERTICAL);
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        int light = LevelRenderer.getLightColor(level, blockEntity.getBlockPos());

        poseStack.pushPose();
        MobReaperRotorTransforms.applyRotorTransform(poseStack, angle, vertical, facing);

        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                null,
                rotorModel,
                1.0f, 1.0f, 1.0f,
                light,
                combinedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public boolean shouldRenderOffScreen(MobReaperBlockEntity blockEntity) {
        return false;
    }
}
