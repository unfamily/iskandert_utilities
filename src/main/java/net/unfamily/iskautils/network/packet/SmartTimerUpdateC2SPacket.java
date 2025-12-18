package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.block.entity.SmartTimerBlockEntity;

/**
 * Packet per aggiornare i parametri del timer
 */
public class SmartTimerUpdateC2SPacket {
    
    private final BlockPos pos;
    private final boolean isCooldown;
    private final int deltaTicks;
    
    public SmartTimerUpdateC2SPacket(BlockPos pos, boolean isCooldown, int deltaTicks) {
        this.pos = pos;
        this.isCooldown = isCooldown;
        this.deltaTicks = deltaTicks;
    }
    
    /**
     * Gestisce il packet sul server
     */
    public void handle(ServerPlayer player) {
        if (player == null || player.level() == null) return;
        
        // Ottieni la BlockEntity
        var blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof SmartTimerBlockEntity timer)) return;
        
        // Leggi i valori correnti dal BlockEntity direttamente in tick
        int currentCooldownTicks = timer.getCooldownTicks();
        int currentSignalDurationTicks = timer.getSignalDurationTicks();
        
        // Calcola i nuovi valori applicando il delta
        int minTicks = 5;
        if (isCooldown) {
            int newCooldownTicks = Math.max(minTicks, currentCooldownTicks + deltaTicks);
            timer.setCooldownTicks(newCooldownTicks); // Set directly in ticks
        } else {
            int newSignalDurationTicks = Math.max(minTicks, currentSignalDurationTicks + deltaTicks);
            timer.setSignalDurationTicks(newSignalDurationTicks); // Set directly in ticks
        }
        
        // Marca la BlockEntity come modificata
        timer.setChanged();
        
        // Aggiorna il client
        player.level().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
    }
}
