package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Block Entity per la macchina Structure Saver
 * Gestisce energia da config e compound tags per il salvataggio strutture
 */
public class StructureSaverMachineBlockEntity extends BlockEntity implements MenuProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSaverMachineBlockEntity.class);
    
    // Energy storage configurabile da Config
    private final EnergyStorage energyStorage = new EnergyStorage(
        net.unfamily.iskautils.Config.structureSaverMachineEnergyBuffer, 
        net.unfamily.iskautils.Config.structureSaverMachineEnergyBuffer, // Max input = capacity
        net.unfamily.iskautils.Config.structureSaverMachineEnergyBuffer  // Max extract = capacity
    );
    
    // Item storage per i 27 slot display
    private final ItemStackHandler itemHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
    };
    
    // Compound tags storage per dati struttura
    private CompoundTag structureData = new CompoundTag();
    
    // Stato operativo
    private boolean isWorking = false;
    private int workProgress = 0;
    
    public StructureSaverMachineBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.STRUCTURE_SAVER_MACHINE_BE.get(), pos, blockState);
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (pkt.getTag() != null) {
            loadAdditional(pkt.getTag(), lookupProvider);
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        // Salva energia
        int currentEnergy = energyStorage.getEnergyStored();
        tag.putInt("energy", currentEnergy);
        LOGGER.debug("Structure Saver Machine saving energy: {}", currentEnergy);
        
        // Salva item handler
        tag.put("inventory", itemHandler.serializeNBT(registries));
        
        // Salva compound tags struttura
        tag.put("structureData", structureData.copy());
        
        // Salva stato operativo
        tag.putBoolean("isWorking", isWorking);
        tag.putInt("workProgress", workProgress);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        // Carica energia
        if (tag.contains("energy")) {
            int savedEnergy = tag.getInt("energy");
            energyStorage.extractEnergy(energyStorage.getEnergyStored(), false);
            energyStorage.receiveEnergy(savedEnergy, false);
            LOGGER.debug("Structure Saver Machine loaded energy: {}", savedEnergy);
        }
        
        // Carica item handler
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        
        // Carica compound tags struttura
        if (tag.contains("structureData")) {
            structureData = tag.getCompound("structureData").copy();
        } else {
            structureData = new CompoundTag();
        }
        
        // Carica stato operativo
        isWorking = tag.getBoolean("isWorking");
        workProgress = tag.getInt("workProgress");
    }
    
    /**
     * Tick method chiamato dal blocco
     */
    public static void tick(Level level, BlockPos pos, BlockState state, StructureSaverMachineBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return; // Solo server side
        }
        
        // Logica di tick per operazioni future
        if (blockEntity.isWorking) {
            blockEntity.workProgress++;
            
            // Esempio di consumo energia durante lavoro
            int energyRequired = net.unfamily.iskautils.Config.structureSaverMachineEnergyConsume;
            if (energyRequired > 0 && blockEntity.energyStorage.getEnergyStored() >= energyRequired) {
                blockEntity.energyStorage.extractEnergy(energyRequired, false);
                blockEntity.setChanged();
            } else if (energyRequired > 0) {
                // Non abbastanza energia, ferma il lavoro
                blockEntity.isWorking = false;
                blockEntity.workProgress = 0;
                blockEntity.setChanged();
            }
        }
    }
    
    /**
     * Rilascia items quando il blocco viene distrutto
     */
    public void drops() {
        if (level != null && !level.isClientSide()) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                if (!itemHandler.getStackInSlot(i).isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), itemHandler.getStackInSlot(i));
                }
            }
        }
    }
    
    // Getters and setters
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
    
    public CompoundTag getStructureData() {
        return structureData.copy();
    }
    
    public void setStructureData(CompoundTag data) {
        this.structureData = data.copy();
        setChanged();
    }
    
    public boolean isWorking() {
        return isWorking;
    }
    
    public void setWorking(boolean working) {
        this.isWorking = working;
        if (!working) {
            this.workProgress = 0;
        }
        setChanged();
    }
    
    public int getWorkProgress() {
        return workProgress;
    }
    
    /**
     * Ottiene l'energia attualmente immagazzinata
     */
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }
    
    /**
     * Ottiene la capacità massima di energia
     */
    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }
    
    // MenuProvider implementation (per la GUI futura)
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.structure_saver_machine");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new net.unfamily.iskautils.client.gui.StructureSaverMachineMenu(containerId, playerInventory, this);
    }
} 