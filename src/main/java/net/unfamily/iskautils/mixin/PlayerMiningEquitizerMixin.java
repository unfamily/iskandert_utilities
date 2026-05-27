package net.unfamily.iskautils.mixin;

import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.unfamily.iskautils.util.MiningEquitizerUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Skips vanilla underwater / airborne mining penalties when Mining Equitizer is equipped in Curios.
 */
@Mixin(Player.class)
public abstract class PlayerMiningEquitizerMixin {

    @Redirect(
            method = "getDigSpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)F",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"
            )
    )
    private boolean iska_utils$redirectEyeInFluidForWaterPenalty(Entity instance, TagKey<Fluid> tag) {
        if (instance instanceof Player player
                && tag == FluidTags.WATER
                && MiningEquitizerUtil.shouldNeutralizeWater(player)) {
            return false;
        }
        return instance.isEyeInFluid(tag);
    }

    @Redirect(
            method = "getDigSpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)F",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;onGround()Z"
            )
    )
    private boolean iska_utils$redirectOnGroundForAirPenalty(Entity instance) {
        if (instance instanceof Player player && MiningEquitizerUtil.shouldNeutralizeAir(player)) {
            return true;
        }
        return instance.onGround();
    }
}
