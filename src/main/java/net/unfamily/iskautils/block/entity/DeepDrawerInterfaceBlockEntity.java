package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for Deep Drawer Interface
 * Exposes the content of adjacent Deep Drawer to other mods via IItemHandler capability
 * This allows storage mods to read the entire content of the Deep Drawer
 */
public class DeepDrawerInterfaceBlockEntity extends BlockEntity {
    
    // Cache of found Deep Drawer (for performance)
    private BlockPos cachedDrawerPos = null;
    private int cacheValidTicks = 0;
    private static final int CACHE_VALIDITY_TICKS = 100; // Cache valid for 5 seconds
    
    // ItemHandler that wraps the adjacent drawer's content
    private final IItemHandler itemHandler = new InterfaceItemHandler();
    
    public DeepDrawerInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEEP_DRAWER_INTERFACE.get(), pos, state);
    }
    
    /**
     * Gets the item handler that exposes the adjacent drawer's content
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
    
    /**
     * Finds an adjacent Deep Drawer (in all 6 directions)
     */
    @Nullable
    private DeepDrawersBlockEntity findAdjacentDrawer() {
        if (level == null) {
            return null;
        }
        
        // Use cache if valid
        if (cachedDrawerPos != null && cacheValidTicks < CACHE_VALIDITY_TICKS) {
            BlockEntity be = level.getBlockEntity(cachedDrawerPos);
            if (be instanceof DeepDrawersBlockEntity drawer) {
                return drawer;
            } else {
                // Cache invalid, reset
                cachedDrawerPos = null;
            }
        }
        
        // Search in all 6 directions
        for (@NotNull Direction direction : Direction.values()) {
            @NotNull BlockPos checkPos = worldPosition.relative(direction);
            BlockEntity be = level.getBlockEntity(checkPos);
            
            if (be instanceof DeepDrawersBlockEntity drawer) {
                // Cache the found position
                cachedDrawerPos = checkPos;
                cacheValidTicks = 0;
                return drawer;
            }
        }
        
        return null;
    }
    
    /**
     * Invalidates the cache (called periodically)
     */
    public void invalidateCache() {
        cacheValidTicks++;
        if (cacheValidTicks >= CACHE_VALIDITY_TICKS) {
            cachedDrawerPos = null;
            cacheValidTicks = 0;
        }
    }
    
    /**
     * Server-side tick for cache invalidation
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DeepDrawerInterfaceBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        // Invalidate cache periodically
        blockEntity.invalidateCache();
    }
    
    /**
     * IItemHandler implementation that wraps the adjacent drawer's content
     * This allows storage mods to read the entire Deep Drawer inventory
     */
    private class InterfaceItemHandler implements IItemHandler {
        
        @Override
        public int getSlots() {
            DeepDrawersBlockEntity drawer = findAdjacentDrawer();
            if (drawer == null) {
                return 0;
            }
            // Return the drawer's item handler which exposes all slots
            return drawer.getItemHandler().getSlots();
        }
        
        @NotNull
        @Override
        public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
            DeepDrawersBlockEntity drawer = findAdjacentDrawer();
            if (drawer == null) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            // Return the item from the drawer's slot
            return drawer.getItemHandler().getStackInSlot(slot);
        }
        
        @NotNull
        @Override
        public net.minecraft.world.item.ItemStack insertItem(int slot, @NotNull net.minecraft.world.item.ItemStack stack, boolean simulate) {
            // Interface is read-only for insertion (drawer handles its own input)
            return stack;
        }
        
        @NotNull
        @Override
        public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
            DeepDrawersBlockEntity drawer = findAdjacentDrawer();
            if (drawer == null) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            // Allow extraction through the drawer's item handler
            return drawer.getItemHandler().extractItem(slot, amount, simulate);
        }
        
        @Override
        public int getSlotLimit(int slot) {
            DeepDrawersBlockEntity drawer = findAdjacentDrawer();
            if (drawer == null) {
                return 0;
            }
            return drawer.getItemHandler().getSlotLimit(slot);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull net.minecraft.world.item.ItemStack stack) {
            // Interface is read-only for validation
            return false;
        }
    }
}
