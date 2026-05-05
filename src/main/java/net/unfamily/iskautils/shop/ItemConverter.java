package net.unfamily.iskautils.shop;

import com.google.gson.Gson;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Uses Minecraft 1.21.1 parsing system to convert item strings using Minecraft 1.21.1 parsing system to support data components
 */
public class ItemConverter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson JSON_STRING_ESCAPER = new Gson();
    
    /**
     * Converts an item string in Minecraft 1.21.1 format to ItemStack
     * Supports both simple format "minecraft:diamond_sword" and with components
     * "minecraft:diamond_sword[damage=500,enchantments={sharpness:3}]"
     * 
     * @param itemString The string representing the item
     * @param count The number of items in the stack
     * @return Corresponding ItemStack or ItemStack.EMPTY if not valid
     */
    public static ItemStack parseItemString(String itemString, int count) {
        if (itemString == null || itemString.trim().isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        try {
            // Get server for registry lookup
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.warn("Server not available for item parsing: {}", itemString);
                return fallbackParsing(itemString, count);
            }
            
            try {
                // Use ItemParser directly like the /give command does
                HolderLookup.Provider registryAccess = server.registryAccess();
                ItemParser itemParser = new ItemParser(registryAccess);
                StringReader reader = new StringReader(itemString);
                
                // Parse string to get ItemResult
                ItemParser.ItemResult itemResult = itemParser.parse(reader);
                
                // Create ItemInput and then the final ItemStack
                ItemInput itemInput = new ItemInput(itemResult.item(), itemResult.components());
                ItemStack result = itemInput.createItemStack(count, false);
                
                return result;
                
            } catch (CommandSyntaxException e) {
                LOGGER.warn("Error parsing item '{}': {}. Attempting fallback.", itemString, e.getMessage());
                return fallbackParsing(itemString, count);
            }
            
        } catch (Exception e) {
            LOGGER.error("Unexpected error during item parsing '{}': {}", itemString, e.getMessage());
            return fallbackParsing(itemString, count);
        }
    }
    
    /**
     * Fallback method that uses simple parsing for compatibility
     * with strings that contain only the item ID
     */
    private static ItemStack fallbackParsing(String itemString, int count) {
        try {
            // Remove any components to get only the item ID
            String itemId = extractItemId(itemString);
            
            ResourceLocation itemResource = ResourceLocation.parse(itemId);
            var item = BuiltInRegistries.ITEM.get(itemResource);
            
            if (item != Items.AIR) {
                ItemStack stack = new ItemStack(item, count);
                return stack;
            }
        } catch (Exception e) {
            LOGGER.warn("Fallback parsing failed for '{}': {}", itemString, e.getMessage());
        }
        
        // Last fallback: return stone
        LOGGER.warn("Unable to parse item '{}', using stone as fallback", itemString);
        return new ItemStack(Items.STONE, count);
    }
    
    /**
     * Extracts the item ID from a string that might contain components
     * Ex: "minecraft:diamond_sword[damage=500]" -> "minecraft:diamond_sword"
     */
    private static String extractItemId(String itemString) {
        int bracketIndex = itemString.indexOf('[');
        if (bracketIndex != -1) {
            return itemString.substring(0, bracketIndex);
        }
        return itemString;
    }
    
    /**
     * Converts an item string with a single item (count = 1)
     */
    public static ItemStack parseItemString(String itemString) {
        return parseItemString(itemString, 1);
    }
    
    /**
     * Verifies if a string can be parsed as a valid item
     */
    public static boolean isValidItemString(String itemString) {
        if (itemString == null || itemString.trim().isEmpty()) {
            return false;
        }
        
        try {
            ItemStack result = parseItemString(itemString, 1);
            return !result.isEmpty() && result.getItem() != Items.STONE;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the display name of an item from its string
     */
    public static String getItemDisplayName(String itemString) {
        ItemStack stack = parseItemString(itemString, 1);
        if (!stack.isEmpty()) {
            return stack.getHoverName().getString();
        }
        return itemString; // Fallback to original name
    }

    /**
     * Item id plus data-component bracket in the same SNBT shape as {@link ItemParser} / {@code /give}.
     * Uses registry codecs + {@link NbtOps} so nested quotes (e.g. {@code item_name} JSON) get proper {@code \} escaping.
     */
    public static String formatAsItemArgument(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            try {
                return formatAsItemArgument(stack, server.registryAccess());
            } catch (RuntimeException e) {
                LOGGER.warn("SNBT item argument encoding failed, using legacy patch string: {}", e.getMessage());
            }
        }
        return formatAsItemArgumentLegacy(stack);
    }

    /**
     * KubeJS-like item string without quotes or count prefix.
     *
     * <p>Example: {@code minecraft:diamond_sword[damage=5,custom_data={...}]}
     * where component keys in the {@code minecraft} namespace are reduced to just the path (e.g. {@code damage}).
     */
    public static String formatAsKubeJsItemString(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            try {
                return formatAsKubeJsItemString(stack, server.registryAccess());
            } catch (RuntimeException e) {
                LOGGER.warn("KubeJS item string encoding failed, falling back to /give shape: {}", e.getMessage());
            }
        }
        return formatAsItemArgumentLegacy(stack);
    }

    /**
     * Same as {@link #formatAsKubeJsItemString(ItemStack)} but uses the given registry context.
     */
    public static String formatAsKubeJsItemString(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            return itemId.toString();
        }

        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        List<Map.Entry<DataComponentType<?>, Optional<?>>> entries = new ArrayList<>(patch.entrySet());
        entries.sort(Comparator.comparing(e -> {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(e.getKey());
            return id != null ? id.toString() : "";
        }));

        StringBuilder bracket = new StringBuilder();
        boolean first = true;
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : entries) {
            DataComponentType<?> type = entry.getKey();
            Optional<?> opt = entry.getValue();
            ResourceLocation compId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (compId == null) {
                continue;
            }

            if (!first) {
                bracket.append(',');
            }
            first = false;

            String compKey = "minecraft".equals(compId.getNamespace()) ? compId.getPath() : compId.toString();
            if (opt.isEmpty()) {
                bracket.append('!').append(compKey);
            } else {
                @SuppressWarnings("unchecked")
                DataComponentType<Object> typed = (DataComponentType<Object>) type;
                Object value = opt.get();
                DataResult<Tag> encoded = typed.codecOrThrow().encodeStart(ops, value);
                Tag tag = encoded.getOrThrow();
                bracket.append(compKey).append('=').append(tag);
            }
        }
        return itemId + "[" + bracket + "]";
    }

    /**
     * JSON compatible representation of {@link #formatAsKubeJsItemString(ItemStack)}.
     *
     * <p>It wraps the string in double quotes and performs exactly two replacements:
     * {@code \} → {@code \\} and {@code "} → {@code \"}.
     */
    public static String formatAsKubeJsItemStringJson(ItemStack stack, HolderLookup.Provider registries) {
        String raw = formatAsKubeJsItemString(stack, registries);
        if (raw.isEmpty()) {
            return "\"\"";
        }
        raw = raw.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + raw + "\"";
    }

    /**
     * Same as {@link #formatAsItemArgument(ItemStack)} but uses the given registry context (e.g. {@code player.registryAccess()} on the server).
     */
    public static String formatAsItemArgument(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            return itemId.toString();
        }
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        List<Map.Entry<DataComponentType<?>, Optional<?>>> entries = new ArrayList<>(patch.entrySet());
        entries.sort(Comparator.comparing(e -> {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(e.getKey());
            return id != null ? id.toString() : "";
        }));

        StringBuilder bracket = new StringBuilder();
        boolean first = true;
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : entries) {
            DataComponentType<?> type = entry.getKey();
            Optional<?> opt = entry.getValue();
            ResourceLocation compId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (compId == null) {
                continue;
            }
            if (!first) {
                bracket.append(',');
            }
            first = false;
            if (opt.isEmpty()) {
                bracket.append('!').append(compId);
            } else {
                @SuppressWarnings("unchecked")
                DataComponentType<Object> typed = (DataComponentType<Object>) type;
                Object value = opt.get();
                DataResult<Tag> encoded = typed.codecOrThrow().encodeStart(ops, value);
                Tag tag = encoded.getOrThrow();
                bracket.append(compId).append('=').append(tag);
            }
        }
        return itemId + "[" + bracket + "]";
    }

    /** Legacy: {@link DataComponentPatch#toString()} with {@code =>} → {@code =}; wrong for complex component values. */
    private static String formatAsItemArgumentLegacy(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            return id.toString();
        }
        String patchStr = patch.toString();
        if (patchStr.startsWith("{") && patchStr.endsWith("}")) {
            patchStr = patchStr.substring(1, patchStr.length() - 1);
        }
        patchStr = patchStr.replace("=>", "=");
        return id + "[" + patchStr + "]";
    }

    /**
     * Escapes {@code raw} for embedding inside JSON double-quoted string values (backslashes, quotes, controls).
     * {@link #formatAsItemArgument} stays unescaped for command/parse use; apply this when pasting into JSON files.
     */
    public static String escapeForJsonStringLiteral(String raw) {
        if (raw == null) {
            return "";
        }
        String quoted = JSON_STRING_ESCAPER.toJson(raw);
        return quoted.substring(1, quoted.length() - 1);
    }
} 