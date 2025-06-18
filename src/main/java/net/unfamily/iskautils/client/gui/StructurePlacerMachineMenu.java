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
    private static final int ROTATION_INDEX = 3;
    private static final int STRUCTURE_HASH_INDEX = 4; // Hash of selected structure string
    private static final int BLOCK_POS_X_INDEX = 5;
    private static final int BLOCK_POS_Y_INDEX = 6;
    private static final int BLOCK_POS_Z_INDEX = 7;
    private static final int REDSTONE_MODE_INDEX = 8;
    private static final int DATA_COUNT = 9;
    
    // Store the structure ID for client-side access (synced separately)
    private String cachedSelectedStructure = "";
    private int lastSyncedStructureHash = 0;
    
    // Constructor for server-side (with block entity)
    public StructurePlacerMachineMenu(int containerId, Inventory playerInventory, StructurePlacerMachineBlockEntity blockEntity) {
        super(ModMenuTypes.STRUCTURE_PLACER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Initialize cached structure on server side
        if (blockEntity != null) {
            this.cachedSelectedStructure = blockEntity.getSelectedStructure();
            this.lastSyncedStructureHash = this.cachedSelectedStructure.hashCode();
        }
        
        // Create container data that syncs with the block entity
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case ENERGY_INDEX -> blockEntity.getEnergyStorage().getEnergyStored();
                    case MAX_ENERGY_INDEX -> blockEntity.getEnergyStorage().getMaxEnergyStored();
                    case SHOW_PREVIEW_INDEX -> blockEntity.isShowPreview() ? 1 : 0;
                    case ROTATION_INDEX -> blockEntity.getRotation();
                    case STRUCTURE_HASH_INDEX -> blockEntity.getSelectedStructure().hashCode();
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
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
        this(containerId, playerInventory, BlockPos.ZERO);
    }
    
    // Constructor for client-side with position (for proper packet handling)
    public StructurePlacerMachineMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.STRUCTURE_PLACER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = null;
        this.blockPos = pos; // Use the provided position instead of ZERO
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
     * Starting at GUI coordinates (7, 99) - moved down 48px for increased button area
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
                int yPos = 100 + row * 18; // GUI coordinate 99 + 1 (slot padding) - moved down 48px
                
                addSlot(new SlotItemHandler(itemHandler, slotIndex, xPos, yPos));
            }
        }
    }
    
    /**
     * Adds player inventory slots
     * Starting at GUI coordinates (7, 165) - moved down 48px for increased button area
     */
    private void addPlayerInventory(Inventory playerInventory) {
        // 3 rows of 9 slots (inventory)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9; // +9 to skip hotbar
                int xPos = 8 + col * 18; // GUI coordinate 7 + 1
                int yPos = 166 + row * 18; // GUI coordinate 165 + 1 - moved down 48px
                
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
            int yPos = 224; // Below inventory (166 + 3*18 = 220, +4 spacing) - moved down 48px
            
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
    
    public int getRotation() {
        return this.containerData.get(ROTATION_INDEX);
    }
    
    public int getStructureHash() {
        return this.containerData.get(STRUCTURE_HASH_INDEX);
    }
    
    public BlockPos getSyncedBlockPos() {
        // Get position from synced data if available, otherwise use stored position
        if (this.blockEntity != null) {
            return this.blockPos; // Server side
        } else {
            // Client side - get from synced data
            int x = this.containerData.get(BLOCK_POS_X_INDEX);
            int y = this.containerData.get(BLOCK_POS_Y_INDEX);
            int z = this.containerData.get(BLOCK_POS_Z_INDEX);
            if (x == 0 && y == 0 && z == 0) {
                return this.blockPos; // Fallback to stored position
            }
            return new BlockPos(x, y, z);
        }
    }
    
    public BlockPos getBlockPos() {
        return this.blockPos;
    }
    
    public int getRedstoneMode() {
        return this.containerData.get(REDSTONE_MODE_INDEX);
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
    
    /**
     * Called every tick on both client and server to sync data
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Server side: update cached structure and sync to client
        if (this.blockEntity != null) {
            String currentStructure = this.blockEntity.getSelectedStructure();
            int currentHash = currentStructure.hashCode();
            
            // If structure changed, update cache
            if (currentHash != this.lastSyncedStructureHash) {
                this.cachedSelectedStructure = currentStructure;
                this.lastSyncedStructureHash = currentHash;
            }
        }
    }
    
    /**
     * Client-side method to update cached structure when hash changes
     */
    public void updateCachedStructure() {
        if (this.blockEntity != null) {
            // Server side - directly get the structure
            this.cachedSelectedStructure = this.blockEntity.getSelectedStructure();
            this.lastSyncedStructureHash = this.cachedSelectedStructure.hashCode();
        } else {
            // Client side - check if hash changed and try to get structure from nearby block entity
            int currentHash = getStructureHash();
            if (currentHash != this.lastSyncedStructureHash) {
                this.lastSyncedStructureHash = currentHash;
                
                // Try to find the block entity to get the structure string
                if (net.minecraft.client.Minecraft.getInstance().level != null) {
                    StructurePlacerMachineBlockEntity be = getBlockEntityFromLevel(net.minecraft.client.Minecraft.getInstance().level);
                    if (be != null) {
                        this.cachedSelectedStructure = be.getSelectedStructure();
                    } else {
                        // If we can't find the block entity, clear the cache
                        this.cachedSelectedStructure = "";
                    }
                }
            }
        }
    }
    
    /**
     * Forces refresh of client data - call this when structure selection changes
     */
    public void forceDataSync() {
        if (this.blockEntity != null) {
            // Reset hash to force resync on next broadcastChanges
            this.lastSyncedStructureHash = -1;
            this.cachedSelectedStructure = "";
            broadcastChanges();
        }
    }
    
    /**
     * Gets the cached selected structure (works on both client and server)
     */
    public String getCachedSelectedStructure() {
        if (this.blockEntity != null) {
            // Server side - always get fresh data
            return this.blockEntity.getSelectedStructure();
        } else {
            // Client side - update cache if needed and return cached value
            updateCachedStructure();
            return this.cachedSelectedStructure;
        }
    }
} 