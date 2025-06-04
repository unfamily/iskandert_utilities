package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.worldgen.tree.ModTreeGrowers;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe per il germoglio dell'albero di gomma.
 * Estende SaplingBlock per utilizzare il sistema standard di crescita degli alberi di Minecraft.
 */
public class RubberSaplingBlock extends SaplingBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(RubberSaplingBlock.class);
    public static final MapCodec<RubberSaplingBlock> CODEC = simpleCodec(RubberSaplingBlock::new);
    
    public RubberSaplingBlock(Properties properties) {
        super(ModTreeGrowers.RUBBER, properties);
        LOGGER.debug("RubberSaplingBlock creato con TreeGrower: {}", ModTreeGrowers.RUBBER);
    }
    
    @Override
    public @NotNull MapCodec<? extends SaplingBlock> codec() {
        return CODEC;
    }
    
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // Verifica che il terreno sotto sia valido (non acqua o lava)
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || 
               state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || 
               state.is(Blocks.FARMLAND);
    }
} 