package net.unfamily.iskautils.client.renderer;

import org.joml.Vector3fc;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.Config;

/**
 * Shared client culling for special BER effects (Dynaimics-style distance + look hemisphere).
 * Used by Entropic Spawner / Temporal Overclocker clocks and Mob Reaper blade spin.
 */
public final class SpecialRenderCulling {

    private SpecialRenderCulling() {}

    /**
     * Config distance capped by the client's Minecraft render distance (chunks × 16).
     */
    public static double effectiveRenderDistance() {
        double config = Config.specialRenderDistance;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) {
            return config;
        }
        double clientBlocks = mc.options.renderDistance().get() * 16.0;
        return Math.min(config, clientBlocks);
    }

    /** Integer blocks for {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer#getViewDistance()}. */
    public static int viewDistanceBlocks() {
        return Math.max(16, (int) Math.ceil(effectiveRenderDistance()));
    }

    public static boolean isWithinRenderDistance(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer != null ? mc.gameRenderer.getMainCamera() : null;
        if (camera == null) {
            return false;
        }
        return isWithinRenderDistance(pos, camera.position());
    }

    public static boolean isWithinRenderDistance(BlockPos pos, Vec3 cameraPos) {
        double dist = effectiveRenderDistance();
        return pos.distToCenterSqr(cameraPos) < dist * dist;
    }

    /**
     * True when the block center is in front of the camera (look · toBlock &gt; 0).
     */
    public static boolean isLookingAt(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer != null ? mc.gameRenderer.getMainCamera() : null;
        if (camera == null) {
            return false;
        }
        return isLookingAt(pos, camera.position(), camera);
    }

    public static boolean isLookingAt(BlockPos pos, Vec3 cameraPos, Camera camera) {
        Vec3 toBlock = Vec3.atCenterOf(pos).subtract(cameraPos);
        if (toBlock.lengthSqr() < 1.0e-6) {
            return true;
        }
        Vector3fc forward = camera.forwardVector();
        Vec3 look = new Vec3(forward.x(), forward.y(), forward.z());
        return look.dot(toBlock.normalize()) > 0.0;
    }

    /**
     * Distance + looking: use for animated / expensive effects (spinning clocks, spinning blades).
     */
    public static boolean shouldRenderDetailed(BlockPos pos) {
        return isWithinRenderDistance(pos) && isLookingAt(pos);
    }

    public static boolean shouldRenderDetailed(BlockPos pos, Vec3 cameraPos) {
        if (!isWithinRenderDistance(pos, cameraPos)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer != null ? mc.gameRenderer.getMainCamera() : null;
        if (camera == null) {
            return false;
        }
        return isLookingAt(pos, cameraPos, camera);
    }
}
