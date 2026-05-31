package net.unfamily.iskautils.client.mobreaper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;

/**
 * Shared pose-stack transforms for the Mob Reaper rotor BER.
 * Block orientation matches mob_reaper blockstate variant rotations (x then y).
 * Spin is applied after orientation around the oriented local Y axis (model rod axis) at the rod pivot.
 */
public final class MobReaperRotorTransforms {

    private static final float HALF = 0.5f;
    /** Rod center Y in block space (model rod spans y=1..16). */
    private static final float SPIN_PIVOT_Y = 8.5f / 16.0f;

    private MobReaperRotorTransforms() {}

    /**
     * Orients like the static block model, then spins around the rod axis in that local frame.
     */
    public static void applyRotorTransform(PoseStack poseStack, float angleDegrees, boolean vertical, Direction facing) {
        applyBlockOrientation(poseStack, vertical, facing);
        poseStack.translate(HALF, SPIN_PIVOT_Y, HALF);
        poseStack.mulPose(Axis.YP.rotationDegrees(angleDegrees));
        poseStack.translate(-HALF, -SPIN_PIVOT_Y, -HALF);
    }

    public static void applyBlockOrientation(PoseStack poseStack, boolean vertical, Direction facing) {
        BlockModelRotation rotation = blockModelRotation(vertical, facing);
        poseStack.translate(HALF, HALF, HALF);
        poseStack.mulPose(rotation.getRotation().getLeftRotation());
        poseStack.translate(-HALF, -HALF, -HALF);
    }

    private static BlockModelRotation blockModelRotation(boolean vertical, Direction facing) {
        int x = vertical ? 90 : 0;
        int y = switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        return BlockModelRotation.by(x, y);
    }
}
