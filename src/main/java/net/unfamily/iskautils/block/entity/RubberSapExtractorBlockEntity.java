package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.RubberLogEmptyBlock;
import net.unfamily.iskautils.block.RubberLogFilledBlock;
import net.unfamily.iskautils.block.RubberSapExtractorBlock;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;

public class RubberSapExtractorBlockEntity extends BlockEntity implements WorldlyContainer {
    // Inventory: slot 0 for output (sap)
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    
    // Energy
    private final EnergyStorageImpl energyStorage;
    
    // Counters and state
    private int extractionProgress = 0;
    private int extractionTime = Config.rubberSapExtractorSpeed;
    private boolean isPowered = false;
    
    public RubberSapExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUBBER_SAP_EXTRACTOR.get(), pos, state);
        int maxEnergy = Config.rubberSapExtractorEnergyConsume <= 0 ? 0 : Config.rubberSapExtractorEnergyBuffer;
        this.energyStorage = new EnergyStorageImpl(maxEnergy);
    }
    
    /**
     * Main method called at each server tick
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, RubberSapExtractorBlockEntity blockEntity) {
        // if the block is powered
        if (state.getValue(RubberSapExtractorBlock.POWERED)) {
            return;
        }
        
        // Check if energy system is enabled
        int energyRequired = getEnergyRequired();
        if (energyRequired > 0) {
            // check if there is enough energy
            if (blockEntity.energyStorage.getEnergyStored() < energyRequired) {
                return;
            }
        }
        
        // only proceed if there is space in the output inventory
        if (blockEntity.isFull()) {
            return;
        }
        
        // if we are not already extracting, search for a filled log
        if (blockEntity.extractionProgress == 0) {
            // get the direction in which the extractor is facing
            Direction facing = state.getValue(RubberSapExtractorBlock.FACING);
            
            // check the blocks behind the extractor
            Direction opposite = facing.getOpposite();
            BlockPos checkPos = pos.relative(opposite);
            
            // function to find a filled rubber log block
            BlockPos filledLogPos = blockEntity.findFilledRubberLog(level, checkPos, opposite);
            
            // if a filled log block was found, start the extraction
            if (filledLogPos != null) {
                blockEntity.extractionProgress = 1;
                blockEntity.setChanged();
            }
        } 
        // if we are already extracting, continue the process
        else {
            blockEntity.extractionProgress++;
            
            // reduce the extraction time to make it faster
            if (blockEntity.extractionProgress >= (blockEntity.extractionTime)) {
                // extract sap
                blockEntity.extractSap();
                
                // consume energy if required
                int energyConsumption = getEnergyRequired();
                if (energyConsumption > 0) {
                    blockEntity.energyStorage.extractEnergy(energyConsumption, false);
                }
                
                // reset progress
                blockEntity.extractionProgress = 0;
                blockEntity.setChanged();
            }
        }
    }
    
    /**
     * Get the energy required for operation
     * @return Energy required, or 0 if energy system is disabled
     */
    private static int getEnergyRequired() {
        // Se l'energia richiesta è a 0, l'energia storabile è a 0 automaticamente
        if (Config.rubberSapExtractorEnergyConsume <= 0) {
            return 0;
        }
        
        // Se l'energia storabile è a 0 il consumo elettrico è disattivato
        if (Config.rubberSapExtractorEnergyBuffer <= 0) {
            return 0;
        }
        
        // Se l'energia richiesta è maggiore di quella massima storabile, l'energia richiesta è = a quella massima storabile
        return Math.min(Config.rubberSapExtractorEnergyConsume, Config.rubberSapExtractorEnergyBuffer);
    }
    
    /**
     * Searches for a filled rubber log block, checking first behind the block and then going up
     * @param level The world
     * @param startPos Starting position
     * @param searchDirection Direction to continue searching
     * @return The position of the found filled block, or null if not found
     */
    private BlockPos findFilledRubberLog(Level level, BlockPos startPos, Direction searchDirection) {
        BlockPos currentPos = startPos;
        
        // First check if the block behind is a filled rubber log
        BlockState state = level.getBlockState(currentPos);
        if (state.is(ModBlocks.RUBBER_LOG_FILLED.get())) {
            return currentPos;
        }
        
        // If it's an empty rubber log or a normal log, continue going up
        if (state.is(ModBlocks.RUBBER_LOG_EMPTY.get()) || state.is(ModBlocks.RUBBER_LOG.get())) {
            // Continue going up until we find a filled block or exit the rubber log column
            BlockPos abovePos = currentPos;
            int maxChecks = 10; // Limit to avoid infinite loops
            
            for (int i = 0; i < maxChecks; i++) {
                abovePos = abovePos.above();
                BlockState aboveState = level.getBlockState(abovePos);
                
                // If we find a filled block, return it
                if (aboveState.is(ModBlocks.RUBBER_LOG_FILLED.get())) {
                    return abovePos;
                }
                
                // If it's not a rubber log, exit the loop
                if (!aboveState.is(ModBlocks.RUBBER_LOG_EMPTY.get()) && !aboveState.is(ModBlocks.RUBBER_LOG.get())) {
                    break;
                }
            }
        }
        
        // No filled block found
        return null;
    }
    
    /**
     * Extracts sap from the filled log and adds it to the inventory
     */
    private void extractSap() {
        if (this.level == null) return;
        
        // get the direction in which the extractor is facing
        BlockState blockState = this.level.getBlockState(this.getBlockPos());
        if (!blockState.hasProperty(RubberSapExtractorBlock.FACING)) return;
        
        Direction facing = blockState.getValue(RubberSapExtractorBlock.FACING);
        
        // check the blocks behind the extractor
        Direction opposite = facing.getOpposite();
        BlockPos checkPos = this.getBlockPos().relative(opposite);
        
        // search for a filled rubber log block
        BlockPos filledLogPos = findFilledRubberLog(this.level, checkPos, opposite);
        
        // add sap only if a replaceable block is found
        if (filledLogPos != null) {
            // get the state of the filled log before modifying it
            BlockState filledLogState = this.level.getBlockState(filledLogPos);
            
            // convert the filled log to empty preserving all properties
            BlockState emptyLogState = ModBlocks.RUBBER_LOG_EMPTY.get().defaultBlockState();
            
            // preserve the orientation (AXIS) if present
            if (filledLogState.hasProperty(BlockStateProperties.AXIS) && 
                emptyLogState.hasProperty(BlockStateProperties.AXIS)) {
                emptyLogState = emptyLogState.setValue(
                    BlockStateProperties.AXIS, 
                    filledLogState.getValue(BlockStateProperties.AXIS)
                );
            }
            
            // preserve the original block direction (FACING)
            if (filledLogState.hasProperty(RubberLogFilledBlock.FACING) && 
                emptyLogState.hasProperty(RubberLogEmptyBlock.FACING)) {
                emptyLogState = emptyLogState.setValue(
                    RubberLogEmptyBlock.FACING, 
                    filledLogState.getValue(RubberLogFilledBlock.FACING)
                );
            }
            
            // set the block as empty
            this.level.setBlock(filledLogPos, emptyLogState, Block.UPDATE_ALL);
            
            // add ONE sap to the inventory up to the max stack size
            ItemStack sapStack = new ItemStack(ModItems.SAP.get());
            int maxStackSize = sapStack.getMaxStackSize();
            
            if (items.get(0).isEmpty()) {
                sapStack.setCount(1);
                items.set(0, sapStack);
            } else {
                ItemStack existingStack = items.get(0);
                if (existingStack.getCount() < maxStackSize) {
                    existingStack.grow(1);
                }
            }
        }
    }
    
    /**
     * Checks if the output inventory is full
     * @return true if the inventory is full
     */
    private boolean isFull() {
        ItemStack stack = items.get(0);
        return !stack.isEmpty() && stack.getCount() >= stack.getMaxStackSize();
    }
    
    /**
     * Called when the powered state of the block changes
     */
    public void onPoweredStateChanged(boolean powered) {
        this.isPowered = powered;
        this.setChanged();
    }
    
    /**
     * Returns the list of items contained
     * @return list of items in the container
     */
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        
        // save the inventory
        ContainerHelper.saveAllItems(tag, this.items, provider);
        
        // save the extraction state
        tag.putInt("ExtractionProgress", this.extractionProgress);
        
        // save energy
        tag.putInt("Energy", this.energyStorage.getEnergyStored());
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        
        // load the inventory 
        ContainerHelper.loadAllItems(tag, this.items, provider);
        
        // load the extraction state
        this.extractionProgress = tag.getInt("ExtractionProgress");
        
        // load energy
        this.energyStorage.setEnergy(tag.getInt("Energy"));
    }
    
    /**
     * Custom Energy Storage implementation
     */
    public class EnergyStorageImpl extends EnergyStorage {
        public EnergyStorageImpl(int capacity) {
            super(capacity);
        }
        
        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }
    
    /**
     * Gets the energy storage
     */
    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }
    
    // --- WorldlyContainer Implementation ---
    
    private static final int[] SLOTS_FOR_UP = new int[]{};
    private static final int[] SLOTS_FOR_DOWN = new int[]{0};
    private static final int[] SLOTS_FOR_SIDES = new int[]{};
    
    @Override
    public int getContainerSize() {
        return items.size();
    }
    
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }
    
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }
    
    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0 &&
               level.getBlockState(worldPosition).getBlock() instanceof RubberSapExtractorBlock;
    }
    
    @Override
    public void clearContent() {
        items.clear();
    }
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        }
        return side == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
    }
    
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return false; // we don't accept input
    }
    
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && slot == 0; // only output from the bottom
    }
} 