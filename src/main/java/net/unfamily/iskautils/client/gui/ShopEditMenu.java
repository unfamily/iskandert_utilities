package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.shop.edit.ShopEditSession;
import net.unfamily.iskautils.shop.edit.ShopEditWorkspace;

/**
 * Shop editor menu with player inventory. Ghost slot is off-screen (screen draws the pick target).
 */
public class ShopEditMenu extends AbstractContainerMenu {

    public static final int GHOST_SLOT = 0;
    public static final int PLAYER_INV_START = 1;
    public static final int PLAYER_INV_END = 37; // exclusive

    private final ItemStackHandler ghost = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private ShopEditWorkspace.ShopEditData clientData = new ShopEditWorkspace.ShopEditData();
    private final boolean serverSide;

    public ShopEditMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, false);
    }

    public ShopEditMenu(int containerId, Inventory playerInventory, boolean serverSide) {
        super(ModMenuTypes.SHOP_EDIT_MENU.get(), containerId);
        this.serverSide = serverSide;
        // Off-screen ghost; screen renders the pick target and applies carried items.
        addSlot(new SlotItemHandler(ghost, 0, -10000, -10000) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public boolean mayPickup(Player player) {
                return true;
            }
        });
        addPlayerInventorySlots(playerInventory);
        if (serverSide) {
            ShopEditWorkspace.ShopEditData data = ShopEditSession.getData();
            if (data != null) {
                clientData = data;
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                int xPos = 20 + col * 18;
                int yPos = 154 + row * 18;
                this.addSlot(new Slot(playerInventory, slotIndex, xPos, yPos));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 20 + col * 18, 212));
        }
    }

    public ShopEditWorkspace.ShopEditData getData() {
        return clientData;
    }

    public void applySync(ShopEditWorkspace.ShopEditData data) {
        this.clientData = data != null ? data : new ShopEditWorkspace.ShopEditData();
    }

    public ItemStackHandler getGhostHandler() {
        return ghost;
    }

    public void setGhostStack(ItemStack stack) {
        ghost.setStackInSlot(0, stack == null ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    public ItemStack getGhostStack() {
        return ghost.getStackInSlot(0);
    }

    public void clearGhostStack() {
        setGhostStack(ItemStack.EMPTY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == GHOST_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 28) {
            // Main inventory → hotbar
            if (!this.moveItemStackTo(stack, 28, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Hotbar → main inventory
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, 28, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) {
            return true;
        }
        return player instanceof net.minecraft.server.level.ServerPlayer sp && ShopEditSession.isHolder(sp);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            ShopEditSession.releaseIfHolder(sp);
        }
    }
}
