package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;

public class RubberLogSacredBlockEntity extends BlockEntity {
    private BlockPos rootPos = null; // Fixed coordinates of the root/sapling
    private int checkCounter = 0; // Counter to check less frequently
    
    public RubberLogSacredBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUBBER_LOG_SACRED_BE.get(), pos, state);
    }
    
    /**
     * Sets the root position (called when block is placed during tree generation)
     */
    public void setRootPos(BlockPos rootPos) {
        this.rootPos = rootPos;
        this.setChanged();
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, RubberLogSacredBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        
        // If root position is not set, cannot check - wait for it to be set
        if (blockEntity.rootPos == null) {
            return;
        }
        
        // Check less frequently (every 20 ticks)
        blockEntity.checkCounter++;
        if (blockEntity.checkCounter < 20) {
            return;
        }
        blockEntity.checkCounter = 0;
        
        // Check if root or sapling exists at saved position
        BlockState rootState = level.getBlockState(blockEntity.rootPos);
        if (!rootState.is(ModBlocks.SACRED_RUBBER_ROOT.get()) && 
            !rootState.is(ModBlocks.SACRED_RUBBER_SAPLING.get())) {
            // Root/sapling is missing, break the block and drop items
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.world.level.block.Block.dropResources(state, serverLevel, pos, null, null, net.minecraft.world.item.ItemStack.EMPTY);
            }
            level.removeBlock(pos, false);
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rootPos != null) {
            tag.putLong("rootPos", rootPos.asLong());
        }
        tag.putInt("checkCounter", checkCounter);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("rootPos")) {
            rootPos = BlockPos.of(tag.getLong("rootPos"));
        }
        checkCounter = tag.getInt("checkCounter");
    }
}
