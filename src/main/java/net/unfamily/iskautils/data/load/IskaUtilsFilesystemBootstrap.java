package net.unfamily.iskautils.data.load;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskalib.load.LoadFilesystemBootstrap;

import java.util.Map;
import java.util.Set;

/**
 * Delegates to Library {@link LoadFilesystemBootstrap} (configurable datapack roots, zip support).
 */
public final class IskaUtilsFilesystemBootstrap {

    private IskaUtilsFilesystemBootstrap() {}

    /**
     * Merges matching files into {@code target} (later files override earlier). Returns file count merged.
     */
    public static int mergeInto(Map<ResourceLocation, JsonElement> target, String subdirUnderLoad) {
        int count = LoadFilesystemBootstrap.mergeIntoLoadSubdir(target, subdirUnderLoad);
        Set<String> types = IskaUtilsLoadPaths.typesForSubdir(subdirUnderLoad);
        if (!types.isEmpty()) {
            count += LoadFilesystemBootstrap.mergeIntoForTypes(target, types);
        }
        return count;
    }

    /** Merges every external {@code load/} JSON whose {@code type} matches {@code jsonType}. */
    public static int mergeIntoForType(Map<ResourceLocation, JsonElement> target, String jsonType) {
        return LoadFilesystemBootstrap.mergeIntoForType(target, jsonType);
    }
}
