package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.unfamily.iskautils.util.DeepDrawerConnectorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for Deep Drawer Extender
 * Extends the drawer's presence, allowing direct interactions (item insertion via hoppers, etc.)
 * Acts as a mirror of the adjacent drawer for IItemHandler capability
 */
public class DeepDrawerExtenderBlockEntity extends BlockEntity {
    
    // Cache of found Deep Drawer (for performance)
    private BlockPos cachedDrawerPos = null;
    private int cacheValidTicks = 0;
    private static final int CACHE_VALIDITY_TICKS = 100; // Cache valid for 5 seconds
    
    // ItemHandler that wraps the adjacent drawer's content
    private final IItemHandler itemHandler = new ExtenderItemHandler();
    
    public DeepDrawerExtenderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEEP_DRAWER_EXTENDER.get(), pos, state);
    }
    
    /**
     * Gets the item handler that exposes the adjacent drawer's content
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
    
    /**
     * Finds a connected Deep Drawer through connector network (extender, interface, extractor)
     * All connectors can connect to each other to find the drawer
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
        
        // Search through connector network
        DeepDrawersBlockEntity drawer = DeepDrawerConnectorHelper.findConnectedDrawer(level, worldPosition);
        if (drawer != null && drawer.getBlockPos() != null) {
            // Cache the found position
            cachedDrawerPos = drawer.getBlockPos();
            cacheValidTicks = 0;
        }
        
        return drawer;
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
    public static void serverTick(Level level, BlockPos pos, BlockState state, DeepDrawerExtenderBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        // Invalidate cache periodically
        blockEntity.invalidateCache();
    }
    
    /**
     * IItemHandler implementation that wraps the adjacent drawer's content
     * This allows hoppers and other blocks to interact with the drawer through the extender
     */
    private class ExtenderItemHandler implements IItemHandler {
        
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
            DeepDrawersBlockEntity drawer = findAdjacentDrawer();
            if (drawer == null) {
                return stack;
            }
            // Delegate insertion to the drawer's item handler (extender is a mirror)
            return drawer.getItemHandler().insertItem(slot, stack, simulate);
        }
        
        @NotNull
        @Override
        public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Block extraction from extender (only insertion allowed)
            return net.minecraft.world.item.ItemStack.EMPTY;
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
            DeepDrawersBlockEntity drawer = findAdjacentDrawer();
            if (drawer == null) {
                return false;
            }
            // Delegate validation to the drawer's item handler (extender is a mirror)
            return drawer.getItemHandler().isItemValid(slot, stack);
        }
    }
}
