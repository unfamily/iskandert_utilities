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
    
    // Coordinate importate dalla blueprint
    private net.minecraft.core.BlockPos blueprintVertex1;
    private net.minecraft.core.BlockPos blueprintVertex2;
    private net.minecraft.core.BlockPos blueprintCenter;
    
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
        CompoundTag tag = super.getUpdateTag(registries);
        
        // Aggiungi esplicitamente i dati blueprint per la sincronizzazione
        if (blueprintVertex1 != null) {
            tag.putInt("blueprintVertex1X", blueprintVertex1.getX());
            tag.putInt("blueprintVertex1Y", blueprintVertex1.getY());
            tag.putInt("blueprintVertex1Z", blueprintVertex1.getZ());
        }
        if (blueprintVertex2 != null) {
            tag.putInt("blueprintVertex2X", blueprintVertex2.getX());
            tag.putInt("blueprintVertex2Y", blueprintVertex2.getY());
            tag.putInt("blueprintVertex2Z", blueprintVertex2.getZ());
        }
        if (blueprintCenter != null) {
            tag.putInt("blueprintCenterX", blueprintCenter.getX());
            tag.putInt("blueprintCenterY", blueprintCenter.getY());
            tag.putInt("blueprintCenterZ", blueprintCenter.getZ());
        }
        
        System.out.println("DEBUG BE: getUpdateTag called, including blueprint data");
        System.out.println("DEBUG BE: tag contains blueprintVertex1X = " + tag.contains("blueprintVertex1X"));
        return tag;
    }
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (pkt.getTag() != null) {
            System.out.println("DEBUG BE: Received data packet on client");
            loadAdditional(pkt.getTag(), lookupProvider);
            System.out.println("DEBUG BE: After packet load - hasValidArea = " + hasValidArea());
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        // Salva item handler
        tag.put("inventory", itemHandler.serializeNBT(registries));
        
        // Salva compound tags struttura
        tag.put("structureData", structureData.copy());
        
        // Salva stato operativo
        tag.putBoolean("isWorking", isWorking);
        tag.putInt("workProgress", workProgress);
        
        // Salva dati blueprint
        if (blueprintVertex1 != null) {
            tag.putInt("blueprintVertex1X", blueprintVertex1.getX());
            tag.putInt("blueprintVertex1Y", blueprintVertex1.getY());
            tag.putInt("blueprintVertex1Z", blueprintVertex1.getZ());
        }
        if (blueprintVertex2 != null) {
            tag.putInt("blueprintVertex2X", blueprintVertex2.getX());
            tag.putInt("blueprintVertex2Y", blueprintVertex2.getY());
            tag.putInt("blueprintVertex2Z", blueprintVertex2.getZ());
        }
        if (blueprintCenter != null) {
            tag.putInt("blueprintCenterX", blueprintCenter.getX());
            tag.putInt("blueprintCenterY", blueprintCenter.getY());
            tag.putInt("blueprintCenterZ", blueprintCenter.getZ());
        }
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
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
        
        // Carica dati blueprint
        if (tag.contains("blueprintVertex1X")) {
            int x1 = tag.getInt("blueprintVertex1X");
            int y1 = tag.getInt("blueprintVertex1Y");
            int z1 = tag.getInt("blueprintVertex1Z");
            blueprintVertex1 = new BlockPos(x1, y1, z1);
            System.out.println("DEBUG BE LOAD: vertex1 loaded = " + blueprintVertex1);
        }
        if (tag.contains("blueprintVertex2X")) {
            int x2 = tag.getInt("blueprintVertex2X");
            int y2 = tag.getInt("blueprintVertex2Y");
            int z2 = tag.getInt("blueprintVertex2Z");
            blueprintVertex2 = new BlockPos(x2, y2, z2);
            System.out.println("DEBUG BE LOAD: vertex2 loaded = " + blueprintVertex2);
        }
        if (tag.contains("blueprintCenterX")) {
            int x3 = tag.getInt("blueprintCenterX");
            int y3 = tag.getInt("blueprintCenterY");
            int z3 = tag.getInt("blueprintCenterZ");
            blueprintCenter = new BlockPos(x3, y3, z3);
            System.out.println("DEBUG BE LOAD: center loaded = " + blueprintCenter);
        }
        
        System.out.println("DEBUG BE LOAD: After loading - hasValidArea = " + hasValidArea());
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
            
            // Lavoro semplificato senza energia
            if (blockEntity.workProgress >= 100) { // Completa dopo 5 secondi (100 tick)
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
     * Imposta i dati della blueprint importata
     */
    public void setBlueprintData(net.minecraft.core.BlockPos vertex1, net.minecraft.core.BlockPos vertex2, net.minecraft.core.BlockPos center) {
        this.blueprintVertex1 = vertex1;
        this.blueprintVertex2 = vertex2;
        this.blueprintCenter = center;
        
        // Debug logging per verificare che i dati siano salvati
        System.out.println("DEBUG BE: setBlueprintData called");
        System.out.println("DEBUG BE: vertex1 set to = " + this.blueprintVertex1);
        System.out.println("DEBUG BE: vertex2 set to = " + this.blueprintVertex2);
        System.out.println("DEBUG BE: center set to = " + this.blueprintCenter);
        System.out.println("DEBUG BE: hasValidArea = " + hasValidArea());
        
        setChanged(); // Salva i dati
        
        // Forza la sincronizzazione standard del BlockEntity
        if (level != null && !level.isClientSide()) {
            System.out.println("DEBUG BE: Forcing block update for sync");
            
            // Metodo 1: Invia update packet standard
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            
            // Metodo 2: Packet custom (backup)
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                for (var player : serverLevel.players()) {
                    if (player.distanceToSqr(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()) < 64 * 64) {
                        // Invia il data packet standard del BlockEntity
                        var packet = getUpdatePacket();
                        if (packet != null) {
                            ((net.minecraft.server.level.ServerPlayer) player).connection.send(packet);
                            System.out.println("DEBUG BE: Sent standard update packet to player " + player.getName().getString());
                        }
                        
                                                 // Backup: packet custom via ModMessages (sistema semplificato)
                        net.unfamily.iskautils.network.ModMessages.sendStructureSaverBlueprintSyncPacket(
                            (net.minecraft.server.level.ServerPlayer) player, getBlockPos(), blueprintVertex1, blueprintVertex2, blueprintCenter
                        );
                    }
                }
            }
            
            System.out.println("DEBUG BE: Sent both standard and custom sync packets");
        }
    }
    
    /**
     * Imposta i dati blueprint lato client (chiamato dal packet)
     */
    public void setBlueprintDataClientSide(net.minecraft.core.BlockPos vertex1, net.minecraft.core.BlockPos vertex2, net.minecraft.core.BlockPos center) {
        this.blueprintVertex1 = vertex1;
        this.blueprintVertex2 = vertex2;
        this.blueprintCenter = center;
        
        System.out.println("DEBUG BE CLIENT: Blueprint data updated from packet");
        System.out.println("DEBUG BE CLIENT: vertex1 = " + this.blueprintVertex1);
        System.out.println("DEBUG BE CLIENT: vertex2 = " + this.blueprintVertex2);
        System.out.println("DEBUG BE CLIENT: center = " + this.blueprintCenter);
        System.out.println("DEBUG BE CLIENT: hasValidArea = " + hasValidArea());
    }
    
    /**
     * Ottiene il primo vertice della blueprint
     */
    public net.minecraft.core.BlockPos getBlueprintVertex1() {
        return blueprintVertex1;
    }
    
    /**
     * Ottiene il secondo vertice della blueprint
     */
    public net.minecraft.core.BlockPos getBlueprintVertex2() {
        return blueprintVertex2;
    }
    
    /**
     * Ottiene il centro della blueprint
     */
    public net.minecraft.core.BlockPos getBlueprintCenter() {
        return blueprintCenter;
    }
    
    /**
     * Verifica se ha dati blueprint validi (tutti e 3 i punti)
     */
    public boolean hasBlueprintData() {
        return blueprintVertex1 != null && blueprintVertex2 != null && blueprintCenter != null;
    }
    
    /**
     * Verifica se ha almeno i primi 2 vertici per calcoli area
     */
    public boolean hasValidArea() {
        return blueprintVertex1 != null && blueprintVertex2 != null;
    }
    
    /**
     * Cancella i dati blueprint
     */
    public void clearBlueprintData() {
        this.blueprintVertex1 = null;
        this.blueprintVertex2 = null;
        this.blueprintCenter = null;
        setChanged();
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