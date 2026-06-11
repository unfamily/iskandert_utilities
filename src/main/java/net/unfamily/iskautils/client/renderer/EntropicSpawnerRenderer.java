package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.block.EntropicSpawnerBlock;
import net.unfamily.iskautils.block.entity.EntropicSpawnerBlockEntity;

/**
 * Vanilla-style oblique mob preview when the Entropic Spawner is active.
 */
public class EntropicSpawnerRenderer
        implements BlockEntityRenderer<EntropicSpawnerBlockEntity, SpawnerRenderState> {

    private final EntityRenderDispatcher entityRenderer;

    public EntropicSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    @Override
    public SpawnerRenderState createRenderState() {
        return new SpawnerRenderState();
    }

    @Override
    public void extractRenderState(
            EntropicSpawnerBlockEntity blockEntity,
            SpawnerRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        if (!blockEntity.getBlockState().getValue(EntropicSpawnerBlock.ACTIVE)) {
            return;
        }
        var type = blockEntity.getSpawnEntityType();
        var level = blockEntity.getLevel();
        if (type == null || level == null) {
            return;
        }
        Entity displayEntity = blockEntity.getOrCreateDisplayEntity(level, type);
        if (displayEntity != null) {
            state.displayEntity = entityRenderer.extractEntity(displayEntity, partialTicks);
            state.displayEntity.lightCoords = state.lightCoords;
            state.spin = (float) Mth.lerp(partialTicks, blockEntity.getOSpin(), blockEntity.getSpin()) * 10.0F;
            state.scale = 0.53125F;
            float maxLength = Math.max(displayEntity.getBbWidth(), displayEntity.getBbHeight());
            if (maxLength > 1.0F) {
                state.scale /= maxLength;
            }
        }
    }

    @Override
    public void submit(
            SpawnerRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        if (state.displayEntity == null) {
            return;
        }
        SpawnerRenderer.submitEntityInSpawner(
                poseStack,
                submitNodeCollector,
                state.displayEntity,
                entityRenderer,
                state.spin,
                state.scale,
                camera);
    }

    @Override
    public AABB getRenderBoundingBox(EntropicSpawnerBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0,
                pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
