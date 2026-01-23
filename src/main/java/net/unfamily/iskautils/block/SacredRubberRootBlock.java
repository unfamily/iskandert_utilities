package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Sacred rubber root block placed at the base of the sacred rubber tree.
 * Replaces the sapling when growth completes.
 */
public class SacredRubberRootBlock extends Block {
    public static final MapCodec<SacredRubberRootBlock> CODEC = simpleCodec(SacredRubberRootBlock::new);
    
    public SacredRubberRootBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
