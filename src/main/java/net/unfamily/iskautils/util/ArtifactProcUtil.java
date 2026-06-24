package net.unfamily.iskautils.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.unfamily.iskautils.Config;
import net.unfamily.iskalib.stage.StageRegistry;

public final class ArtifactProcUtil {
    private static final String MINIATURE_TENT_STAGE = ArtifactEquipStages.MINIATURE_TENT;

    private ArtifactProcUtil() {}

    public static boolean rollProc(Player player, double baseChance) {
        if (baseChance <= 0.0D) {
            return false;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return player.getRandom().nextDouble() < baseChance;
        }
        if (!ArtifactEffectGate.shouldApply(sp)) {
            return false;
        }
        double chance = Math.min(1.0D, baseChance * procMultiplier(sp));
        return sp.getRandom().nextDouble() < chance;
    }

    public static double procMultiplier(ServerPlayer player) {
        if (!StageRegistry.playerHasStage(player, MINIATURE_TENT_STAGE)) {
            return 1.0D;
        }
        return Math.max(1.0D, Config.miniatureTentProcMultiplier);
    }
}
