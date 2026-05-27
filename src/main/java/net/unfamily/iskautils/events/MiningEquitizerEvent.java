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
import net.neoforged.neoforge.common.extensions.IFluidStateExtension;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskalib.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

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
        if (!StageRegistry.playerHasStage(player, STAGE_ID)) {
            return;
        }

        float originalSpeed = event.getOriginalSpeed();
        float multiplier = inWater
                ? (float) Config.miningEquitizerWaterMultiplier
                : (float) Config.miningEquitizerAirMultiplier;
        event.setNewSpeed(originalSpeed * multiplier);
    }

    /**
     * "Air" bonus: not standing on solid ground (jumping, falling, swimming, flying).
     */
    private static boolean isMiningEquitizerInAir(Player player) {
        return !player.onGround();
    }

    /**
     * Full submersion in a fluid that counts for the water-only multiplier (tag water or water-like fluid type).
     */
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
        if (state.isEmpty()) {
            return false;
        }
        if (state.is(FluidTags.WATER)) {
            return true;
        }
        FluidType type = ((IFluidStateExtension) (Object) state).getFluidType();
        return type.getIsWaterLike();
    }

    // Equipped checks are centralized in CurioEquipUtil to avoid duplication and to
    // guarantee consistent "equipped-only" semantics.
}
