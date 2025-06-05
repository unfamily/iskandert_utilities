package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Blocco di legno di gomma scortecciato.
 * Ottenibile usando un'ascia sul tronco di gomma o attraverso ricette specifiche.
 */
public class StrippedRubberLogBlock extends RotatedPillarBlock {
    public static final MapCodec<StrippedRubberLogBlock> CODEC = simpleCodec(StrippedRubberLogBlock::new);
    
    public StrippedRubberLogBlock(Properties properties) {
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
} 