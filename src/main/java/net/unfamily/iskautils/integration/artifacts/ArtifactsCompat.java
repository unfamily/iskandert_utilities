package net.unfamily.iskautils.integration.artifacts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Optional Artifacts integration without compile-time dependency.
 */
public final class ArtifactsCompat {
    public static final String MOD_ID = "artifacts";
    private static final ResourceLocation MIMIC_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "mimic");
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsCompat.class);

    private ArtifactsCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean spawnDormantMimic(ServerLevel level, BlockPos pos, Direction chestFacing) {
        if (!isLoaded()) {
            return false;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(MIMIC_ID);
        if (type == null) {
            return false;
        }
        Entity entity = type.create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        Direction facing = chestFacing != null ? chestFacing : Direction.NORTH;
        try {
            Method setFacing = entity.getClass().getMethod("setFacing", Direction.class);
            setFacing.invoke(entity, facing);
            Method setDormant = entity.getClass().getMethod("setDormant", boolean.class);
            setDormant.invoke(entity, true);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to configure Artifacts mimic at {}: {}", pos, e.toString());
            return false;
        }
        return level.addFreshEntity(entity);
    }
}
