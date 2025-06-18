package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;

/**
 * Menu for the Structure Placer Machine block
 */
public class StructurePlacerMachineMenu extends AbstractContainerMenu {
    private final StructurePlacerMachineBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData containerData;
    private final BlockPos blockPos;
    
    // Slot indices
    private static final int MACHINE_SLOTS = 27; // 3 rows x 9 slots
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;
    
    // ContainerData indices for syncing
    private static final int ENERGY_INDEX = 0;
    private static final int MAX_ENERGY_INDEX = 1;
    private static final int SHOW_PREVIEW_INDEX = 2;
    private static final int DATA_COUNT = 3;
    
    // Constructor for server-side (with block entity)
    public StructurePlacerMachineMenu(int containerId, Inventory playerInventory, StructurePlacerMachineBlockEntity blockEntity) {
        super(ModMenuTypes.STRUCTURE_PLACER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Create container data that syncs with the block entity
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case ENERGY_INDEX -> blockEntity.getEnergyStorage().getEnergyStored();
                    case MAX_ENERGY_INDEX -> blockEntity.getEnergyStorage().getMaxEnergyStored();
                    case SHOW_PREVIEW_INDEX -> blockEntity.isShowPreview() ? 1 : 0;
                    default -> 0;
                };
            }
            
            @Override
            public void set(int index, int value) {
                // Values are read-only from client side, handled by server
            }
            
            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        
        this.addDataSlots(this.containerData);
        
        addMachineSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }
    
    // Constructor for client-side (simplified)
    public StructurePlacerMachineMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.STRUCTURE_PLACER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        // Create dummy container data for client
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
        
        addMachineSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }
    
    /**
     * Adds the 27 machine slots (3 rows x 9 slots)
     * Starting at GUI coordinates (7, 51)
     */
    private void addMachineSlots() {
        IItemHandler itemHandler;
        
        if (blockEntity != null) {
            itemHandler = blockEntity.getItemHandler();
        } else {
            // Client-side fallback
            itemHandler = new ItemStackHandler(MACHINE_SLOTS);
        }
        
        // 3 rows of 9 slots each
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                int xPos = 8 + col * 18; // GUI coordinate 7 + 1 (slot padding)
                int yPos = 52 + row * 18; // GUI coordinate 51 + 1 (slot padding)
                
                addSlot(new SlotItemHandler(itemHandler, slotIndex, xPos, yPos));
            }
        }
    }
    
    /**
     * Adds player inventory slots
     * Starting at GUI coordinates (7, 117)
     */
    private void addPlayerInventory(Inventory playerInventory) {
        // 3 rows of 9 slots (inventory)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9; // +9 to skip hotbar
                int xPos = 8 + col * 18; // GUI coordinate 7 + 1
                int yPos = 118 + row * 18; // GUI coordinate 117 + 1
                
                addSlot(new Slot(playerInventory, slotIndex, xPos, yPos));
            }
        }
    }
    
    /**
     * Adds player hotbar slots
     */
    private void addPlayerHotbar(Inventory playerInventory) {
        // 1 row of 9 slots (hotbar) - below inventory
        for (int col = 0; col < 9; col++) {
            int xPos = 8 + col * 18;
            int yPos = 176; // Below inventory (118 + 3*18 = 172, +4 spacing)
            
            addSlot(new Slot(playerInventory, col, xPos, yPos));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            
            if (index < MACHINE_SLOTS) {
                // Moving from machine to player
                if (!this.moveItemStackTo(originalStack, MACHINE_SLOTS, PLAYER_HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player to machine
                if (!this.moveItemStackTo(originalStack, 0, MACHINE_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return newStack;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.STRUCTURE_PLACER_MACHINE.get());
    }
    
    public StructurePlacerMachineBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    // Methods to access synced data (works on both client and server)
    public int getEnergyStored() {
        return this.containerData.get(ENERGY_INDEX);
    }
    
    public int getMaxEnergyStored() {
        return this.containerData.get(MAX_ENERGY_INDEX);
    }
    
    public boolean isShowPreview() {
        return this.containerData.get(SHOW_PREVIEW_INDEX) != 0;
    }
    
    public BlockPos getBlockPos() {
        return this.blockPos;
    }
    
    // Method to get block entity from level (for client-side actions)
    // This searches for the nearest Structure Placer Machine in render distance
    public StructurePlacerMachineBlockEntity getBlockEntityFromLevel(Level level) {
        if (this.blockEntity != null) {
            return this.blockEntity; // Server side
        } else {
            // Client side - find the block entity by searching near player
            if (level.isClientSide && net.minecraft.client.Minecraft.getInstance().player != null) {
                BlockPos playerPos = net.minecraft.client.Minecraft.getInstance().player.blockPosition();
                
                // Search in a 16x16x16 area around player for the machine
                for (int x = -8; x <= 8; x++) {
                    for (int y = -8; y <= 8; y++) {
                        for (int z = -8; z <= 8; z++) {
                            BlockPos searchPos = playerPos.offset(x, y, z);
                            BlockEntity be = level.getBlockEntity(searchPos);
                            if (be instanceof StructurePlacerMachineBlockEntity machineEntity) {
                                return machineEntity;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
} 