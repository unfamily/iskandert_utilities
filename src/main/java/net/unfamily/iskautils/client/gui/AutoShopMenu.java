package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.block.ModBlocks;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class AutoShopMenu extends AbstractContainerMenu {
    private final AutoShopBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;

    public static final int FILTER_SLOT_INDEX = 0;

    private static final int BLOCK_POS_X_INDEX = 0;
    private static final int BLOCK_POS_Y_INDEX = 1;
    private static final int BLOCK_POS_Z_INDEX = 2;
    private static final int REDSTONE_MODE_INDEX = 3;
    private static final int AUTO_BUY_MODE_INDEX = 4;
    private static final int CURRENCY_INDEX = 5;
    private static final int FLUID_AMOUNT_INDEX = 6;
    private static final int FLUID_CAPACITY_INDEX = 7;
    private static final int FLUID_ID_INDEX = 8;
    private static final int GAS_AMOUNT_LOW_INDEX = 9;
    private static final int GAS_AMOUNT_HIGH_INDEX = 10;
    private static final int GAS_CAPACITY_LOW_INDEX = 11;
    private static final int GAS_CAPACITY_HIGH_INDEX = 12;
    private static final int GAS_ID_LENGTH_INDEX = 13;
    private static final int GAS_ID_START_INDEX = 14;
    private static final int GAS_ID_PACKED_INTS = 16;
    private static final int ENTRY_TYPE_INDEX = GAS_ID_START_INDEX + GAS_ID_PACKED_INTS;
    private static final int DATA_COUNT = ENTRY_TYPE_INDEX + 1;

    // Costruttore server-side
    public AutoShopMenu(int containerId, Inventory playerInventory, AutoShopBlockEntity blockEntity) {
        super(ModMenuTypes.AUTO_SHOP_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case AUTO_BUY_MODE_INDEX -> blockEntity.isAutoBuyMode() ? 1 : 0;
                    case CURRENCY_INDEX -> blockEntity.getCurrencyIndex();
                    case FLUID_AMOUNT_INDEX -> blockEntity.getFluidAmount();
                    case FLUID_CAPACITY_INDEX -> blockEntity.getFluidCapacity();
                    case FLUID_ID_INDEX -> blockEntity.getFluidRegistryId();
                    case GAS_AMOUNT_LOW_INDEX -> (int) blockEntity.getGasAmount();
                    case GAS_AMOUNT_HIGH_INDEX -> (int) (blockEntity.getGasAmount() >>> 32);
                    case GAS_CAPACITY_LOW_INDEX -> (int) blockEntity.getGasCapacity();
                    case GAS_CAPACITY_HIGH_INDEX -> (int) (blockEntity.getGasCapacity() >>> 32);
                    case GAS_ID_LENGTH_INDEX -> Math.min(blockEntity.getGasId().length(), GAS_ID_PACKED_INTS * 2);
                    case ENTRY_TYPE_INDEX -> blockEntity.getSelectedEntryType().ordinal();
                    default -> index >= GAS_ID_START_INDEX && index < ENTRY_TYPE_INDEX
                            ? packGasIdChars(blockEntity.getGasId(), index - GAS_ID_START_INDEX)
                            : 0;
                };
            }
            @Override
            public void set(int index, int value) {}
            @Override
            public int getCount() { return DATA_COUNT; }
        };
        this.addDataSlots(this.containerData);
        addAutoShopSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Costruttore client-side (NeoForge factory)
    public AutoShopMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.AUTO_SHOP_MENU.get(), containerId);
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
        addAutoShopSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.AUTO_SHOP.get());
    }

    /**
     * Ghost filter slot (slot 0): on click, set filter to a copy of carried item or clear if carried empty.
     * No item movement, no sound. We do not call super so the cursor is not modified.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == FILTER_SLOT_INDEX) {
            ItemStack carried = getCarried();
            ItemStack toSet = carried.isEmpty() ? ItemStack.EMPTY : carried.copy();
            if (!toSet.isEmpty()) {
                toSet.setCount(1);
            }
            if (blockEntity != null) {
                blockEntity.setSelectedItem(toSet);
                broadcastFullState();
            } else if (player.level().isClientSide()) {
                BlockPos pos = getSyncedBlockPos();
                if (!pos.equals(BlockPos.ZERO)) {
                    net.unfamily.iskautils.network.ModMessages.sendAutoShopSelectedItemPacket(pos, toSet);
                }
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            // Slot 0-1: slot speciali dell'Auto Shop (0 = selected, 1 = encapsulated)
            int autoShopSlots = 2;
            int inventoryEnd = 38;
            
            if (index < autoShopSlots) {
                if (!this.moveItemStackTo(itemstack1, autoShopSlots, inventoryEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < inventoryEnd) {
                // From player inventory slot: no-op for now
                // In futuro qui si potrebbero gestire transazioni automatiche con l'Auto Shop
                return ItemStack.EMPTY;
            }
        }
        
        return itemstack;
    }

    public AutoShopBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public BlockPos getBlockPos() {
        return blockPos;
    }

    public BlockPos getSyncedBlockPos() {
        if (blockEntity != null) {
            return blockPos;
        }
        int x = containerData.get(BLOCK_POS_X_INDEX);
        int y = containerData.get(BLOCK_POS_Y_INDEX);
        int z = containerData.get(BLOCK_POS_Z_INDEX);
        if (x == 0 && y == 0 && z == 0) {
            return blockPos;
        }
        return new BlockPos(x, y, z);
    }

    public int getRedstoneMode() {
        return containerData.get(REDSTONE_MODE_INDEX);
    }

    public boolean isAutoBuyMode() {
        return containerData.get(AUTO_BUY_MODE_INDEX) != 0;
    }

    public int getCurrencyIndex() {
        return containerData.get(CURRENCY_INDEX);
    }

    public String getSelectedCurrencyId() {
        return AutoShopBlockEntity.getCurrencyIdFromIndex(getCurrencyIndex());
    }

    public int getFluidAmount() {
        return containerData.get(FLUID_AMOUNT_INDEX);
    }

    public int getFluidCapacity() {
        return containerData.get(FLUID_CAPACITY_INDEX);
    }

    public int getFluidRegistryId() {
        return containerData.get(FLUID_ID_INDEX);
    }

    public long getGasAmount() {
        return combineLong(containerData.get(GAS_AMOUNT_LOW_INDEX), containerData.get(GAS_AMOUNT_HIGH_INDEX));
    }

    public long getGasCapacity() {
        return combineLong(containerData.get(GAS_CAPACITY_LOW_INDEX), containerData.get(GAS_CAPACITY_HIGH_INDEX));
    }

    public String getGasId() {
        int length = Math.max(0, Math.min(containerData.get(GAS_ID_LENGTH_INDEX), GAS_ID_PACKED_INTS * 2));
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < GAS_ID_PACKED_INTS && value.length() < length; i++) {
            int packed = containerData.get(GAS_ID_START_INDEX + i);
            value.append((char) (packed & 0xFFFF));
            if (value.length() < length) {
                value.append((char) ((packed >>> 16) & 0xFFFF));
            }
        }
        return value.toString();
    }

    public net.unfamily.iskautils.shop.ShopEntry.EntryType getSelectedEntryType() {
        int index = containerData.get(ENTRY_TYPE_INDEX);
        var values = net.unfamily.iskautils.shop.ShopEntry.EntryType.values();
        return index >= 0 && index < values.length ? values[index] : values[0];
    }

    private static long combineLong(int low, int high) {
        return Integer.toUnsignedLong(low) | ((long) high << 32);
    }

    private static int packGasIdChars(String value, int pairIndex) {
        int firstIndex = pairIndex * 2;
        int first = firstIndex < value.length() ? value.charAt(firstIndex) : 0;
        int second = firstIndex + 1 < value.length() ? value.charAt(firstIndex + 1) : 0;
        return first | (second << 16);
    }
    
    private void addAutoShopSlots() {
        IItemHandler filterHandler;
        IItemHandler encapsulatedHandler;
        
        if (blockEntity != null) {
            filterHandler = blockEntity.getFilterDisplayHandler();
            encapsulatedHandler = blockEntity.getEncapsulatedSlot();
        } else {
            // Client-side fallback (filter is logical only, syncs from server)
            filterHandler = new ItemStackHandler(1);
            encapsulatedHandler = new ItemStackHandler(1);
        }
        
        // Ghost slot for filter (display only: set/clear via click, no put/take)
        this.addSlot(new SlotItemHandler(filterHandler, 0, 61, 23) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
            @Override
            public boolean mayPickup(@NotNull Player player) {
                return false;
            }
        });
        
        // Physical slot for encapsulated item (auto buy/sell output or input)
        this.addSlot(new SlotItemHandler(encapsulatedHandler, 0, 61, 48));
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        // Player inventory (3x9)
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 20 + l * 18, 84 + i * 18));
            }
        }
    }
    
    private void addPlayerHotbar(Inventory playerInventory) {
        // Player hotbar (1x9)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 20 + i * 18, 142));
        }
    }
} 