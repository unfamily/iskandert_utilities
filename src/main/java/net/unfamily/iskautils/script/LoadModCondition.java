package net.unfamily.iskautils.script;

import net.neoforged.fml.ModList;

/**
 * A single mod presence requirement for load JSON ({@code mods[]} entries).
 */
public record LoadModCondition(String modId, boolean requiredLoaded) {

    public boolean matches() {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        return ModList.get().isLoaded(modId) == requiredLoaded;
    }
}
