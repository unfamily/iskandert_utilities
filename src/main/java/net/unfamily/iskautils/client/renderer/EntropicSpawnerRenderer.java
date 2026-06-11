package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.unfamily.iskautils.block.EntropicSpawnerBlock;
import net.unfamily.iskautils.block.entity.EntropicSpawnerBlockEntity;

/**
 * Vanilla-style oblique mob preview when the Entropic Spawner is active.
 */
public class EntropicSpawnerRenderer implements BlockEntityRenderer<EntropicSpawnerBlockEntity> {

    private final EntityRenderDispatcher entityRenderer;

    public EntropicSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(EntropicSpawnerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null || !blockEntity.getBlockState().getValue(EntropicSpawnerBlock.ACTIVE)) {
            return;
        }
        EntityType<?> type = blockEntity.getSpawnEntityType();
        if (type == null) {
            return;
        }

        Entity entity = blockEntity.getOrCreateDisplayEntity(level, type);
        if (entity == null) {
            return;
        }

        SpawnerRenderer.renderEntityInSpawner(
                partialTick,
                poseStack,
                buffer,
                packedLight,
                entity,
                entityRenderer,
                blockEntity.getOSpin(),
                blockEntity.getSpin());
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
