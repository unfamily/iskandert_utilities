package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

/**
 * Blocco di legno di gomma con tutte le 6 facce con corteccia.
 */
public class RubberWoodBlock extends RotatedPillarBlock {
    public static final MapCodec<RubberWoodBlock> CODEC = simpleCodec(RubberWoodBlock::new);
    
    public RubberWoodBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }
    
	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 15;
	}
	
	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 10;
	}

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }
} 