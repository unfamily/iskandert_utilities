package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;
import net.unfamily.iskautils.world.BlazingAltarSpatialIndex;

import java.util.Set;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class BlazingAltarSpawnHandler {

    private BlazingAltarSpawnHandler() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Mob mob = event.getEntity();
        MobCategory category = mob.getType().getCategory();
        BlockPos spawnPos = mob.blockPosition();
        ChunkPos chunkPos = new ChunkPos(spawnPos);

        Set<BlockPos> altars = BlazingAltarSpatialIndex.getAltarsInChunk(level.dimension(), chunkPos);
        if (altars.isEmpty()) {
            return;
        }

        for (BlockPos altarPos : altars) {
            if (!(level.getBlockEntity(altarPos) instanceof BlazingAltarBlockEntity altar)) {
                continue;
            }
            if (!BlazingAltarSpatialIndex.isWithinChunkRadius(altarPos, spawnPos, altar.getChunkRadius())) {
                continue;
            }
            if (altar.blocksNaturalSpawn(category)) {
                event.setSpawnCancelled(true);
                return;
            }
        }
    }
}
