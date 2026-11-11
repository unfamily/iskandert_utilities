package net.unfamily.iskautils.util;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.unfamily.iskalib.crafting.RecipeBlockBridge;
import org.jetbrains.annotations.Nullable;

/**
 * Machine-side gate for Library {@link RecipeBlockBridge} (stages / pack filters).
 * No providers → always allowed. With providers: null recipe id → allowed (untracked);
 * null owner UUID is normal for unclaimed machines — bridge delegates to providers
 * (world stages apply; player-scoped conditions resolve as missing when no online owner).
 */
public final class RecipeStageGate {
    private RecipeStageGate() {}

    /** True if this recipe may be crafted for the given machine owner UUID. */
    public static boolean isAllowed(
            @Nullable ResourceLocation recipeId, @Nullable UUID ownerUuid, @Nullable Level level) {
        if (!RecipeBlockBridge.hasProviders()) {
            return true;
        }
        if (recipeId == null) {
            return true;
        }
        return !RecipeBlockBridge.isRecipeBlocked(recipeId, ownerUuid, level);
    }
}
