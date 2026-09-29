package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;

/**
 * Summoning Rituals–style FIXED item renders for Entropic Clock:
 * orbiting ring inside the spawner, single spinning clock above the overclocker.
 */
public final class EntropicClockItemRenderHelper {

    private static final float FULL_CIRCLE = 360f;
    private static final float HALF = 0.5f;
    private static final float ORBIT_DEGREES_PER_SECOND = 40f;
    private static final float ORBIT_RADIUS = 0.4f;
    private static final float ORBIT_ITEM_SCALE = 0.35f;
    private static final float OVERCLOCKER_Y = 1.2f;
    private static final float OVERCLOCKER_SCALE = 0.35f;
    private static final float SPAWNER_CENTER_Y = 0.5f;

    private EntropicClockItemRenderHelper() {}

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

    /** Single clock above the Temporal Overclocker, spinning on Y. */
    public static void renderSingleSpinning(
            ItemStack clock,
            BlockPos pos,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (clock == null || clock.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }

        ItemStack display = clock.copyWithCount(1);
        float spin = orbitDegrees(level, partialTick);
        // Sample light above the solid machine — BER packedLight is inside/under the cube and looks black
        int lightAbove = LevelRenderer.getLightColor(level, pos.above());

        poseStack.pushPose();
        poseStack.translate(HALF, OVERCLOCKER_Y, HALF);
        poseStack.mulPose(Axis.YN.rotationDegrees(spin));
        poseStack.scale(OVERCLOCKER_SCALE, OVERCLOCKER_SCALE, OVERCLOCKER_SCALE);
        minecraft.getItemRenderer().renderStatic(
                display,
                ItemDisplayContext.FIXED,
                lightAbove,
                packedOverlay,
                poseStack,
                buffer,
                level,
                0);
        poseStack.popPose();
    }

    /**
     * N clocks orbiting inside the Entropic Spawner (N = stack count, capped by config).
     * Always visible (no vertical-occlusion hide).
     */
    public static void renderOrbitingClocks(
            ItemStack clockStack,
            BlockPos pos,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (clockStack == null || clockStack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }

        int count = Math.min(clockStack.getCount(), Config.entropicSpawnerMaxEntropicClocks);
        if (count <= 0) {
            return;
        }

        ItemStack display = new ItemStack(ModItems.ENTROPIC_CLOCK.get());
        float orbitRotation = orbitDegrees(level, partialTick);

        poseStack.pushPose();
        poseStack.translate(HALF, SPAWNER_CENTER_Y, HALF);

        for (int i = 0; i < count; i++) {
            poseStack.pushPose();
            float itemAngle = FULL_CIRCLE - ((i * FULL_CIRCLE) / count);
            poseStack.mulPose(Axis.YN.rotationDegrees(clampRotation(itemAngle + orbitRotation)));
            poseStack.translate(0.0F, 0.0F, -ORBIT_RADIUS);
            poseStack.scale(ORBIT_ITEM_SCALE, ORBIT_ITEM_SCALE, ORBIT_ITEM_SCALE);
            minecraft.getItemRenderer().renderStatic(
                    display,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffer,
                    level,
                    i);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
