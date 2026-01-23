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
 * Standard rubber wood block.
 * Visible in the creative tab and placeable by the player.
 */
public class RubberLogBlock extends RotatedPillarBlock {
    public static final MapCodec<RubberLogBlock> CODEC = simpleCodec(RubberLogBlock::new);
    
    public RubberLogBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 15;
    }
    
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 10;
    }
} 