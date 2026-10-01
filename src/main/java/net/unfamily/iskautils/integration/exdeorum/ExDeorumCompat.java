package net.unfamily.iskautils.integration.exdeorum;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.fml.ModList;

/**
 * Soft dependency helpers for Ex Deorum (no compile-time linkage to mod classes).
 */
public final class ExDeorumCompat {
    public static final String MOD_ID = "exdeorum";

    private static final Identifier WITCH_WATER = Identifier.fromNamespaceAndPath(MOD_ID, "witch_water");

    private ExDeorumCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isWitchWater(FluidState state) {
        return isWitchWater(state.getType());
    }

    public static boolean isWitchWater(net.minecraft.world.level.material.Fluid fluid) {
        return isLoaded() && WITCH_WATER.equals(BuiltInRegistries.FLUID.getKey(fluid));
    }

    /** True for any Ex Deorum barrel block (oak_barrel, stone_barrel, …). */
    public static boolean isBarrel(Block block) {
        if (!isLoaded()) {
            return false;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("_barrel");
    }
}
