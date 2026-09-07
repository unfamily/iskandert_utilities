package net.unfamily.iskautils.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.client.gui.SwissWrenchRadialScreen;

/** Client-only Swiss Wrench helpers (keep imports out of common item code paths via DistExecutor). */
public final class SwissWrenchClient {
    private SwissWrenchClient() {
    }

    public static void openRadial(BlockPos pos, BlockState state) {
        SwissWrenchRadialScreen.tryOpen(pos, state);
    }
}
