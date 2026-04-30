package net.unfamily.iskautils.data.load;

import net.minecraft.resources.ResourceLocation;

/**
 * Subdirectories under {@code data/<namespace>/load/} for IskaUtils JSON.
 */
public final class IskaUtilsLoadPaths {
    private IskaUtilsLoadPaths() {}

    public static final String LOAD_FOLDER = "load";

    public static final String COMMAND_ITEMS = "iska_utils_command_items";
    public static final String PLATES = "iska_utils_plates";
    public static final String STRUCTURE_MONOUSE = "iska_utils_structures_monouse";
    public static final String SHOP = "iska_utils_shop";
    public static final String MACROS = "iska_utils_macros";
    public static final String STAGE_ACTIONS = "iska_utils_stage_actions";
    public static final String STAGE_ITEMS = "iska_utils_stage_items";
    /** Server structure definitions (iska_utils:structure), not monouse items. */
    public static final String STRUCTURE_DEFINITIONS = "iska_utils_structure_definitions";

    public static String loadSubdirPrefix(String subdir) {
        return LOAD_FOLDER + "/" + subdir + "/";
    }

    public static boolean isJsonUnderLoadSubdir(ResourceLocation id, String subdir) {
        String p = id.getPath();
        return p.startsWith(loadSubdirPrefix(subdir)) && p.endsWith(".json");
    }
}

