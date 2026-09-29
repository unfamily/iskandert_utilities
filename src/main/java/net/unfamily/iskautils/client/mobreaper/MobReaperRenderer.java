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
import net.unfamily.iskautils.client.renderer.SpecialRenderCulling;

/**
 * Renders the Mob Reaper rotor. Spins only when within special-render distance and looked at;
 * otherwise draws frozen at the last angle (still within distance).
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

        // Beyond special distance: no rotor BER (blockstate is base-only when powered).
        if (!SpecialRenderCulling.isWithinRenderDistance(blockEntity.getBlockPos())) {
            return;
        }

        MobReaperClientAnimation.poll(blockEntity, partialTick);
        boolean animate = SpecialRenderCulling.isLookingAt(blockEntity.getBlockPos());
        float angle = MobReaperClientAnimation.getAngleDegrees(
                blockEntity.getBlockPos(), animate ? partialTick : 0.0f);
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
        return SpecialRenderCulling.viewDistanceBlocks();
    }

    @Override
    public boolean shouldRenderOffScreen(MobReaperBlockEntity blockEntity) {
        return false;
    }
}
