package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.client.gui.StructurePlacerMachineMenu;

/**
 * Block Entity for the Structure Placer Machine
 */
public class StructurePlacerMachineBlockEntity extends BlockEntity implements MenuProvider {
    
    private final ItemStackHandler itemHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
    // Energy storage for the machine (10,000 FE capacity, 100 FE/t input)
    private final EnergyStorage energyStorage = new EnergyStorage(10000, 100, 0);
    
    // Selected structure for this machine
    private String selectedStructure = "";
    
    // Preview mode toggle
    private boolean showPreview = false;
    
    // Structure rotation (0, 90, 180, 270 degrees)
    private int rotation = 0;
    
    // Redstone mode (0 = NONE, 1 = LOW, 2 = HIGH)
    private int redstoneMode = 0;
    
    /**
     * Enum for redstone modes
     */
    public enum RedstoneMode {
        NONE(0),    // Gunpowder icon
        LOW(1),     // Redstone dust icon  
        HIGH(2);    // Redstone gui icon
        
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
                case HIGH -> NONE;
            };
        }
    }
    
    public StructurePlacerMachineBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.STRUCTURE_PLACER_MACHINE_BE.get(), pos, blockState);
        
        // Debug: Start with some energy for testing the energy bar
        this.energyStorage.receiveEnergy(5000, false); // Start with 5000/10000 FE (50%)
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
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, net.minecraft.core.HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (pkt.getTag() != null) {
            loadAdditional(pkt.getTag(), lookupProvider);
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putString("selectedStructure", selectedStructure);
        tag.putBoolean("showPreview", showPreview);
        tag.putInt("rotation", rotation);
        tag.putInt("redstoneMode", redstoneMode);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("energy")) {
            energyStorage.deserializeNBT(registries, tag.get("energy"));
        }
        selectedStructure = tag.getString("selectedStructure");
        showPreview = tag.getBoolean("showPreview");
        rotation = tag.getInt("rotation");
        redstoneMode = tag.getInt("redstoneMode");
    }
    
    /**
     * Drops all items when the block is broken
     */
    public void drops() {
        if (level != null) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 
                                       itemHandler.getStackInSlot(i));
            }
        }
    }
    
    // Getters and setters
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
    
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    public String getSelectedStructure() {
        return selectedStructure;
    }
    
    public void setSelectedStructure(String selectedStructure) {
        this.selectedStructure = selectedStructure;
        setChanged();
    }
    
    public boolean isShowPreview() {
        return showPreview;
    }
    
    public void setShowPreview(boolean showPreview) {
        this.showPreview = showPreview;
        setChanged();
    }
    
    public int getRotation() {
        return rotation;
    }
    
    public void setRotation(int rotation) {
        this.rotation = rotation % 360; // Ensure rotation is always 0-359
        setChanged();
    }
    
    public int getRedstoneMode() {
        return redstoneMode;
    }
    
    public void setRedstoneMode(int redstoneMode) {
        this.redstoneMode = redstoneMode % 3; // Ensure mode is always 0-2
        setChanged();
    }
    
    // Static tick method for server-side updates
    public static void tick(Level level, BlockPos pos, BlockState state, StructurePlacerMachineBlockEntity blockEntity) {
        if (level.isClientSide()) return;
        
        // TODO: Add machine logic here (auto-placement, etc.)
        
        // For testing: slowly drain energy (1 FE per second)
        if (level.getGameTime() % 20 == 0) { // Every second
            if (blockEntity.energyStorage.getEnergyStored() > 0) {
                blockEntity.energyStorage.extractEnergy(1, false);
                blockEntity.setChanged();
            }
        }
    }
    
    /**
     * Debug method to add energy for testing
     */
    public void addEnergyForTesting(int amount) {
        energyStorage.receiveEnergy(amount, false);
        setChanged();
    }
    
    // MenuProvider implementation
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.structure_placer_machine");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StructurePlacerMachineMenu(containerId, playerInventory, this);
    }
} 