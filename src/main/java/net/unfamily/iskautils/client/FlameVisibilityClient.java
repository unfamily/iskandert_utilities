package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.unfamily.iskautils.block.BurningFlameBlock;

/**
 * Client-side global flame vision (toggle via Blazing Altar GUI or brazier/candle left-click).
 */
public final class FlameVisibilityClient {
    /** Chunk radius around the player for mesh invalidation (avoids full {@link LevelRenderer#allChanged()}). */
    private static final int REFRESH_CHUNK_RADIUS = 4;

    private static boolean globalFlameVision;

    private FlameVisibilityClient() {}

    public static void applyGlobalFlameVision(boolean enabled) {
        if (globalFlameVision == enabled) {
            return;
        }
        globalFlameVision = enabled;
        requestChunkMeshRefresh();
    }

    public static void toggleGlobalFlameVision() {
        applyGlobalFlameVision(!globalFlameVision);
    }

    public static boolean isGlobalFlameVisionEnabled() {
        return globalFlameVision;
    }

    /** @deprecated Use {@link #isGlobalFlameVisionEnabled()} */
    public static void setGlobalFlameVision(boolean enabled) {
        applyGlobalFlameVision(enabled);
    }

    public static boolean shouldShowFlames(Player player) {
        return player != null && globalFlameVision;
    }

    /**
     * Marks a modest area dirty so {@link BurningFlameBlock#getRenderShape()} updates without rebuilding the entire world.
     */
    private static void requestChunkMeshRefresh() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LevelRenderer renderer = mc.levelRenderer;
        LocalPlayer player = mc.player;
        if (level == null || renderer == null || player == null) {
            return;
        }
        int horizontal = REFRESH_CHUNK_RADIUS * 16;
        BlockPos pos = player.blockPosition();
        renderer.setBlocksDirty(
                pos.getX() - horizontal,
                level.getMinBuildHeight(),
                pos.getZ() - horizontal,
                pos.getX() + horizontal,
                level.getMaxBuildHeight(),
                pos.getZ() + horizontal);
    }
}
