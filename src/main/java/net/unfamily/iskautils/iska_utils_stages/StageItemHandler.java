package net.unfamily.iskautils.iska_utils_stages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.stage.StageRegistry;
import net.unfamily.iskautils.util.ResourceUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages staged items by checking inventories and applying consequences
 * based on restrictions defined in JSON files.
 */
public class StageItemHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map of item restrictions loaded from JSON files
    private static final Map<String, StageItemRestriction> ITEM_RESTRICTIONS = new HashMap<>();
    
    /**
     * Loads all item restrictions from JSON files
     */
    public static void loadItemRestrictions(Path configPath) {
        ITEM_RESTRICTIONS.clear();
        
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                // Create an example file
                createExampleFile(configPath);
            }
            
            Files.list(configPath)
                .filter(path -> path.toString().endsWith(".json") && !path.getFileName().toString().equals("example.json"))
                .forEach(path -> {
                    try {
                        JsonObject json = GSON.fromJson(new FileReader(path.toFile()), JsonObject.class);
                        if (json.has("type") && json.get("type").getAsString().equals("iska_utils:stage_item")) {
                            StageItemRestriction restriction = parseRestriction(json);
                            String id = path.getFileName().toString().replace(".json", "");
                            ITEM_RESTRICTIONS.put(id, restriction);
                            LOGGER.info("Loaded item restriction: {}", id);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error loading item restriction from {}: {}", path, e.getMessage());
                    }
                });
                
            LOGGER.info("Loaded {} item restrictions", ITEM_RESTRICTIONS.size());
        } catch (IOException e) {
            LOGGER.error("Error loading item restrictions: {}", e.getMessage());
        }
    }
    
    /**
     * Creates an example file for restrictions
     */
    private static void createExampleFile(Path directory) throws IOException {
        Path examplePath = directory.resolve("example.json");
        
        if (!Files.exists(examplePath)) {
            // Use correct JSON as example
            String exampleJson = "{\n" +
                "  \"type\": \"iska_utils:stage_item\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"restrictions\": [\n" +
                "    {\n" +
                "      \"stages_logic\": \"AND\",\n" +
                "      \"stages\": [\n" +
                "          {\"stage_type\": \"player\", \"stage\": \"curio_basic\", \"is\": false},\n" +
                "          {\"stage_type\": \"world\", \"stage\": \"curio_dim_nether\", \"is\": false}\n" +
                "      ],\n" +
                "      \"containers_whitelist\": true,\n" +
                "      \"containers_list\": [\n" +
                "        \"top.theillusivec4.curios.common.inventory.container.CuriosContainer\"\n" +
                "      ],\n" +
                "      \"items\":[\n" +
                "        \"minecraft:structure_void\",\n" +
                "        \"#minecraft:trim_templates\"\n" +
                "      ],\n" +
                "      \"consequence\":\"drop\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
            
            Files.writeString(examplePath, exampleJson);
            LOGGER.info("Created example item restriction file: {}", examplePath);
        }
    }
    
    /**
     * Checks a container when opened by a player
     */
    public static void checkContainer(AbstractContainerMenu container, Player player) {
        if (!(player instanceof ServerPlayer) || player.level().isClientSide()) {
            return;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        Level level = player.level();
        
        LOGGER.debug("Checking container for player {}: {}", player.getName().getString(), container.getClass().getName());
        
        if (ITEM_RESTRICTIONS.isEmpty()) {
            LOGGER.debug("No item restrictions loaded, skipping check");
            return;
        }
        
        for (Map.Entry<String, StageItemRestriction> entry : ITEM_RESTRICTIONS.entrySet()) {
            String restrictionId = entry.getKey();
            StageItemRestriction restriction = entry.getValue();
            
            for (StageItemRule rule : restriction.restrictions) {
                // Check stage conditions
                boolean stageConditionsMet = checkStageConditions(rule, serverPlayer, level);
                
                if (!stageConditionsMet) {
                    continue;
                }
                
                // Check if container is in whitelist/blacklist
                String containerClassName = container.getClass().getName();
                boolean isContainerAffected = isContainerAffected(rule, containerClassName);
                
                if (isContainerAffected) {
                    // Apply consequence to items
                    LOGGER.info("Applying consequence '{}' to items in container {} for player {}", 
                        rule.consequence, containerClassName, player.getName().getString());
                    applyItemConsequences(rule, serverPlayer, container);
                    
                    // Notify player about the restriction
                    switch (rule.consequence.toLowerCase()) {
                        case "drop":
                            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.iska_utils.item_restriction.dropped"), true);
                            break;
                        case "delete":
                            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.iska_utils.item_restriction.deleted"), true);
                            break;
                        case "return":
                            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.iska_utils.item_restriction.returned"), true);
                            break;
                    }
                }
            }
        }
    }
    
    /**
     * Checks if a container is affected by the rule
     */
    private static boolean isContainerAffected(StageItemRule rule, String containerClassName) {
        if (rule.containersWhitelist) {
            // If whitelist, container must be in the list
            return rule.containersList.contains(containerClassName);
        } else {
            // If blacklist, container must not be in the list
            return rule.containersList.isEmpty() || !rule.containersList.contains(containerClassName);
        }
    }
    
    /**
     * Checks stage conditions
     */
    private static boolean checkStageConditions(StageItemRule rule, ServerPlayer player, Level level) {
        if (rule.stagesLogic.equals("AND")) {
            // All conditions must be true
            for (StageCondition condition : rule.stages) {
                boolean hasStage = checkSingleStageCondition(condition, player, level);
                if (hasStage != condition.is) {
                    return false;
                }
            }
            return true;
        } else if (rule.stagesLogic.equals("OR")) {
            // At least one condition must be true
            for (StageCondition condition : rule.stages) {
                boolean hasStage = checkSingleStageCondition(condition, player, level);
                if (hasStage == condition.is) {
                    return true;
                }
            }
            return false;
        } else if (rule.stagesLogic.equals("DEF_OR")) {
            // Custom DEF_OR logic
            // Check conditions based on specific subconditions in "if"
            if (rule.ifConditions != null && !rule.ifConditions.isEmpty()) {
                for (StageItemIfRule ifRule : rule.ifConditions) {
                    if (checkIfRuleConditions(ifRule, rule.stages, player, level)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (rule.stagesLogic.equals("DEF_AND")) {
            // Custom DEF_AND logic
            // All conditions in "if" must be true
            if (rule.ifConditions != null && !rule.ifConditions.isEmpty()) {
                boolean allValid = true;
                for (StageItemIfRule ifRule : rule.ifConditions) {
                    if (!checkIfRuleConditions(ifRule, rule.stages, player, level)) {
                        allValid = false;
                        break;
                    }
                }
                return allValid;
            }
            return false;
        }
        
        // Default: no conditions means always active
        return rule.stages.isEmpty();
    }
    
    /**
     * Checks a single stage condition
     */
    private static boolean checkSingleStageCondition(StageCondition condition, ServerPlayer player, Level level) {
        if ("player".equals(condition.stageType)) {
            return StageRegistry.playerHasStage(player, condition.stage);
        } else if ("world".equals(condition.stageType)) {
            return StageRegistry.worldHasStage(level, condition.stage);
        }
        return false;
    }
    
    /**
     * Checks conditions of an if rule
     */
    private static boolean checkIfRuleConditions(StageItemIfRule ifRule, List<StageCondition> allStages, ServerPlayer player, Level level) {
        // If no conditions specified, rule is always valid
        if (ifRule.conditions.isEmpty()) {
            return true;
        }
        
        // Check each condition index
        for (int conditionIndex : ifRule.conditions) {
            if (conditionIndex < 0 || conditionIndex >= allStages.size()) {
                // Invalid condition index
                LOGGER.warn("Invalid condition index: {} (max: {})", conditionIndex, allStages.size() - 1);
                return false;
            }
            
            // Check if condition is satisfied based on "is" value
            StageCondition condition = allStages.get(conditionIndex);
            boolean hasStage = checkSingleStageCondition(condition, player, level);
            
            // If condition is not satisfied, rule is not valid
            if (hasStage != condition.is) {
                return false;
            }
        }
        
        // All conditions are valid
        return true;
    }
    
    /**
     * Applies consequences to items
     */
    private static void applyItemConsequences(StageItemRule rule, ServerPlayer player, AbstractContainerMenu container) {
        List<Integer> slotsToAffect = new ArrayList<>();
        
        // Find all slots containing target items
        for (int i = 0; i < container.slots.size(); i++) {
            ItemStack stack = container.slots.get(i).getItem();
            if (!stack.isEmpty() && isItemAffected(stack.getItem(), rule.items)) {
                slotsToAffect.add(i);
            }
        }
        
        // Apply consequence to all affected slots
        for (int slot : slotsToAffect) {
            ItemStack stack = container.slots.get(slot).getItem().copy();
            
            switch (rule.consequence.toLowerCase()) {
                case "drop":
                    // Drop item on the ground
                    player.drop(stack, false);
                    container.slots.get(slot).set(ItemStack.EMPTY);
                    break;
                    
                case "delete":
                    // Delete the item
                    container.slots.get(slot).set(ItemStack.EMPTY);
                    break;
                    
                case "return":
                    // Return item to player inventory if possible
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    container.slots.get(slot).set(ItemStack.EMPTY);
                    break;
                    
                default:
                    // Default does nothing
                    break;
            }
        }
        
        // Update container if any items were modified
        if (!slotsToAffect.isEmpty()) {
            container.broadcastChanges();
        }
    }
    
    /**
     * Checks if an item is affected by the rule
     */
    private static boolean isItemAffected(Item item, List<String> itemsList) {
        ResourceLocation itemId = ResourceUtil.getKey(item);
        String itemIdStr = itemId.toString();
        
        for (String entry : itemsList) {
            if (entry.startsWith("#")) {
                // It's an item tag
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null && item.builtInRegistryHolder().is(ItemTags.create(tagId))) {
                    return true;
                }
            } else {
                // It's a direct item ID
                if (itemIdStr.equals(entry)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Parses a restriction from a JSON object
     */
    private static StageItemRestriction parseRestriction(JsonObject json) {
        StageItemRestriction restriction = new StageItemRestriction();
        restriction.overwritable = json.has("overwritable") && json.get("overwritable").getAsBoolean();
        
        if (json.has("restrictions") && json.get("restrictions").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("restrictions")) {
                if (element.isJsonObject()) {
                    JsonObject ruleObj = element.getAsJsonObject();
                    StageItemRule rule = new StageItemRule();
                    
                    // Parse stage logic
                    rule.stagesLogic = ruleObj.has("stages_logic") ? ruleObj.get("stages_logic").getAsString() : "AND";
                    
                    // Parse stage conditions
                    if (ruleObj.has("stages") && ruleObj.get("stages").isJsonArray()) {
                        for (JsonElement stageElement : ruleObj.getAsJsonArray("stages")) {
                            if (stageElement.isJsonObject()) {
                                JsonObject stageObj = stageElement.getAsJsonObject();
                                StageCondition condition = new StageCondition();
                                condition.stageType = stageObj.has("stage_type") ? stageObj.get("stage_type").getAsString() : "player";
                                condition.stage = stageObj.has("stage") ? stageObj.get("stage").getAsString() : "";
                                condition.is = !stageObj.has("is") || stageObj.get("is").getAsBoolean();
                                rule.stages.add(condition);
                            }
                        }
                    }
                    
                    // Parse container whitelist
                    String containerWhitelistKey = "containers_whitelist";
                    if (ruleObj.has(containerWhitelistKey)) {
                        rule.containersWhitelist = ruleObj.get(containerWhitelistKey).getAsBoolean();
                    } else {
                        rule.containersWhitelist = true; // Default
                    }
                    
                    // Parse container list
                    if (ruleObj.has("containers_list") && ruleObj.get("containers_list").isJsonArray()) {
                        for (JsonElement containerElement : ruleObj.getAsJsonArray("containers_list")) {
                            if (containerElement.isJsonPrimitive()) {
                                String containerClass = containerElement.getAsString();
                                rule.containersList.add(containerClass);
                            }
                        }
                    }
                    
                    // Parse item list
                    if (ruleObj.has("items") && ruleObj.get("items").isJsonArray()) {
                        for (JsonElement itemElement : ruleObj.getAsJsonArray("items")) {
                            if (itemElement.isJsonPrimitive()) {
                                String itemId = itemElement.getAsString();
                                rule.items.add(itemId);
                            }
                        }
                    }
                    
                    // Parse consequence
                    rule.consequence = ruleObj.has("consequence") ? ruleObj.get("consequence").getAsString() : "drop";
                    
                    // Parse "if" conditions for DEF_OR and DEF_AND logics
                    if (("DEF_OR".equals(rule.stagesLogic) || "DEF_AND".equals(rule.stagesLogic)) 
                            && ruleObj.has("if") && ruleObj.get("if").isJsonArray()) {
                        for (JsonElement ifElement : ruleObj.getAsJsonArray("if")) {
                            if (ifElement.isJsonObject()) {
                                JsonObject ifObj = ifElement.getAsJsonObject();
                                StageItemIfRule ifRule = new StageItemIfRule();
                                
                                // Parse condition indices
                                if (ifObj.has("conditions") && ifObj.get("conditions").isJsonArray()) {
                                    for (JsonElement condElement : ifObj.getAsJsonArray("conditions")) {
                                        if (condElement.isJsonPrimitive()) {
                                            int conditionIndex = condElement.getAsInt();
                                            ifRule.conditions.add(conditionIndex);
                                        }
                                    }
                                }
                                
                                // Parse container list
                                if (ifObj.has("containers_list") && ifObj.get("containers_list").isJsonArray()) {
                                    for (JsonElement containerElement : ifObj.getAsJsonArray("containers_list")) {
                                        if (containerElement.isJsonPrimitive()) {
                                            String containerClass = containerElement.getAsString();
                                            ifRule.containersList.add(containerClass);
                                        }
                                    }
                                }
                                
                                // Parse item list
                                if (ifObj.has("items") && ifObj.get("items").isJsonArray()) {
                                    for (JsonElement itemElement : ifObj.getAsJsonArray("items")) {
                                        if (itemElement.isJsonPrimitive()) {
                                            String itemId = itemElement.getAsString();
                                            ifRule.items.add(itemId);
                                        }
                                    }
                                }
                                
                                // Parse consequence
                                ifRule.consequence = ifObj.has("consequence") ? ifObj.get("consequence").getAsString() : rule.consequence;
                                
                                rule.ifConditions.add(ifRule);
                            }
                        }
                    }
                    
                    restriction.restrictions.add(rule);
                }
            }
        }
        
        return restriction;
    }
    
    // Classes to represent JSON structures
    
    /**
     * Represents a complete item restriction
     */
    public static class StageItemRestriction {
        public boolean overwritable = true;
        public List<StageItemRule> restrictions = new ArrayList<>();
    }
    
    /**
     * Represents a restriction rule
     */
    public static class StageItemRule {
        public String stagesLogic = "AND";
        public List<StageCondition> stages = new ArrayList<>();
        public boolean containersWhitelist = true;
        public List<String> containersList = new ArrayList<>();
        public List<String> items = new ArrayList<>();
        public String consequence = "drop";
        public List<StageItemIfRule> ifConditions = new ArrayList<>();
    }
    
    /**
     * Represents an if condition for DEF_OR logic
     */
    public static class StageItemIfRule {
        public List<Integer> conditions = new ArrayList<>();
        public List<String> containersList = new ArrayList<>();
        public List<String> items = new ArrayList<>();
        public String consequence = "drop";
    }
    
    /**
     * Represents a stage condition
     */
    public static class StageCondition {
        public String stageType = "player";
        public String stage = "";
        public boolean is = true;
    }
} 