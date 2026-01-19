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
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.FanBlockEntity;

public class FanMenu extends AbstractContainerMenu {
    private final FanBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;
    
    // ContainerData indices for syncing
    private static final int RANGE_UP_INDEX = 0;
    private static final int RANGE_DOWN_INDEX = 1;
    private static final int RANGE_RIGHT_INDEX = 2;
    private static final int RANGE_LEFT_INDEX = 3;
    private static final int RANGE_FRONT_INDEX = 4;
    private static final int FAN_POWER_INDEX = 5; // Will store as int (multiply by 1000 for precision)
    private static final int PUSH_TYPE_INDEX = 6;
    private static final int REDSTONE_MODE_INDEX = 7;
    private static final int IS_PULL_INDEX = 8; // 0 = push, 1 = pull
    private static final int BLOCK_POS_X_INDEX = 9;
    private static final int BLOCK_POS_Y_INDEX = 10;
    private static final int BLOCK_POS_Z_INDEX = 11;
    private static final int DATA_COUNT = 12;
    
    // Module slot positions (GUI coordinates)
    private static final int MODULE_SLOTS_X = 7;
    private static final int MODULE_SLOT_1_Y = 23;
    private static final int MODULE_SLOT_2_Y = 41;
    private static final int MODULE_SLOT_3_Y = 59;
    
    // Player inventory positions
    private static final int PLAYER_INVENTORY_X = 7;
    private static final int PLAYER_INVENTORY_Y = 117;
    
    // Server-side constructor
    public FanMenu(int containerId, Inventory playerInventory, FanBlockEntity blockEntity) {
        super(ModMenuTypes.FAN_MENU.get(), containerId);
        
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Create container data that syncs with the block entity
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case RANGE_UP_INDEX -> blockEntity.getRangeUp();
                    case RANGE_DOWN_INDEX -> blockEntity.getRangeDown();
                    case RANGE_RIGHT_INDEX -> blockEntity.getRangeRight();
                    case RANGE_LEFT_INDEX -> blockEntity.getRangeLeft();
                    case RANGE_FRONT_INDEX -> blockEntity.getRangeFront();
                    case FAN_POWER_INDEX -> (int)(blockEntity.getFanPower() * 1000); // Store as int (multiply by 1000)
                    case PUSH_TYPE_INDEX -> blockEntity.getPushType().getId();
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case IS_PULL_INDEX -> blockEntity.isPull() ? 1 : 0;
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    default -> 0;
                };
            }
            
            @Override
            public void set(int index, int value) {
                // Values are read-only from client side
            }
            
            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        
        this.addDataSlots(this.containerData);
        
        // Add module slots
        addModuleSlots();
        
        // Add player inventory
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }
    
    // Client-side constructor
    public FanMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.FAN_MENU.get(), containerId);
        
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        // Create dummy container data for client
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
        
        // Add module slots (client-side fallback)
        addModuleSlots();
        
        // Add player inventory
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }
    
    private void addModuleSlots() {
        IItemHandler itemHandler;
        
        if (blockEntity != null) {
            itemHandler = blockEntity.getModuleHandler();
        } else {
            // Client-side fallback
            itemHandler = new ItemStackHandler(3);
        }
        
        // 3 module slots stacked vertically
        addSlot(new SlotItemHandler(itemHandler, 0, MODULE_SLOTS_X + 1, MODULE_SLOT_1_Y + 1));
        addSlot(new SlotItemHandler(itemHandler, 1, MODULE_SLOTS_X + 1, MODULE_SLOT_2_Y + 1));
        addSlot(new SlotItemHandler(itemHandler, 2, MODULE_SLOTS_X + 1, MODULE_SLOT_3_Y + 1));
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        // 3 rows of 9 slots (inventory)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9; // +9 to skip hotbar
                int xPos = PLAYER_INVENTORY_X + 1 + col * 18;
                int yPos = PLAYER_INVENTORY_Y + 1 + row * 18;
                
                addSlot(new Slot(playerInventory, slotIndex, xPos, yPos));
            }
        }
    }
    
    private void addPlayerHotbar(Inventory playerInventory) {
        // 1 row of 9 slots (hotbar)
        for (int col = 0; col < 9; col++) {
            int xPos = PLAYER_INVENTORY_X + 1 + col * 18;
            int yPos = PLAYER_INVENTORY_Y + 1 + 3 * 18 + 4; // Below inventory with spacing
            
            addSlot(new Slot(playerInventory, col, xPos, yPos));
        }
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.FAN.get());
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            // Module slots are 0-2
            if (index < 3) {
                // Try to move to player inventory
                if (!this.moveItemStackTo(slotStack, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try to move to module slots
                if (!this.moveItemStackTo(slotStack, 0, 3, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemstack;
    }
    
    // Getters for synced data
    public FanBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public int getRangeUp() {
        return containerData.get(RANGE_UP_INDEX);
    }
    
    public int getRangeDown() {
        return containerData.get(RANGE_DOWN_INDEX);
    }
    
    public int getRangeRight() {
        return containerData.get(RANGE_RIGHT_INDEX);
    }
    
    public int getRangeLeft() {
        return containerData.get(RANGE_LEFT_INDEX);
    }
    
    public int getRangeFront() {
        return containerData.get(RANGE_FRONT_INDEX);
    }
    
    public double getFanPower() {
        return containerData.get(FAN_POWER_INDEX) / 1000.0; // Convert back from int
    }
    
    public int getPushType() {
        return containerData.get(PUSH_TYPE_INDEX);
    }
    
    public int getRedstoneMode() {
        return containerData.get(REDSTONE_MODE_INDEX);
    }
    
    public boolean isPull() {
        return containerData.get(IS_PULL_INDEX) == 1;
    }
    
    public BlockPos getSyncedBlockPos() {
        if (this.blockEntity != null) {
            return this.blockPos;
        } else {
            int x = this.containerData.get(BLOCK_POS_X_INDEX);
            int y = this.containerData.get(BLOCK_POS_Y_INDEX);
            int z = this.containerData.get(BLOCK_POS_Z_INDEX);
            if (x == 0 && y == 0 && z == 0) {
                return this.blockPos;
            }
            return new BlockPos(x, y, z);
        }
    }
}
