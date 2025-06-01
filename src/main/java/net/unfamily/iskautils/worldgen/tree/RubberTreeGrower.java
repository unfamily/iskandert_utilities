package net.unfamily.iskautils.worldgen.tree;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.unfamily.iskautils.worldgen.ModConfiguredFeatures;
import org.jetbrains.annotations.Nullable;

/**
 * Implementazione semplificata di un generatore di alberi.
 * Questa classe è usata solo come riferimento, dato che l'implementazione reale
 * è stata spostata in RubberSaplingBlock per evitare problemi di compatibilità.
 */
public class RubberTreeGrower {
    
    private final String name;
    
    public RubberTreeGrower() {
        this.name = "rubber";
    }
    
    /**
     * Ottiene la feature configurata per l'albero di gomma.
     */
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource randomSource, boolean hasFlowers) {
        return ModConfiguredFeatures.RUBBER_KEY;
    }
} 