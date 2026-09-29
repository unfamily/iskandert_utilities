package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.Config;

/**
 * Summoning Rituals–style FIXED item renders for Entropic Clock (26.x extract/submit).
 */
public final class EntropicClockItemRenderHelper {

    private static final float FULL_CIRCLE = 360f;
    private static final float HALF = 0.5f;
    private static final float ORBIT_DEGREES_PER_SECOND = 40f;
    private static final float ORBIT_RADIUS = 0.4f;
    private static final float ORBIT_ITEM_SCALE = 0.35f;
    public static final float OVERCLOCKER_Y = 1.2f;
    private static final float OVERCLOCKER_SCALE = 0.35f;
    public static final float SPAWNER_CENTER_Y = 0.5f;

    private EntropicClockItemRenderHelper() {}

    public static boolean shouldHideForVerticalNeighbor(Level level, BlockPos pos, Vec3 eye) {
        if (eye.y > pos.getY() + 1.0D) {
            return isOccludingNeighbor(level, pos.relative(Direction.UP));
        }
        if (eye.y < pos.getY()) {
            return isOccludingNeighbor(level, pos.relative(Direction.DOWN));
        }
        return false;
    }

    private static boolean isOccludingNeighbor(Level level, BlockPos neighborPos) {
        BlockState state = level.getBlockState(neighborPos);
        return !state.isAir() && state.canOcclude();
    }

    public static float orbitDegrees(Level level, float partialTick) {
        if (level == null) {
            return 0.0F;
        }
        float renderSeconds = (level.getGameTime() + partialTick) / 20.0F;
        return clampRotation(ORBIT_DEGREES_PER_SECOND * renderSeconds);
    }

    public static float clampRotation(float degree) {
        return ((degree % FULL_CIRCLE) + FULL_CIRCLE) % FULL_CIRCLE;
    }

    public static void updateItemState(ItemStackRenderState output, ItemStack clock, Level level) {
        output.clear();
        if (clock == null || clock.isEmpty()) {
            return;
        }
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                output,
                clock.copyWithCount(1),
                ItemDisplayContext.FIXED,
                level,
                null,
                0);
    }

    public static int orbitCount(ItemStack clockStack) {
        if (clockStack == null || clockStack.isEmpty()) {
            return 0;
        }
        return Math.min(clockStack.getCount(), Config.entropicSpawnerMaxEntropicClocks);
    }

    public static int lightAbove(Level level, BlockPos pos) {
        if (level == null) {
            return 15728880;
        }
        return LevelRenderer.getLightCoords(level, pos.above());
    }

    public static void submitSingleSpinning(
            ItemStackRenderState itemState,
            float spinDegrees,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords) {
        if (itemState.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(HALF, OVERCLOCKER_Y, HALF);
        poseStack.mulPose(Axis.YN.rotationDegrees(spinDegrees));
        poseStack.scale(OVERCLOCKER_SCALE, OVERCLOCKER_SCALE, OVERCLOCKER_SCALE);
        itemState.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static void submitOrbitingClocks(
            ItemStackRenderState itemState,
            int count,
            float orbitRotation,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords) {
        if (itemState.isEmpty() || count <= 0) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(HALF, SPAWNER_CENTER_Y, HALF);
        for (int i = 0; i < count; i++) {
            poseStack.pushPose();
            float itemAngle = FULL_CIRCLE - ((i * FULL_CIRCLE) / count);
            poseStack.mulPose(Axis.YN.rotationDegrees(clampRotation(itemAngle + orbitRotation)));
            poseStack.translate(0.0F, 0.0F, -ORBIT_RADIUS);
            poseStack.scale(ORBIT_ITEM_SCALE, ORBIT_ITEM_SCALE, ORBIT_ITEM_SCALE);
            itemState.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** Convenience for extract: eye-based vertical occlusion for spawner. */
    public static boolean shouldHideSpawnerClocks(BlockPos pos, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return true;
        }
        return shouldHideForVerticalNeighbor(level, pos, player.getEyePosition(partialTick));
    }
}
