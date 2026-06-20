package net.unfamily.iskautils.util;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.arcane.effects.CurseOfUselessEffect;

public final class RelicEffectGate {
    private RelicEffectGate() {}

    public static boolean shouldApply(ServerPlayer player) {
        return !CurseOfUselessEffect.isBlocking(player);
    }
}
