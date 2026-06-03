package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.util.EntropicSoilUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class EntropicAgglomerationSpreadHandler {
    private static final Map<ResourceKey<Level>, List<SpreadJob>> JOBS = new HashMap<>();

    private EntropicAgglomerationSpreadHandler() {}

    public static boolean enqueue(ServerLevel level, BlockPos origin) {
        List<List<BlockPos>> rings = EntropicSoilUtil.collectAgglomerationSpreadRings(level, origin);
        if (rings.isEmpty()) {
            return false;
        }
        JOBS.computeIfAbsent(level.dimension(), key -> new ArrayList<>())
                .add(new SpreadJob(rings));
        return true;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<SpreadJob> jobs = JOBS.get(level.dimension());
        if (jobs == null || jobs.isEmpty()) {
            return;
        }

        Iterator<SpreadJob> iterator = jobs.iterator();
        while (iterator.hasNext()) {
            SpreadJob job = iterator.next();
            if (job.ticksUntilNext > 0) {
                job.ticksUntilNext--;
                continue;
            }

            List<BlockPos> ring = job.rings.poll();
            if (ring != null) {
                for (BlockPos pos : ring) {
                    if (EntropicSoilUtil.convertToEntropicSoilForAgglomeration(level, pos)) {
                        level.sendParticles(
                                ParticleTypes.WITCH,
                                pos.getX() + 0.5D,
                                pos.getY() + 1.0D,
                                pos.getZ() + 0.5D,
                                4,
                                0.2D,
                                0.12D,
                                0.2D,
                                0.01D);
                    }
                }
            }

            if (job.rings.isEmpty()) {
                iterator.remove();
            } else {
                job.ticksUntilNext = Config.entropicAgglomerationSpreadIntervalTicks;
            }
        }

        if (jobs.isEmpty()) {
            JOBS.remove(level.dimension());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
    }

    private static final class SpreadJob {
        private final ArrayDeque<List<BlockPos>> rings;
        private int ticksUntilNext;

        private SpreadJob(List<List<BlockPos>> rings) {
            this.rings = new ArrayDeque<>(rings);
            this.ticksUntilNext = 0;
        }
    }
}
