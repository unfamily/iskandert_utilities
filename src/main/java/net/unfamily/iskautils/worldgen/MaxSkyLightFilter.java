package net.unfamily.iskautils.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Rejects placements where sky (natural) light exceeds {@code max_inclusive}.
 * Used to keep Entropic Fungus out of brightly lit / open areas.
 */
public final class MaxSkyLightFilter extends PlacementFilter {
    public static final MapCodec<MaxSkyLightFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 15).fieldOf("max_inclusive").forGetter(filter -> filter.maxInclusive)
    ).apply(instance, MaxSkyLightFilter::new));

    private final int maxInclusive;

    private MaxSkyLightFilter(int maxInclusive) {
        this.maxInclusive = maxInclusive;
    }

    public static MaxSkyLightFilter of(int maxInclusive) {
        return new MaxSkyLightFilter(maxInclusive);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return context.getLevel().getBrightness(LightLayer.SKY, pos) <= maxInclusive;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.MAX_SKY_LIGHT.get();
    }
}
