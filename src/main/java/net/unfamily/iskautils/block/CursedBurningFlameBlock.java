package net.unfamily.iskautils.block;

/**
 * Cursed variant of burning flame: same ignite rules as {@link BurningFlameBlock}
 * ({@code burning_flame_super_hot} config or {@code iska_utils_internal-curse_flame} stage).
 * Never drops when broken.
 */
public class CursedBurningFlameBlock extends BurningFlameBlock {

    public CursedBurningFlameBlock(Properties properties) {
        super(properties);
    }
}
