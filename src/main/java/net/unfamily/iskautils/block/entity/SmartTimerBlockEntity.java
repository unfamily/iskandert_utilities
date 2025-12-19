package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.unfamily.iskautils.block.SmartTimerBlock;
import net.unfamily.iskautils.client.model.SmartTimerModelData;

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
    
    // Redstone mode (preparato per uso futuro)
    private int redstoneMode = 0;
    
    // I/O Configuration: byte array per le 6 facce (indice = Direction.ordinal())
    // 0 = BLANK, 1 = INPUT, 2 = OUTPUT
    private byte[] ioConfig = new byte[6]; // Inizializzato a tutti 0 (BLANK) di default
    
    // Tracking per PULSE mode (stato precedente degli input)
    private boolean[] previousInputStates = new boolean[6];
    
    /**
     * Enum for I/O types
     */
    public enum IoType {
        BLANK(0),
        INPUT(1),
        OUTPUT(2);
        
        private final byte value;
        
        IoType(int value) {
            this.value = (byte) value;
        }
        
        public byte getValue() {
            return value;
        }
        
        public static IoType fromValue(byte value) {
            for (IoType type : values()) {
                if (type.value == value) return type;
            }
            return BLANK;
        }
        
        public IoType next() {
            return switch (this) {
                case BLANK -> INPUT;
                case INPUT -> OUTPUT;
                case OUTPUT -> BLANK;
            };
        }
    }
    
    /**
     * Enum for redstone modes
     */
    public enum RedstoneMode {
        NONE(0),    // Gunpowder icon
        LOW(1),     // Redstone dust icon  
        HIGH(2),    // Redstone gui icon
        PULSE(3);   // Repeater icon
        
        private final int value;
        
        RedstoneMode(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static RedstoneMode fromValue(int value) {
            for (RedstoneMode mode : values()) {
                if (mode.value == value) return mode;
            }
            return NONE;
        }
        
        public RedstoneMode next() {
            return switch (this) {
                case NONE -> LOW;
                case LOW -> HIGH;
                case HIGH -> PULSE;
                case PULSE -> NONE;
            };
        }
    }
    
    public SmartTimerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_TIMER_BE.get(), pos, state);
        // Inizializza tutte le facce a BLANK (già fatto dal default del byte array)
    }
    
    /**
     * Metodo tick chiamato ogni frame
     */
    public static void tick(Level level, BlockPos pos, BlockState state, SmartTimerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return; // Solo lato server
        }
        
        // Controlla se il timer deve essere bloccato dagli input redstone
        boolean shouldBlock = blockEntity.shouldBlockTimer(level, pos, state);
        
        if (shouldBlock) {
            // Blocca il timer: azzera contatore, spegni segnale se attivo
            if (blockEntity.isSignalActive) {
                blockEntity.isSignalActive = false;
                BlockState newState = state.setValue(SmartTimerBlock.POWERED, false);
                level.setBlock(pos, newState, 3);
                level.updateNeighborsAt(pos, state.getBlock());
            }
            blockEntity.currentTick = 0;
            // NON incrementare currentTick quando bloccato (timer si ferma)
            return;
        }
        
        // Timer non bloccato, procedi normalmente
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
     * Controlla se il timer deve essere bloccato in base agli input redstone configurati
     */
    private boolean shouldBlockTimer(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(SmartTimerBlock.FACING);
        RedstoneMode mode = RedstoneMode.fromValue(redstoneMode);
        
        // NONE mode: non bloccare mai
        if (mode == RedstoneMode.NONE) {
            return false;
        }
        
        // Raccogli tutti gli input e i loro stati
        boolean[] currentInputStates = new boolean[6];
        boolean hasAnyInput = false;
        
        for (Direction dir : Direction.values()) {
            // Salta il front (non configurabile)
            if (dir == facing) {
                continue;
            }
            
            int dirIndex = dir.ordinal();
            IoType ioType = IoType.fromValue(ioConfig[dirIndex]);
            
            if (ioType == IoType.INPUT) {
                hasAnyInput = true;
                // Controlla se c'è segnale redstone da questa direzione
                BlockPos neighborPos = pos.relative(dir);
                int signal = level.getSignal(neighborPos, dir.getOpposite());
                currentInputStates[dirIndex] = signal > 0;
            }
        }
        
        // Se non ci sono input configurati, non bloccare
        if (!hasAnyInput) {
            return false;
        }
        
        // Valuta in base al redstone mode
        boolean shouldBlock = false;
        
        switch (mode) {
            case LOW -> {
                // LOW: blocca se almeno un INPUT ha segnale > 0
                for (Direction dir : Direction.values()) {
                    if (dir == facing) continue;
                    int dirIndex = dir.ordinal();
                    if (IoType.fromValue(ioConfig[dirIndex]) == IoType.INPUT && currentInputStates[dirIndex]) {
                        shouldBlock = true;
                        break;
                    }
                }
            }
            case HIGH -> {
                // HIGH: blocca se almeno un INPUT NON ha segnale (tutti gli INPUT devono avere segnale per non bloccare)
                for (Direction dir : Direction.values()) {
                    if (dir == facing) continue;
                    int dirIndex = dir.ordinal();
                    if (IoType.fromValue(ioConfig[dirIndex]) == IoType.INPUT && !currentInputStates[dirIndex]) {
                        shouldBlock = true;
                        break;
                    }
                }
            }
            case PULSE -> {
                // PULSE: blocca su transizione LOW->HIGH (almeno un INPUT passa da false a true)
                for (Direction dir : Direction.values()) {
                    if (dir == facing) continue;
                    int dirIndex = dir.ordinal();
                    if (IoType.fromValue(ioConfig[dirIndex]) == IoType.INPUT) {
                        if (currentInputStates[dirIndex] && !previousInputStates[dirIndex]) {
                            shouldBlock = true;
                            break;
                        }
                    }
                }
            }
            default -> shouldBlock = false;
        }
        
        // Aggiorna previousInputStates per PULSE mode
        System.arraycopy(currentInputStates, 0, previousInputStates, 0, 6);
        
        return shouldBlock;
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
    
    /**
     * Ottiene il redstone mode
     */
    public int getRedstoneMode() {
        return redstoneMode;
    }
    
    /**
     * Imposta il redstone mode
     */
    public void setRedstoneMode(int redstoneMode) {
        this.redstoneMode = redstoneMode % 4; // Ensure mode is always 0-3
        setChanged();
    }
    
    /**
     * Ottiene il tipo I/O per una direzione specifica
     */
    public byte getIoConfig(Direction direction) {
        return ioConfig[direction.ordinal()];
    }
    
    /**
     * Imposta il tipo I/O per una direzione specifica
     */
    public void setIoConfig(Direction direction, byte ioType) {
        ioConfig[direction.ordinal()] = ioType;
        setChanged();
        if (this.level != null) {
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            } else {
                // Client-side: richiedi aggiornamento del ModelData
                requestModelDataUpdate();
            }
        }
    }
    
    /**
     * Cicla il tipo I/O per una direzione (BLANK -> INPUT -> OUTPUT -> BLANK)
     */
    public void cycleIoConfig(Direction direction) {
        int dirIndex = direction.ordinal();
        IoType currentType = IoType.fromValue(ioConfig[dirIndex]);
        IoType nextType = currentType.next();
        ioConfig[dirIndex] = nextType.getValue();
        setChanged();
        if (this.level != null) {
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            } else {
                // Client-side: richiedi aggiornamento del ModelData
                requestModelDataUpdate();
            }
        }
    }
    
    /**
     * Resetta tutte le facce a BLANK
     */
    public void resetAllIoConfig() {
        for (int i = 0; i < 6; i++) {
            ioConfig[i] = IoType.BLANK.getValue();
        }
        setChanged();
        if (this.level != null) {
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            } else {
                // Client-side: richiedi aggiornamento del ModelData
                requestModelDataUpdate();
            }
        }
    }
    
    /**
     * Ottiene l'array completo della configurazione I/O (per sincronizzazione)
     */
    public byte[] getIoConfigArray() {
        return ioConfig.clone(); // Ritorna una copia per sicurezza
    }
    
    /**
     * Imposta l'array completo della configurazione I/O (per sincronizzazione)
     */
    public void setIoConfigArray(byte[] config) {
        if (config != null && config.length == 6) {
            System.arraycopy(config, 0, ioConfig, 0, 6);
            setChanged();
            if (this.level != null) {
                if (!this.level.isClientSide) {
                    this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
                } else {
                    // Client-side: richiedi aggiornamento del ModelData
                    requestModelDataUpdate();
                }
            }
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("SignalDurationTicks", signalDurationTicks);
        tag.putInt("CurrentTick", currentTick);
        tag.putBoolean("IsSignalActive", isSignalActive);
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putByteArray("IoConfig", ioConfig);
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
        if (tag.contains("RedstoneMode")) {
            redstoneMode = tag.getInt("RedstoneMode");
        }
        if (tag.contains("IoConfig")) {
            byte[] loadedConfig = tag.getByteArray("IoConfig");
            if (loadedConfig.length == 6) {
                System.arraycopy(loadedConfig, 0, ioConfig, 0, 6);
            }
        }
    }
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        // Include le configurazioni I/O nel packet di sincronizzazione
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("SignalDurationTicks", signalDurationTicks);
        tag.putByteArray("IoConfig", ioConfig); // Sincronizza anche le configurazioni I/O
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
            if (tag.contains("IoConfig")) {
                byte[] loadedConfig = tag.getByteArray("IoConfig");
                if (loadedConfig.length == 6) {
                    System.arraycopy(loadedConfig, 0, ioConfig, 0, 6);
                    // Richiedi aggiornamento del ModelData quando i dati I/O cambiano
                    requestModelDataUpdate();
                }
            }
        }
    }
    
    /**
     * Fornisce i dati I/O al modello per il rendering dinamico delle texture
     */
    @Override
    public ModelData getModelData() {
        ModelData.Builder builder = ModelData.builder();
        
        // Copia la configurazione I/O
        byte[] ioConfigCopy = new byte[6];
        System.arraycopy(ioConfig, 0, ioConfigCopy, 0, 6);
        builder.with(SmartTimerModelData.IO_CONFIG_PROPERTY, ioConfigCopy);
        
        // Determina lo stato degli input (se hanno segnale redstone)
        boolean[] inputStates = new boolean[6];
        if (this.level != null && this.getBlockPos() != null) {
            BlockState state = this.getBlockState();
            Direction facing = state.getValue(SmartTimerBlock.FACING);
            
            for (Direction dir : Direction.values()) {
                if (dir == facing) {
                    continue; // Salta il front
                }
                
                if (ioConfig[dir.ordinal()] == 1) { // INPUT
                    // Controlla se c'è segnale redstone da questa direzione
                    int signal = this.level.getSignal(
                        this.getBlockPos().relative(dir),
                        dir.getOpposite()
                    );
                    inputStates[dir.ordinal()] = signal > 0;
                }
            }
        }
        builder.with(SmartTimerModelData.INPUT_STATES_PROPERTY, inputStates);
        
        return builder.build();
    }
}
