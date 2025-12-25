package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity for the Temporal Overclocker
 * Accelerates ticks of linked blocks via chipset
 */
public class TemporalOverclockerBlockEntity extends BlockEntity {
    private static final String ENERGY_TAG = "Energy";
    private static final String LINKED_BLOCKS_TAG = "LinkedBlocks";
    
    // List of linked block positions
    private final List<BlockPos> linkedBlocks = new ArrayList<>();
    
    // Energy storage
    private final EnergyStorageImpl energyStorage;
    
    // Counter to manage acceleration (accelerates every N ticks)
    private int tickCounter = 0;
    private static final int ACCELERATION_INTERVAL = 1; // Accelerate every tick
    
    // Redstone mode (0=NONE, 1=LOW, 2=HIGH, 3=PULSE, 4=DISABLED)
    private int redstoneMode = 0;
    
    // Acceleration factor (modifiable from GUI, default from config)
    private int accelerationFactor = Config.temporalOverclockerAccelerationFactor;
    
    public TemporalOverclockerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEMPORAL_OVERCLOCKER_BE.get(), pos, state);
        int maxEnergy = Config.temporalOverclockerEnergyPerAcceleration <= 0 ? 0 : Config.temporalOverclockerEnergyBuffer;
        this.energyStorage = new EnergyStorageImpl(maxEnergy);
        // accelerationFactor is already initialized from the field with the value from config
    }
    
    /**
     * Adds a block to the list of linked blocks
     */
    public boolean addLinkedBlock(BlockPos pos) {
        if (linkedBlocks.size() >= Config.temporalOverclockerMaxLinks) {
            return false; // Limit reached
        }
        
        // Check distance (maximum range)
        double distance = Math.sqrt(this.worldPosition.distSqr(pos));
        if (distance > Config.temporalOverclockerLinkRange) {
            return false; // Too far
        }
        
        if (!linkedBlocks.contains(pos)) {
            linkedBlocks.add(pos);
            setChanged();
            // Force sync to client
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false; // Already present
    }
    
    /**
     * Removes a block from the list of linked blocks
     */
    public boolean removeLinkedBlock(BlockPos pos) {
        boolean removed = linkedBlocks.remove(pos);
        if (removed) {
            setChanged();
            // Force sync to client
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
        return removed;
    }
    
    /**
     * Gets the list of linked blocks
     */
    public List<BlockPos> getLinkedBlocks() {
        return new ArrayList<>(linkedBlocks);
    }
    
    /**
     * Clears all links
     */
    public void clearLinkedBlocks() {
        linkedBlocks.clear();
        setChanged();
    }
    
    /**
     * Checks if a block is linked
     */
    public boolean isLinked(BlockPos pos) {
        return linkedBlocks.contains(pos);
    }
    
    /**
     * Checks if a block can be linked (checks distance and limit)
     * @return null if it can be linked, otherwise an error message
     */
    public String canLinkBlock(BlockPos pos) {
        if (linkedBlocks.size() >= Config.temporalOverclockerMaxLinks) {
            return "max_links";
        }
        
        // Check distance (maximum range)
        double distance = Math.sqrt(this.worldPosition.distSqr(pos));
        if (distance > Config.temporalOverclockerLinkRange) {
            return "too_far";
        }
        
        if (linkedBlocks.contains(pos)) {
            return "already_linked";
        }
        
        return null; // Can be linked
    }
    
    /**
     * Main tick method
     */
    public static void tick(Level level, BlockPos pos, BlockState state, TemporalOverclockerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        
        blockEntity.tickCounter++;
        
        // Every ACCELERATION_INTERVAL ticks, check for broken blocks and accelerate linked blocks
        if (blockEntity.tickCounter >= ACCELERATION_INTERVAL) {
            blockEntity.tickCounter = 0;
            
            // Check and remove broken or invalid linked blocks
            int sizeBefore = blockEntity.linkedBlocks.size();
            blockEntity.linkedBlocks.removeIf(linkedPos -> {
                // Check if chunk is loaded
                if (!level.isLoaded(linkedPos)) {
                    return false; // Keep if chunk is not loaded (might be unloaded temporarily)
                }
                
                // Check if block still exists and is not air
                BlockState linkedState = level.getBlockState(linkedPos);
                if (linkedState.isAir()) {
                    return true; // Remove if block is air (broken)
                }
                
                // Keep blocks even if they don't have a BlockEntity (we can accelerate block ticks).
                // Only remove if it's air. This allows temporal accelerators to remain bound to plain blocks
                // that temporarily lose their BlockEntity (e.g., when converted to a normal block).
                
                return false; // Keep this block
            });
            
            // If we removed any blocks, mark as changed and sync to client
            if (blockEntity.linkedBlocks.size() < sizeBefore) {
                blockEntity.setChanged();
                // Force sync to client
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(pos, state, state, 3);
                }
            }
            
            // Accelerate each linked block
            if (!blockEntity.linkedBlocks.isEmpty() && level instanceof ServerLevel serverLevel) {
                int accelerationFactor = blockEntity.accelerationFactor;
                
                // Calculate energy consumption: energyPerAcceleration * accelerationFactor * numBlocks
                int energyPerBlock = Config.temporalOverclockerEnergyPerAcceleration * accelerationFactor;
                int totalEnergyNeeded = energyPerBlock * blockEntity.linkedBlocks.size();
                
                // Check if we have enough energy
                if (totalEnergyNeeded > 0 && blockEntity.energyStorage.getEnergyStored() < totalEnergyNeeded) {
                    return; // Not enough energy for all blocks
                }
                
                // Accelerate valid blocks (invalid ones were already removed above)
                for (BlockPos linkedPos : blockEntity.linkedBlocks) {
                    // Read state first
                    BlockState linkedState = level.getBlockState(linkedPos);
                    BlockEntity linkedBE = level.getBlockEntity(linkedPos);
                    
                    // If there's a BlockEntity, try to accelerate its ticker
                    if (linkedBE != null) {
                        BlockEntityType<?> beType = linkedBE.getType();
                        Block block = linkedState.getBlock();
                        if (block instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                            BlockEntityTicker<?> ticker = entityBlock.getTicker(level, linkedState, beType);
                            if (ticker != null) {
                                // Call the ticker multiple times to accelerate
                                @SuppressWarnings("unchecked")
                                BlockEntityTicker<BlockEntity> safeTicker = (BlockEntityTicker<BlockEntity>) ticker;
                                for (int i = 0; i < accelerationFactor - 1; i++) {
                                    try {
                                        safeTicker.tick(level, linkedPos, linkedState, linkedBE);
                                    } catch (Exception e) {
                                        // If there's an error, stop acceleration for this block
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Trigger random ticks directly (covers crops and other random-ticking blocks).
                    // Calling randomTick on a block that doesn't implement it is a no-op.
                    if (serverLevel != null) {
                        for (int i = 0; i < accelerationFactor - 1; i++) {
                            try {
                                linkedState.randomTick(serverLevel, linkedPos, serverLevel.getRandom());
                            } catch (Exception ignored) {
                                // ignore individual random tick failures
                            }
                        }
                    }
                    // Always attempt to accelerate block ticks as well (covers plain blocks without BE).
                    for (int i = 0; i < accelerationFactor - 1; i++) {
                        level.scheduleTick(linkedPos, linkedState.getBlock(), 0);
                    }
                }
                
                // Consume energy
                if (totalEnergyNeeded > 0) {
                    blockEntity.energyStorage.extractEnergy(totalEnergyNeeded, false);
                    blockEntity.setChanged();
                }
            }
        }
    }
    
    /**
     * Gets the acceleration factor
     */
    public int getAccelerationFactor() {
        return accelerationFactor;
    }
    
    /**
     * Sets the acceleration factor
     */
    public void setAccelerationFactor(int factor) {
        // Limit between min and max from config
        int min = Config.temporalOverclockerAccelerationFactorMin;
        int max = Config.temporalOverclockerAccelerationFactorMax;
        // If exceeds maximum, set to maximum
        if (factor > max) {
            this.accelerationFactor = max;
        } else {
            this.accelerationFactor = Math.max(min, factor);
        }
        setChanged();
    }
    
    /**
     * Increases the acceleration factor by 1
     */
    public void increaseAccelerationFactor() {
        int max = Config.temporalOverclockerAccelerationFactorMax;
        setAccelerationFactor(Math.min(this.accelerationFactor + 1, max));
    }
    
    /**
     * Decreases the acceleration factor by 1
     */
    public void decreaseAccelerationFactor() {
        int min = Config.temporalOverclockerAccelerationFactorMin;
        setAccelerationFactor(Math.max(this.accelerationFactor - 1, min));
    }
    
    /**
     * Increases the acceleration factor by 5
     */
    public void increaseAccelerationFactorBy5() {
        int max = Config.temporalOverclockerAccelerationFactorMax;
        setAccelerationFactor(Math.min(this.accelerationFactor + 5, max));
    }
    
    /**
     * Decreases the acceleration factor by 5
     */
    public void decreaseAccelerationFactorBy5() {
        int min = Config.temporalOverclockerAccelerationFactorMin;
        setAccelerationFactor(Math.max(this.accelerationFactor - 5, min));
    }
    
    /**
     * Sets the acceleration factor to maximum
     */
    public void setAccelerationFactorToMax() {
        setAccelerationFactor(Config.temporalOverclockerAccelerationFactorMax);
    }
    
    /**
     * Sets the acceleration factor to minimum
     */
    public void setAccelerationFactorToMin() {
        setAccelerationFactor(Config.temporalOverclockerAccelerationFactorMin);
    }
    
    /**
     * Sets the acceleration factor to default
     */
    public void setAccelerationFactorToDefault() {
        setAccelerationFactor(Config.temporalOverclockerAccelerationFactor);
    }
    
    /**
     * Gets the energy storage
     */
    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt("redstoneMode", redstoneMode);
        tag.putInt("accelerationFactor", accelerationFactor);
        
        // Save linked blocks
        ListTag linkedBlocksTag = new ListTag();
        for (BlockPos linkedPos : linkedBlocks) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("X", linkedPos.getX());
            posTag.putInt("Y", linkedPos.getY());
            posTag.putInt("Z", linkedPos.getZ());
            linkedBlocksTag.add(posTag);
        }
        tag.put(LINKED_BLOCKS_TAG, linkedBlocksTag);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(ENERGY_TAG)) {
            this.energyStorage.setEnergy(tag.getInt(ENERGY_TAG));
        }
        
        // Load redstone mode
        if (tag.contains("redstoneMode")) {
            this.redstoneMode = tag.getInt("redstoneMode");
        }
        
        // Load acceleration factor
        if (tag.contains("accelerationFactor")) {
            this.accelerationFactor = tag.getInt("accelerationFactor");
            // Ensure it's in the valid range from config
            int min = Config.temporalOverclockerAccelerationFactorMin;
            int max = Config.temporalOverclockerAccelerationFactorMax;
            // If exceeds maximum, set to maximum (as requested)
            if (this.accelerationFactor > max) {
                this.accelerationFactor = max;
            } else {
                this.accelerationFactor = Math.max(min, this.accelerationFactor);
            }
        } else {
            // Default from config
            this.accelerationFactor = Config.temporalOverclockerAccelerationFactor;
        }
        
        // Load linked blocks
        linkedBlocks.clear();
        if (tag.contains(LINKED_BLOCKS_TAG)) {
            ListTag linkedBlocksTag = tag.getList(LINKED_BLOCKS_TAG, 10); // 10 = CompoundTag type
            for (int i = 0; i < linkedBlocksTag.size(); i++) {
                CompoundTag posTag = linkedBlocksTag.getCompound(i);
                // Read manually saved coordinates (X, Y, Z)
                if (posTag.contains("X") && posTag.contains("Y") && posTag.contains("Z")) {
                    int x = posTag.getInt("X");
                    int y = posTag.getInt("Y");
                    int z = posTag.getInt("Z");
                    linkedBlocks.add(new BlockPos(x, y, z));
                }
            }
        }
    }
    
    /**
     * Gets the redstone mode
     */
    public int getRedstoneMode() {
        return redstoneMode;
    }
    
    /**
     * Sets the redstone mode
     */
    public void setRedstoneMode(int redstoneMode) {
        this.redstoneMode = redstoneMode % 5; // Ensure mode is always 0-4
        setChanged();
    }
    
    /**
     * Custom EnergyStorage implementation
     */
    public static class EnergyStorageImpl extends EnergyStorage {
        public EnergyStorageImpl(int capacity) {
            super(capacity);
        }
        
        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }
    
    // ===== Network Synchronization =====
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveAdditional(tag, provider);
        return tag;
    }
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, 
                            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, 
                            HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (pkt.getTag() != null) {
            loadAdditional(pkt.getTag(), lookupProvider);
        }
    }
}

