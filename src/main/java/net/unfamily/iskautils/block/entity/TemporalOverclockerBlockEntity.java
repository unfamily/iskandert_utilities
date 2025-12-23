package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity per il Temporal Overclocker
 * Accelera i tick dei blocchi collegati tramite chipset
 */
public class TemporalOverclockerBlockEntity extends BlockEntity {
    private static final String ENERGY_TAG = "Energy";
    private static final String LINKED_BLOCKS_TAG = "LinkedBlocks";
    
    // Lista delle posizioni dei blocchi collegati
    private final List<BlockPos> linkedBlocks = new ArrayList<>();
    
    // Energy storage
    private final EnergyStorageImpl energyStorage;
    
    // Contatore per gestire l'accelerazione (ogni N tick accelera)
    private int tickCounter = 0;
    private static final int ACCELERATION_INTERVAL = 1; // Accelera ogni tick
    
    public TemporalOverclockerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEMPORAL_OVERCLOCKER_BE.get(), pos, state);
        int maxEnergy = Config.temporalOverclockerEnergyConsume <= 0 ? 0 : Config.temporalOverclockerEnergyBuffer;
        this.energyStorage = new EnergyStorageImpl(maxEnergy);
    }
    
    /**
     * Aggiunge un blocco alla lista dei blocchi collegati
     */
    public boolean addLinkedBlock(BlockPos pos) {
        if (linkedBlocks.size() >= Config.temporalOverclockerMaxLinks) {
            return false; // Limite raggiunto
        }
        
        if (!linkedBlocks.contains(pos)) {
            linkedBlocks.add(pos);
            setChanged();
            return true;
        }
        return false; // Già presente
    }
    
    /**
     * Rimuove un blocco dalla lista dei blocchi collegati
     */
    public boolean removeLinkedBlock(BlockPos pos) {
        boolean removed = linkedBlocks.remove(pos);
        if (removed) {
            setChanged();
        }
        return removed;
    }
    
    /**
     * Ottiene la lista dei blocchi collegati
     */
    public List<BlockPos> getLinkedBlocks() {
        return new ArrayList<>(linkedBlocks);
    }
    
    /**
     * Pulisce tutti i link
     */
    public void clearLinkedBlocks() {
        linkedBlocks.clear();
        setChanged();
    }
    
    /**
     * Verifica se un blocco è collegato
     */
    public boolean isLinked(BlockPos pos) {
        return linkedBlocks.contains(pos);
    }
    
    /**
     * Metodo tick principale
     */
    public static void tick(Level level, BlockPos pos, BlockState state, TemporalOverclockerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        
        blockEntity.tickCounter++;
        
        // Controlla se abbiamo abbastanza energia
        int energyRequired = getEnergyRequired();
        if (energyRequired > 0 && blockEntity.energyStorage.getEnergyStored() < energyRequired) {
            return; // Non abbastanza energia
        }
        
        // Ogni ACCELERATION_INTERVAL tick, accelera i blocchi collegati
        if (blockEntity.tickCounter >= ACCELERATION_INTERVAL) {
            blockEntity.tickCounter = 0;
            
            // Accelera ogni blocco collegato
            if (!blockEntity.linkedBlocks.isEmpty() && level instanceof ServerLevel serverLevel) {
                int accelerationFactor = Config.temporalOverclockerAccelerationFactor;
                
                // Consuma energia per ogni blocco collegato
                int totalEnergyNeeded = energyRequired * blockEntity.linkedBlocks.size();
                if (totalEnergyNeeded > 0 && blockEntity.energyStorage.getEnergyStored() < totalEnergyNeeded) {
                    return; // Non abbastanza energia per tutti i blocchi
                }
                
                // Rimuovi blocchi non validi e accelera quelli validi
                List<BlockPos> toRemove = new ArrayList<>();
                for (BlockPos linkedPos : blockEntity.linkedBlocks) {
                    BlockEntity linkedBE = level.getBlockEntity(linkedPos);
                    if (linkedBE == null) {
                        toRemove.add(linkedPos);
                        continue;
                    }
                    
                    // Accelera il BlockEntity chiamando il suo ticker più volte
                    BlockState linkedState = level.getBlockState(linkedPos);
                    BlockEntityType<?> beType = linkedBE.getType();
                    
                    // Ottieni il ticker dal blocco usando il metodo corretto
                    Block block = linkedState.getBlock();
                    if (block instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                        BlockEntityTicker<?> ticker = entityBlock.getTicker(level, linkedState, beType);
                        if (ticker != null) {
                            // Chiama il ticker più volte per accelerare
                            @SuppressWarnings("unchecked")
                            BlockEntityTicker<BlockEntity> safeTicker = (BlockEntityTicker<BlockEntity>) ticker;
                            for (int i = 0; i < accelerationFactor - 1; i++) {
                                try {
                                    safeTicker.tick(level, linkedPos, linkedState, linkedBE);
                                } catch (Exception e) {
                                    // Se c'è un errore, interrompi l'accelerazione per questo blocco
                                    break;
                                }
                            }
                        }
                    }
                    
                    // Accelera anche i tick del blocco stesso (per blocchi senza BlockEntity ma con tick)
                    for (int i = 0; i < accelerationFactor - 1; i++) {
                        level.scheduleTick(linkedPos, linkedState.getBlock(), 0);
                    }
                }
                
                // Rimuovi blocchi non validi
                for (BlockPos removePos : toRemove) {
                    blockEntity.linkedBlocks.remove(removePos);
                }
                
                if (!toRemove.isEmpty()) {
                    blockEntity.setChanged();
                }
                
                // Consuma energia
                if (totalEnergyNeeded > 0) {
                    blockEntity.energyStorage.extractEnergy(totalEnergyNeeded, false);
                    blockEntity.setChanged();
                }
            }
        }
    }
    
    /**
     * Ottiene l'energia richiesta per operazione
     */
    private static int getEnergyRequired() {
        if (Config.temporalOverclockerEnergyConsume <= 0) {
            return 0;
        }
        if (Config.temporalOverclockerEnergyBuffer <= 0) {
            return 0;
        }
        return Math.min(Config.temporalOverclockerEnergyConsume, Config.temporalOverclockerEnergyBuffer);
    }
    
    /**
     * Ottiene lo storage di energia
     */
    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        
        // Salva i blocchi collegati
        ListTag linkedBlocksTag = new ListTag();
        for (BlockPos linkedPos : linkedBlocks) {
            linkedBlocksTag.add(NbtUtils.writeBlockPos(linkedPos));
        }
        tag.put(LINKED_BLOCKS_TAG, linkedBlocksTag);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(ENERGY_TAG)) {
            this.energyStorage.setEnergy(tag.getInt(ENERGY_TAG));
        }
        
        // Carica i blocchi collegati
        linkedBlocks.clear();
        if (tag.contains(LINKED_BLOCKS_TAG)) {
            ListTag linkedBlocksTag = tag.getList(LINKED_BLOCKS_TAG, 10); // 10 = CompoundTag type
            for (int i = 0; i < linkedBlocksTag.size(); i++) {
                CompoundTag posTag = linkedBlocksTag.getCompound(i);
                BlockPos pos = NbtUtils.readBlockPos(posTag, "pos").orElse(null);
                if (pos != null) {
                    linkedBlocks.add(pos);
                }
            }
        }
    }
    
    /**
     * Custom EnergyStorage implementation
     */
    public static class EnergyStorageImpl extends EnergyStorage {
        public EnergyStorageImpl(int capacity) {
            super(capacity);
        }
        
        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }
}

