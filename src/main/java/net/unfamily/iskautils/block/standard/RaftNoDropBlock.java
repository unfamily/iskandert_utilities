package net.unfamily.iskautils.block.standard;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.block.RaftBlock;

import java.util.Collections;
import java.util.List;

/**
 * RaftNoDropBlock - A version of the raft that never drops when destroyed
 */
public class RaftNoDropBlock extends RaftBlock {
    
    public static final MapCodec<RaftNoDropBlock> CODEC = simpleCodec(RaftNoDropBlock::new);
    
    public RaftNoDropBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    protected MapCodec<? extends RaftBlock> codec() {
        return CODEC;
    }
    
    // Override getDrops method to never drop anything
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Return an empty list (no drops)
        return Collections.emptyList();
    }
} 