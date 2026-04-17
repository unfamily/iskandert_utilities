package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("refill_timer", this.refillTimer);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.refillTimer = input.getInt("refill_timer").orElse(0);
    }
}
