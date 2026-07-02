package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;

/**
 * Spawner scan modes for scanner generic target strings ({@code spawners:all}, {@code spawners:monster}, ...).
 */
public final class ScannerSpawnerModes {
    public static final String ALL = "spawners:all";
    public static final String MONSTER = "spawners:monster";
    public static final String TRIAL = "spawners:trial";

    private static final Identifier SPAWNER_ID = BuiltInRegistries.BLOCK.getKey(Blocks.SPAWNER);
    private static final Identifier TRIAL_SPAWNER_ID = BuiltInRegistries.BLOCK.getKey(Blocks.TRIAL_SPAWNER);

    private ScannerSpawnerModes() {}

    public static boolean isSpawnerScanTarget(String genericTarget) {
        return genericTarget != null && genericTarget.startsWith("spawners");
    }

    public static String normalizedMode(String genericTarget) {
        if (genericTarget == null) {
            return "all";
        }
        if ("spawners".equals(genericTarget) || ALL.equals(genericTarget)) {
            return "all";
        }
        if (!genericTarget.startsWith("spawners:")) {
            return "all";
        }
        String suffix = genericTarget.substring("spawners:".length());
        return suffix.isEmpty() ? "all" : suffix;
    }

    public static String cycleSpawnerTarget(String current) {
        String mode = normalizedMode(current);
        return switch (mode) {
            case "monster" -> TRIAL;
            case "trial" -> ALL;
            default -> MONSTER;
        };
    }

    public static boolean matches(BlockState state, String genericTarget) {
        Block block = state.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String mode = normalizedMode(genericTarget);
        return switch (mode) {
            case "monster" -> SPAWNER_ID.equals(id);
            case "trial" -> TRIAL_SPAWNER_ID.equals(id);
            default -> isKnownSpawnerBlock(id);
        };
    }

    public static boolean isKnownSpawnerBlock(Identifier id) {
        if (SPAWNER_ID.equals(id) || TRIAL_SPAWNER_ID.equals(id)) {
            return true;
        }
        String blockId = id.toString();
        for (String entry : Config.scannerSpawnerEntries) {
            String[] parts = entry.split(";");
            if (parts.length >= 1 && parts[0].equals(blockId)) {
                return true;
            }
        }
        return false;
    }
}
