package net.unfamily.iskautils.arcane;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class ArcaneDictionaryEntryGate {
    private ArcaneDictionaryEntryGate() {}

    public static boolean traitActive(ServerPlayer player, Identifier traitId) {
        ArcaneDictionaryDefinition.Entry entry = ArcaneDictionaryLoader.findEntry(traitId);
        if (entry == null) {
            return true;
        }
        return entry.isFullyEligible(player);
    }
}
