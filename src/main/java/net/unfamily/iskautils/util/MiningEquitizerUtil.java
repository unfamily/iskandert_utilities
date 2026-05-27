package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.item.ModItems;

/**
 * Mining Equitizer: skip vanilla dig-speed penalties when equipped in Curios.
 */
public final class MiningEquitizerUtil {
    private MiningEquitizerUtil() {}

    public static boolean isEquipped(Player player) {
        return RelicActivationUtil.hasItemActiveInCurioOnly(player, ModItems.MINING_EQUITIZER.get());
    }

    public static boolean shouldNeutralizeWater(Player player) {
        return isEquipped(player) && isFullySubmergedInUnderwaterMiningFluid(player);
    }

    public static boolean shouldNeutralizeAir(Player player) {
        return isEquipped(player) && isMiningEquitizerFlying(player);
    }

    private static boolean isMiningEquitizerFlying(Player player) {
        return player.getAbilities().flying || player.isFallFlying();
    }

    private static boolean isFullySubmergedInUnderwaterMiningFluid(Player player) {
        Level level = player.level();
        Vec3 eyePos = player.getEyePosition(1.0f);
        BlockPos eyeBlock = BlockPos.containing(eyePos);
        FluidState eyeFluid = level.getFluidState(eyeBlock);
        BlockPos feetBlock = player.blockPosition();
        FluidState feetFluid = level.getFluidState(feetBlock);

        return fluidAppliesUnderwaterMiningSlowdown(eyeFluid)
                && fluidAppliesUnderwaterMiningSlowdown(feetFluid);
    }

    private static boolean fluidAppliesUnderwaterMiningSlowdown(FluidState state) {
        return !state.isEmpty() && state.is(FluidTags.WATER);
    }
}
