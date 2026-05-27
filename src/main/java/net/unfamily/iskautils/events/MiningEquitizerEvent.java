package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber
public class MiningEquitizerEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiningEquitizerEvent.class);
    private static final String STAGE_ID = "iska_utils_internal-mining_equitizer_equip";

    @SubscribeEvent
    public static void onPlayerBreakSpeed(PlayerEvent.BreakSpeed event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        boolean inAir = isMiningEquitizerInAir(player);
        boolean inWaterStrict = isFullySubmergedInUnderwaterMiningFluid(player);
        boolean inWaterVanilla = player.isInWater();
        boolean inWater = inWaterStrict || inWaterVanilla;

        if (!inAir && !inWater) {
            return;
        }

        // Only active when in Curios (never in hand/inventory).
        if (!net.unfamily.iskautils.stage.StageRegistry.playerHasStage(player, STAGE_ID)) {
            return;
        }

        float originalSpeed = event.getOriginalSpeed();
        float multiplier;
        if (inWater) {
            multiplier = (float) Config.miningEquitizerWaterMultiplier;
        } else {
            multiplier = (float) Config.miningEquitizerAirMultiplier;
        }
        event.setNewSpeed(originalSpeed * multiplier);
    }

    /**
     * "Air" bonus: not standing on solid ground (jumping, falling, swimming, flying).
     */
    private static boolean isMiningEquitizerInAir(Player player) {
        return !player.onGround();
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
        // MC 1.21.1 NeoForge: use water tag for strict submersion (FluidType.isWaterLike is 26+).
        return !state.isEmpty() && state.is(FluidTags.WATER);
    }

    // Equipped checks are centralized in CurioEquipUtil to avoid duplication and to
    // guarantee consistent "equipped-only" semantics.
}
