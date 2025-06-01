package net.unfamily.iskautils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.worldgen.ModConfiguredFeatures;
import net.unfamily.iskautils.worldgen.ModPlacedFeatures;

/**
 * Una classe personalizzata per il germoglio dell'albero di gomma.
 * Implementazione semplificata che non richiede TreeGrower/AbstractTreeGrower.
 */
public class RubberSaplingBlock extends SaplingBlock {
    
    public RubberSaplingBlock(Properties properties) {
        // Usiamo un dummy TreeGrower nullo poiché sovrascriviamo il metodo advanceTree
        super(null, properties);
    }
    
    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        // La logica per far crescere l'albero qui
        // Invece di usare TreeGrower, usiamo direttamente il PlacedFeature dell'albero
        if (state.getValue(STAGE) == 0) {
            // Non è ancora pronto, imposta solo lo stadio a 1
            level.setBlock(pos, state.setValue(STAGE, 1), 4);
        } else {
            // È pronto per crescere, prova a generare l'albero
            // Rimuove il sapling
            level.removeBlock(pos, false);
            
            // Prova a posizionare l'albero
            if (!ModPlacedFeatures.placeRubberTree(level, level.getChunkSource().getGenerator(), pos, random)) {
                // Se fallisce, rimetti il sapling
                level.setBlock(pos, state, 4);
            }
        }
    }
} 