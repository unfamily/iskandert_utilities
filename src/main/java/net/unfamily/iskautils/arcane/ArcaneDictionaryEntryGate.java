package net.unfamily.iskautils.arcane;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ArcaneDictionaryEntryGate {
    private ArcaneDictionaryEntryGate() {}

    public static boolean traitActive(ServerPlayer player, ResourceLocation traitId) {
        ArcaneDictionaryDefinition.Entry entry = ArcaneDictionaryLoader.findEntry(traitId);
        if (entry == null) {
            return true;
        }
        return entry.isFullyEligible(player);
    }
}
