package net.unfamily.iskautils.integration.anotherdynamics;

import net.neoforged.fml.ModList;

/**
 * Optional integration with Another Dynamics ({@code another_dynamics}).
 */
public final class AnotherDynamicsCompat {
    public static final String MOD_ID = "another_dynamics";

    private AnotherDynamicsCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }
}
