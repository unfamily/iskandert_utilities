package net.unfamily.iskautils.client;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.unfamily.iskautils.network.packet.FanShowAreaC2SPacket;
import net.unfamily.iskautils.network.packet.StructurePlacerMachineTogglePreviewC2SPacket;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity;
import net.unfamily.iskalib.structure.StructureLoader;

@EventBusSubscriber(value = Dist.CLIENT, modid = IskaUtils.MOD_ID)
public final class IskaUtilsClientGameEvents {
    private static final ModLogger LOGGER = ModLogger.of(IskaUtilsClientGameEvents.class);

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
        if (sound.getSource() == SoundSource.MUSIC) {
            return;
        }
        BlockPos soundPos = BlockPos.containing(sound.getX(), sound.getY(), sound.getZ());
        String soundId = sound.getIdentifier().toString();
        int maxRadius = Config.soundMufflerRangeMax;
        int effectivePercent = 100;
        for (BlockPos pos : BlockPos.betweenClosed(
                soundPos.offset(-maxRadius, -maxRadius, -maxRadius),
                soundPos.offset(maxRadius, maxRadius, maxRadius))) {
            var be = level.getBlockEntity(pos);
            if (be instanceof SoundMufflerBlockEntity muffler) {
                int r = muffler.getRange();
                if (pos.distSqr(soundPos) > (long) r * r) continue;
                if (!muffler.shouldMuffleSound(soundId)) continue;
                int p = muffler.getEffectiveVolumeFor(sound.getSource(), sound.getIdentifier());
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        MachinePreviewTracker.tickPeriodicReconcile(mc.level);
        for (BlockPos ownerPos : MachinePreviewTracker.pollOwnersNeedingWorldRefresh(mc.level)) {
            MachinePreviewTracker.onFootprintRefreshRequested(mc.level, ownerPos);
            var be = mc.level.getBlockEntity(ownerPos);
            if (be instanceof FanBlockEntity fan && fan.isShowAreaEnabled()) {
                ClientPacketDistributor.sendToServer(new FanShowAreaC2SPacket(ownerPos, true));
            } else if (be instanceof StructurePlacerMachineBlockEntity machine && machine.isShowPreview()) {
                ClientPacketDistributor.sendToServer(new StructurePlacerMachineTogglePreviewC2SPacket(ownerPos, true));
            }
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            MachinePreviewTracker.clearAll();
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof Level level) {
            MachinePreviewTracker.onBlockInPreviewChanged(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level) {
            MachinePreviewTracker.onBlockInPreviewChanged(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }
        try {
            LOGGER.info("Local player joined world, reloading client structures...");
            StructureLoader.reloadAllDefinitions(true);
        } catch (Exception e) {
            LOGGER.error("Error reloading client structures on player join: {}", e.getMessage());
        }
    }
}
