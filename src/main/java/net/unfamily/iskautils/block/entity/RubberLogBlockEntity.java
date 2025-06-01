package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.RubberLogBlock;

public class RubberLogBlockEntity extends BlockEntity {
    // Time until the sap refills (in ticks)
    private int refillTime = -1;

    public RubberLogBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUBBER_LOG.get(), pos, state);
        
        // If sap is empty, set a random refill time
        if (!state.getValue(RubberLogBlock.SAP_FILLED)) {
            calculateRefillTime(null);
        }
    }

    public void tickSapRefill(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        // If refill time not set, calculate it
        if (refillTime == -1) {
            calculateRefillTime(random);
            setChanged(level, pos, state);
            return;
        }
        
        // If time is up, refill the sap
        if (refillTime > 0) {
            refillTime--;
            if (refillTime == 0) {
                level.setBlock(pos, state.setValue(RubberLogBlock.SAP_FILLED, true), 3);
                setChanged(level, pos, state);
                refillTime = -1; // Reset refill time
            }
        }
    }
    
    private void calculateRefillTime(RandomSource random) {
        if (random == null) {
            random = RandomSource.create();
        }
        
        // Calculate random time between min and max from config
        int minTime = Config.rubberSapMinRefillTime;
        int maxTime = Config.rubberSapMaxRefillTime;
        refillTime = minTime + random.nextInt(maxTime - minTime + 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("RefillTime", refillTime);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("RefillTime")) {
            refillTime = tag.getInt("RefillTime");
        }
    }
} 