package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.RubberLogEmptyBlock;
import net.unfamily.iskautils.block.RubberLogFilledBlock;

/** 
 * BlockEntity for the empty rubber log block.
 * Handles the timer for the sap refill.
 */
public class RubberLogEmptyBlockEntity extends BlockEntity {
    private int refillTimer;    // Timer for the sap refill, decremented at each tick

    public RubberLogEmptyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUBBER_LOG_EMPTY.get(), pos, state);
        RandomSource random = RandomSource.create();
        this.refillTimer = Config.MIN_SAP_REFILL_TIME.get() + 
                random.nextInt(Config.MAX_SAP_REFILL_TIME.get() - Config.MIN_SAP_REFILL_TIME.get());
    }

    /**
     * Gets the current refill timer value.
     * @return Current refill timer in ticks
     */
    public int getRefillTime() {
        return this.refillTimer;
    }

    /**
     * Sets the timer for the sap refill of the block.
     * @param ticks Time in tick
     */
    public void setRefillTime(int ticks) {
        this.refillTimer = ticks;
        this.setChanged();
    }
    
    /**
     * Checks if the block is ready to be filled with sap.
     * Decrements the timer and returns true when it reaches zero.
     * @return true if the timer has expired
     */
    public boolean shouldRefill() {
        if (this.refillTimer > 0) {
            this.refillTimer--;
            this.setChanged();
        }
        return this.refillTimer <= 0;
    }
    
    /**
     * Fills the block with sap, transforming it into a filled block.
     * @param level The level
     * @param pos The position of the block
     * @param state The state of the block
     */
    public void fillWithSap(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.refillTimer <= 0) {
            Direction facing = state.getValue(RubberLogEmptyBlock.FACING);
            BlockState filledState = ModBlocks.RUBBER_LOG_FILLED.get().defaultBlockState()
                    .setValue(RubberLogFilledBlock.FACING, facing);
            
            level.setBlock(pos, filledState, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("refill_timer", this.refillTimer);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.refillTimer = tag.getInt("refill_timer");
    }
} 