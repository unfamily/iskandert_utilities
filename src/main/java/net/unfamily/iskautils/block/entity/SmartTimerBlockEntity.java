package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.SmartTimerBlock;

/**
 * Smart Timer Block Entity
 * Gestisce un timer che ogni X secondi emette un segnale redstone di durata Y secondi
 * Default: 5s di cooldown, 3s di durata del segnale
 */
public class SmartTimerBlockEntity extends BlockEntity {
    // Valori di default (in tick: 1 secondo = 20 tick)
    private static final int DEFAULT_COOLDOWN_TICKS = 5 * 20; // 5 secondi
    private static final int DEFAULT_SIGNAL_DURATION_TICKS = 3 * 20; // 3 secondi
    
    // Timer attuali (in tick)
    private int cooldownTicks = DEFAULT_COOLDOWN_TICKS;
    private int signalDurationTicks = DEFAULT_SIGNAL_DURATION_TICKS;
    
    // Stato del timer
    private int currentTick = 0; // Contatore tick corrente
    private boolean isSignalActive = false; // Se il segnale è attualmente attivo
    
    public SmartTimerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_TIMER_BE.get(), pos, state);
    }
    
    /**
     * Metodo tick chiamato ogni frame
     */
    public static void tick(Level level, BlockPos pos, BlockState state, SmartTimerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return; // Solo lato server
        }
        
        blockEntity.currentTick++;
        
        if (blockEntity.isSignalActive) {
            // Il segnale è attivo, controlla se deve spegnersi
            if (blockEntity.currentTick >= blockEntity.signalDurationTicks) {
                // Il segnale è durato abbastanza, spegnilo
                blockEntity.isSignalActive = false;
                blockEntity.currentTick = 0;
                
                // Aggiorna lo stato del blocco
                level.setBlock(pos, state.setValue(SmartTimerBlock.POWERED, false), 3);
                level.updateNeighborsAt(pos, state.getBlock());
            }
        } else {
            // Il segnale non è attivo, controlla se deve attivarsi
            if (blockEntity.currentTick >= blockEntity.cooldownTicks) {
                // Il cooldown è scaduto, attiva il segnale
                blockEntity.isSignalActive = true;
                blockEntity.currentTick = 0;
                
                // Aggiorna lo stato del blocco
                level.setBlock(pos, state.setValue(SmartTimerBlock.POWERED, true), 3);
                level.updateNeighborsAt(pos, state.getBlock());
            }
        }
    }
    
    /**
     * Imposta il cooldown in secondi
     */
    public void setCooldownSeconds(int seconds) {
        this.cooldownTicks = seconds * 20;
        this.currentTick = 0; // Reset del timer
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * Imposta il cooldown direttamente in tick
     */
    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = Math.max(5, ticks); // Minimum 5 ticks
        this.currentTick = 0; // Reset del timer
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * Imposta la durata del segnale in secondi
     */
    public void setSignalDurationSeconds(int seconds) {
        this.signalDurationTicks = seconds * 20;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * Imposta la durata del segnale direttamente in tick
     */
    public void setSignalDurationTicks(int ticks) {
        this.signalDurationTicks = Math.max(5, ticks); // Minimum 5 ticks
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * Ottiene il cooldown in secondi
     */
    public int getCooldownSeconds() {
        return cooldownTicks / 20;
    }
    
    /**
     * Ottiene il cooldown direttamente in tick
     */
    public int getCooldownTicks() {
        return cooldownTicks;
    }
    
    /**
     * Ottiene la durata del segnale in secondi
     */
    public int getSignalDurationSeconds() {
        return signalDurationTicks / 20;
    }
    
    /**
     * Ottiene la durata del segnale direttamente in tick
     */
    public int getSignalDurationTicks() {
        return signalDurationTicks;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("SignalDurationTicks", signalDurationTicks);
        tag.putInt("CurrentTick", currentTick);
        tag.putBoolean("IsSignalActive", isSignalActive);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("CooldownTicks")) {
            cooldownTicks = tag.getInt("CooldownTicks");
        }
        if (tag.contains("SignalDurationTicks")) {
            signalDurationTicks = tag.getInt("SignalDurationTicks");
        }
        if (tag.contains("CurrentTick")) {
            currentTick = tag.getInt("CurrentTick");
        }
        if (tag.contains("IsSignalActive")) {
            isSignalActive = tag.getBoolean("IsSignalActive");
        }
    }
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("SignalDurationTicks", signalDurationTicks);
        return tag;
    }
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, 
                            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                            net.minecraft.core.HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        if (pkt.getTag() != null) {
            CompoundTag tag = pkt.getTag();
            if (tag.contains("CooldownTicks")) {
                cooldownTicks = tag.getInt("CooldownTicks");
            }
            if (tag.contains("SignalDurationTicks")) {
                signalDurationTicks = tag.getInt("SignalDurationTicks");
            }
        }
    }
}
