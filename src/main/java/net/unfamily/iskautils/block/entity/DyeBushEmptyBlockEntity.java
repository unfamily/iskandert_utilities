package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;

/**
 * BlockEntity for the empty dye bush block.
 * Handles the timer for berry refill (empty -> filled).
 */
public class DyeBushEmptyBlockEntity extends BlockEntity {
    private int refillTimer;

    public DyeBushEmptyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DYE_BUSH_EMPTY.get(), pos, state);
        RandomSource random = RandomSource.create();
        int min = Config.MIN_DYE_BUSH_REFILL_TIME.get();
        int max = Config.MAX_DYE_BUSH_REFILL_TIME.get();
        this.refillTimer = min + random.nextInt(Math.max(1, max - min));
    }

    public int getRefillTime() {
        return this.refillTimer;
    }

    public void setRefillTime(int ticks) {
        this.refillTimer = ticks;
        this.setChanged();
    }

    /**
     * Decrements the timer and returns true when it reaches zero.
     */
    public boolean shouldRefill() {
        if (this.refillTimer > 0) {
            this.refillTimer--;
            this.setChanged();
        }
        return this.refillTimer <= 0;
    }

    public void fillWithBerries(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.refillTimer <= 0) {
            level.setBlock(pos, ModBlocks.DYE_BUSH_FILLED.get().defaultBlockState(), Block.UPDATE_ALL);
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
