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
import net.minecraft.world.item.ItemStack;

/**
 * Block Entity for Shop Block
 * Manages shop interface and transactions
 */
public class ShopBlockEntity extends BlockEntity implements MenuProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopBlockEntity.class);
    
    // Item storage for shop slots (display and transactions)
    private final ItemStackHandler itemHandler = new ItemStackHandler(36) { // 4 rows x 9 slots
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
        
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };
    
    // Shop state (simplified)
    private boolean isActive = false;
    private String currentCategory = "000_default";
    
    public ShopBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SHOP_BE.get(), pos, blockState);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.shop");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new net.unfamily.iskautils.client.gui.ShopMenu(containerId, playerInventory, this);
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        
        // Save only essential data
        nbt.putBoolean("isActive", this.isActive);
        nbt.putString("currentCategory", this.currentCategory);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        
        // Load only essential data
        this.isActive = nbt.getBoolean("isActive");
        this.currentCategory = nbt.getString("currentCategory");
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    // Methods for data access
    
    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }
    
    public boolean isActive() {
        return this.isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
        setChanged();
    }
    
    public String getCurrentCategory() {
        return this.currentCategory;
    }
    
    public void setCurrentCategory(String category) {
        this.currentCategory = category;
        setChanged();
    }
    
    /**
     * Block tick (called by server)
     */
    public static void tick(Level level, BlockPos pos, BlockState state, ShopBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
    }
} 