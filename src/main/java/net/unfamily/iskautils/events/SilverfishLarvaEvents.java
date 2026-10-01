package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.integration.exdeorum.ExDeorumCompat;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class SilverfishLarvaEvents {
    private SilverfishLarvaEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Silverfish silverfish)) {
            return;
        }
        Level level = silverfish.level();
        if (level.isClientSide() || !ExDeorumCompat.isLoaded()) {
            return;
        }
        BlockPos feet = BlockPos.containing(silverfish.getX(), silverfish.getY() + 0.125D, silverfish.getZ());
        FluidState fluid = level.getFluidState(feet);
        if (!ExDeorumCompat.isWitchWater(fluid)) {
            fluid = level.getFluidState(feet.below());
            if (!ExDeorumCompat.isWitchWater(fluid)) {
                return;
            }
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(EntityType.ENDERMITE.create(serverLevel, EntitySpawnReason.MOB_SUMMONED) instanceof Endermite endermite)) {
            silverfish.discard();
            return;
        }
        endermite.setPos(silverfish.getX(), silverfish.getY(), silverfish.getZ());
        endermite.setYRot(silverfish.getYRot());
        endermite.setXRot(silverfish.getXRot());
        serverLevel.addFreshEntity(endermite);
        silverfish.discard();
    }
}
