package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class RubberPlanksBlock extends Block {
    public static final MapCodec<RubberPlanksBlock> CODEC = simpleCodec(RubberPlanksBlock::new);
    
    public RubberPlanksBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 20;
	}
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }
} 