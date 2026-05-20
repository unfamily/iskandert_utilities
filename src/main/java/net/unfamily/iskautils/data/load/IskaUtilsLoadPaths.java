package net.unfamily.iskautils.data.load;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Set;

/**
 * Subdirectories under {@code data/<namespace>/load/} for IskaUtils JSON.
 * <p>
 * Files may live in {@code load/<subdir>/} or directly in {@code load/} (KubeJS-style);
 * flat files are routed by their JSON {@code type} field.
 */
public final class IskaUtilsLoadPaths {
    private IskaUtilsLoadPaths() {}

    public static final String LOAD_FOLDER = "load";

    public static final String TYPE_PLATES = "iska_utils:plates";
    public static final String TYPE_PLATES_LEGACY = "iska_utils:potion_plates";
    public static final String TYPE_COMMAND_ITEM = "iska_utils:command_item";
    public static final String TYPE_STRUCTURE_MONOUSE = "iska_utils:structure_monouse_item";
    public static final String TYPE_SHOP_CURRENCY = "iska_utils:shop_currency";
    public static final String TYPE_SHOP_CURRENCY_LEGACY = "iska_utils:shop_valute";
    public static final String TYPE_SHOP_CATEGORY = "iska_utils:shop_category";
    public static final String TYPE_SHOP_ENTRY = "iska_utils:shop_entry";
    public static final String TYPE_MACRO = "iska_utils:commands_macro";
    public static final String TYPE_STAGE_ACTIONS = "iska_utils:stage_actions";
    public static final String TYPE_STAGE_ITEM = "iska_utils:stage_item";
    public static final String TYPE_STRUCTURE = "iska_utils:structure";

    public static final String COMMAND_ITEMS = "iska_utils_command_items";
    public static final String PLATES = "iska_utils_plates";
    public static final String STRUCTURE_MONOUSE = "iska_utils_structures_monouse";
    public static final String SHOP = "iska_utils_shop";
    public static final String MACROS = "iska_utils_macros";
    public static final String STAGE_ACTIONS = "iska_utils_stage_actions";
    public static final String STAGE_ITEMS = "iska_utils_stage_items";
    /** Server structure definitions (iska_utils:structure), not monouse items. */
    public static final String STRUCTURE_DEFINITIONS = "iska_utils_structure_definitions";

    private static final Map<String, Set<String>> TYPES_BY_SUBDIR = Map.of(
            COMMAND_ITEMS, Set.of(TYPE_COMMAND_ITEM),
            PLATES, Set.of(TYPE_PLATES, TYPE_PLATES_LEGACY),
            STRUCTURE_MONOUSE, Set.of(TYPE_STRUCTURE_MONOUSE),
            SHOP, Set.of(TYPE_SHOP_CURRENCY, TYPE_SHOP_CURRENCY_LEGACY, TYPE_SHOP_CATEGORY, TYPE_SHOP_ENTRY),
            MACROS, Set.of(TYPE_MACRO),
            STAGE_ACTIONS, Set.of(TYPE_STAGE_ACTIONS),
            STAGE_ITEMS, Set.of(TYPE_STAGE_ITEM),
            STRUCTURE_DEFINITIONS, Set.of(TYPE_STRUCTURE)
    );

    public static String loadSubdirPrefix(String subdir) {
        return LOAD_FOLDER + "/" + subdir + "/";
    }

    public static boolean isJsonUnderLoadSubdir(Identifier id, String subdir) {
        String p = id.getPath();
        return p.startsWith(loadSubdirPrefix(subdir)) && p.endsWith(".json");
    }

    /** {@code data/<namespace>/load/<file>.json} (not under a load subfolder). */
    public static boolean isJsonDirectlyUnderLoad(Identifier id) {
        String p = id.getPath();
        if (!p.startsWith(LOAD_FOLDER + "/") || !p.endsWith(".json")) {
            return false;
        }
        String afterLoad = p.substring(LOAD_FOLDER.length() + 1);
        return !afterLoad.contains("/");
    }

    public static Set<String> typesForSubdir(String subdirUnderLoad) {
        return TYPES_BY_SUBDIR.getOrDefault(subdirUnderLoad, Set.of());
    }

    public static boolean jsonMatchesSubdir(JsonElement element, String subdirUnderLoad) {
        if (element == null || !element.isJsonObject()) {
            return false;
        }
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has("type") || !obj.get("type").isJsonPrimitive()) {
            return false;
        }
        return typesForSubdir(subdirUnderLoad).contains(obj.get("type").getAsString());
    }

    /**
     * {@code data/iska_utils/recipe/factory/*.json} (crafting + factory datapack definitions).
     */
    public static boolean isFactoryRecipeFile(Identifier id) {
        String p = id.getPath();
        return p.startsWith("recipe/factory/") && p.endsWith(".json");
    }
}
