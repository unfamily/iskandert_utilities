package net.unfamily.iskautils.client.mobreaper;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.MobReaperBlock;
import net.unfamily.iskautils.block.entity.MobReaperBlockEntity;

/** Client-side blade angle tracking for powered mob reapers. */
@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT)
public final class MobReaperClientAnimation {

    private static final Long2FloatMap ANGLES = new Long2FloatOpenHashMap();
    private static final LongOpenHashSet ACTIVE = new LongOpenHashSet();

    private MobReaperClientAnimation() {}

    public static void markActive(BlockPos pos, boolean powered) {
        long key = pos.asLong();
        if (powered) {
            ACTIVE.add(key);
            if (!ANGLES.containsKey(key)) {
                ANGLES.put(key, 0.0f);
            }
        } else {
            ACTIVE.remove(key);
            ANGLES.remove(key);
        }
    }

    public static float getAngleDegrees(BlockPos pos, float partialTick) {
        float angle = ANGLES.getOrDefault(pos.asLong(), 0.0f);
        if (partialTick > 0.0f && ACTIVE.contains(pos.asLong())) {
            angle += (float) Config.reaperBladeMaxDegPerTick * partialTick;
        }
        return angle % 360.0f;
    }

    public static boolean shouldHideStaticRotor(BlockPos pos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.MOB_REAPER.get()) && state.getValue(MobReaperBlock.POWERED);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            ACTIVE.clear();
            ANGLES.clear();
            return;
        }

        float step = (float) Config.reaperBladeMaxDegPerTick;
        LongOpenHashSet stale = new LongOpenHashSet();

        for (long key : ACTIVE) {
            BlockPos pos = BlockPos.of(key);
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.MOB_REAPER.get()) || !state.getValue(MobReaperBlock.POWERED)) {
                stale.add(key);
                continue;
            }
            float angle = ANGLES.getOrDefault(key, 0.0f) + step;
            if (angle >= 360.0f) {
                angle -= 360.0f;
            }
            ANGLES.put(key, angle);
        }

        for (long key : stale) {
            ACTIVE.remove(key);
            ANGLES.remove(key);
        }
    }

    public static void poll(MobReaperBlockEntity blockEntity, float partialTick) {
        BlockState state = blockEntity.getBlockState();
        markActive(blockEntity.getBlockPos(), state.getValue(MobReaperBlock.POWERED));
    }
}
