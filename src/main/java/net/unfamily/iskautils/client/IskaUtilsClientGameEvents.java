package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity;

@EventBusSubscriber(modid = IskaUtils.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class IskaUtilsClientGameEvents {
    private IskaUtilsClientGameEvents() {}

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || event.getSound() == null) {
            return;
        }
        SoundInstance sound = event.getOriginalSound();
        if (sound == null) {
            sound = event.getSound();
        }
        if (sound == null || sound.getSource() == SoundSource.MUSIC) {
            return;
        }
        BlockPos soundPos = BlockPos.containing(sound.getX(), sound.getY(), sound.getZ());
        String soundId = sound.getLocation().toString();
        int maxRadius = Config.soundMufflerRangeMax;
        int effectivePercent = 100;
        for (BlockPos pos : BlockPos.betweenClosed(
                soundPos.offset(-maxRadius, -maxRadius, -maxRadius),
                soundPos.offset(maxRadius, maxRadius, maxRadius))) {
            var be = level.getBlockEntity(pos);
            if (be instanceof SoundMufflerBlockEntity muffler) {
                int r = muffler.getRange();
                if (pos.distSqr(soundPos) > (long) r * r) continue;
                if (muffler.hasFilter() && !muffler.isSoundAllowedByFilter(soundId)) continue;
                int p = muffler.getEffectiveVolumeFor(sound.getSource(), sound.getLocation());
                if (p < effectivePercent) effectivePercent = p;
            }
        }
        if (effectivePercent <= 0) {
            event.setSound(null);
            return;
        }
        if (effectivePercent < 100) {
            event.setSound(new SoundMufflerVolumeScaledSound(sound, effectivePercent / 100f));
        }
    }
}
