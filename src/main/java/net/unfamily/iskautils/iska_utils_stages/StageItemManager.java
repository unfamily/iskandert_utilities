package net.unfamily.iskautils.iska_utils_stages;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.Config;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages events and configuration for the item stage system
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class StageItemManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized = false;
    
    /**
     * Initializes the item stage system
     */
    public static void initialize(FMLCommonSetupEvent event) {
        if (initialized) {
            return;
        }
        
        try {
            // Load item restrictions using configured path
            Path externalScriptsPath = Paths.get(Config.externalScriptsPath);
            Path stageItemsPath = externalScriptsPath.resolve("iska_utils_stage_items");
            ensureDirectoryExists(stageItemsPath);
            createReadmeFile(stageItemsPath, true); // Always create/update README
            
            StageItemHandler.loadItemRestrictions(stageItemsPath);
            
            initialized = true;
            LOGGER.info("Item stage system successfully initialized (path: {})", stageItemsPath);
        } catch (Exception e) {
            LOGGER.error("Error initializing item stage system: {}", e.getMessage());
        }
    }
    
    /**
     * Handles container open events
     */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!initialized) {
            return;
        }
        
        AbstractContainerMenu container = event.getContainer();
        Player player = event.getEntity();
        
        try {
            // Check and apply item restrictions
            StageItemHandler.checkContainer(container, player);
        } catch (Exception e) {
            LOGGER.error("Error checking container: {}", e.getMessage());
        }
    }
    
    /**
     * Handles right-click item interactions
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!initialized || event.getLevel().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        
        try {
            if (StageItemHandler.shouldBlockRightClick(player, itemStack)) {
                event.setCanceled(true);
            }
            
            // Check main/off hand
            checkAndApplyHandRestrictions(player);
        } catch (Exception e) {
            LOGGER.error("Error checking right-click restriction: {}", e.getMessage());
        }
    }
    
    /**
     * Handles right-click block interactions
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!initialized || event.getLevel().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        
        try {
            if (StageItemHandler.shouldBlockRightClick(player, itemStack)) {
                event.setCanceled(true);
            }
            
            // Check main/off hand
            checkAndApplyHandRestrictions(player);
        } catch (Exception e) {
            LOGGER.error("Error checking right-click block restriction: {}", e.getMessage());
        }
    }
    
    /**
     * Handles left-click block interactions
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!initialized || event.getLevel().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        
        try {
            if (StageItemHandler.shouldBlockLeftClick(player, itemStack)) {
                event.setCanceled(true);
                
                // Notify the player
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.iska_utils.item_restriction.blocked"), true);
                }
            }
            
            // Check main/off hand
            checkAndApplyHandRestrictions(player);
        } catch (Exception e) {
            LOGGER.error("Error checking left-click restriction: {}", e.getMessage());
        }
    }
    
    /**
     * Handles equipment change events for main/off hand checking
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!initialized || event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        try {
            checkAndApplyHandRestrictions((Player) event.getEntity());
        } catch (Exception e) {
            LOGGER.error("Error checking equipment change: {}", e.getMessage());
        }
    }
    
    /**
     * Handles entity attack (left-click on entity)
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!initialized || event.getEntity().level().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        
        try {
            if (StageItemHandler.shouldBlockLeftClick(player, itemStack)) {
                event.setCanceled(true);
            }
            
            // Check main/off hand
            checkAndApplyHandRestrictions(player);
        } catch (Exception e) {
            LOGGER.error("Error checking attack entity restriction: {}", e.getMessage());
        }
    }
    
    /**
     * Helper method to check and apply restrictions for main/off hand
     */
    private static void checkAndApplyHandRestrictions(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Check main hand
            ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!mainHandItem.isEmpty()) {
                String mainHandConsequence = StageItemHandler.checkMainHandRestriction(player, mainHandItem);
                if (mainHandConsequence != null) {
                    StageItemHandler.applyHandConsequence(serverPlayer, InteractionHand.MAIN_HAND, mainHandConsequence);
                }
            }
            
            // Check off hand
            ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!offHandItem.isEmpty()) {
                String offHandConsequence = StageItemHandler.checkOffHandRestriction(player, offHandItem);
                if (offHandConsequence != null) {
                    StageItemHandler.applyHandConsequence(serverPlayer, InteractionHand.OFF_HAND, offHandConsequence);
                }
            }
        }
    }
    
    /**
     * Reloads item restrictions (useful for admin commands)
     */
    public static void reloadItemRestrictions() {
        try {
            Path externalScriptsPath = Paths.get(Config.externalScriptsPath);
            Path stageItemsPath = externalScriptsPath.resolve("iska_utils_stage_items");
            ensureDirectoryExists(stageItemsPath);
            createReadmeFile(stageItemsPath, true); // Always update README on reload
            StageItemHandler.loadItemRestrictions(stageItemsPath);
            LOGGER.info("Item restrictions reloaded (path: {})", stageItemsPath);
        } catch (Exception e) {
            LOGGER.error("Error reloading item restrictions: {}", e.getMessage());
        }
    }
    
    /**
     * Ensures the directory exists, creating it if necessary
     */
    private static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            LOGGER.info("Created item stages directory: {}", directory);
        }
    }
    
    /**
     * Creates a README.md file with instructions on using the system
     * @param forceUpdate If true, always update the README even if it exists
     */
    private static void createReadmeFile(Path directory, boolean forceUpdate) throws IOException {
        Path readmePath = directory.resolve("README.md");
        
        if (forceUpdate || !Files.exists(readmePath)) {
            String readmeContent = "# Stage Item System\n\n" +
                "This system allows you to define restrictions on items based on player or world stages.\n\n" +
                
                "## JSON File Format\n\n" +
                "JSON files should follow this format:\n\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:stage_item\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"restrictions\": [\n" +
                "    {\n" +
                "      \"stages_logic\": \"AND\",\n" +
                "      \"stages\": [\n" +
                "          {\"stage_type\": \"player\", \"stage\": \"example_stage\", \"is\": false},\n" +
                "          {\"stage_type\": \"world\", \"stage\": \"example_world_stage\", \"is\": false}\n" +
                "      ],\n" +
                "      \"containers_whitelist\": true,\n" +
                "      \"containers_list\": [\n" +
                "        \"net.namespace.inventory.container.ExampleContainer\"\n" +
                "      ],\n" +
                "      \"other_case\":[\n" +
                "        \"right_click\",\n" +
                "        \"left_click\",\n" +
                "        \"main_hand\",\n" +
                "        \"off_hand\"\n" +
                "      ],\n" +
                "      \"items\":[\n" +
                "        \"minecraft:example_item\",\n" +
                "        \"#c:example_tag\",\n" +
                "        \"@rep_ae2_bridge\"\n" +
                "      ],\n" +
                "      \"consequence\":\"block_drop\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                
                "## Exclude Example\n\n" +
                "Use the optional `exclude` array to exempt specific items from the rule (same format as `items`):\n\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:stage_item\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"restrictions\": [\n" +
                "    {\n" +
                "      \"stages_logic\": \"AND\",\n" +
                "      \"stages\": [{\"stage_type\": \"player\", \"stage\": \"no_advanced\", \"is\": false}],\n" +
                "      \"items\": [\"@minecraft\", \"#c:ingots\", \"iska_utils:dolly\"],\n" +
                "      \"exclude\": [\"minecraft:diamond\", \"#c:ingots/lead\", \"@iska_utils\"],\n" +
                "      \"consequence\": \"drop\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "Here, diamond, lead (from the tag), and all iska_utils items are exempt and will not be dropped.\n\n" +
                
                "## DEF_OR Example\n\n" +
                "You can use DEF_OR logic to apply different restrictions based on specific conditions:\n\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:stage_item\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"restrictions\": [\n" +
                "    {\n" +
                "      \"stages_logic\": \"DEF_OR\",\n" +
                "      \"stages\": [\n" +
                "          {\"stage_type\": \"player\", \"stage\": \"example_stage\", \"is\": false},\n" +
                "          {\"stage_type\": \"world\", \"stage\": \"example_world_stage\", \"is\": false},\n" +
                "          {\"stage_type\": \"player\", \"stage\": \"admin\", \"is\": true}\n" +
                "      ],\n" +
                "      \"if\":[\n" +
                "        {\n" +
                "          \"conditions\":[0],\n" +
                "          \"containers_whitelist\": true,\n" +
                "          \"containers_list\": [\n" +
                "              \"net.namespace.inventory.container.ExampleCraftingContainer\"\n" +
                "          ],\n" +
                "          \"items\":[\n" +
                "            \"minecraft:diamond_sword\"\n" +
                "          ],\n" +
                "          \"consequence\":\"drop\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"conditions\":[1],\n" +
                "          \"containers_list\": [\n" +
                "              \"net.namespace.inventory.container.ExampleChestContainer\"\n" +
                "          ],\n" +
                "          \"other_case\":[\n" +
                "              \"right_click\",\n" +
                "              \"left_click\"\n" +
                "          ],\n" +
                "          \"items\":[\n" +
                "            \"minecraft:nether_star\"\n" +
                "          ],\n" +
                "          \"consequence\":\"block_delete\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +

                "A specific case to apply this to is with a curio item; you can drop/delete the curio item if the player doesn't have the required stage.\n" +
                "The container class for Curios in Minecraft 1.21.1 is `top.theillusivec4.curios.common.inventory.container.CuriosContainer`.\n" +
                "Note that the Curios mod author has a habit of changing the container class name with each Minecraft version update.\n\n" +
                
                "## Available Logic Types\n\n" +
                "- `AND`: All conditions must be true\n" +
                "- `OR`: At least one condition must be true\n" +
                "- `DEF_OR`: Custom logic using the `if` attribute, where any condition set can match\n" +
                "- `DEF_AND`: Custom logic where all conditions in `if` must be true\n\n" +
                
                "## Container Filtering\n\n" +
                "Container filtering is controlled by two parameters:\n\n" +
                "- `containers_whitelist`: When `true`, only the containers in the list are affected; when `false`, all containers EXCEPT those in the list are affected\n" +
                "- `containers_list`: A list of container class names to include or exclude based on the whitelist setting\n\n" +
                
                "## Items List Format\n\n" +
                "Each entry in the `items` array can be:\n\n" +
                "- **Item ID**: exact match, e.g. `minecraft:diamond_sword`\n" +
                "- **#tag**: item tag, e.g. `#c:ingots` matches all items in that tag\n" +
                "- **@modid**: all items from a mod, e.g. `@rep_ae2_bridge` matches every item whose namespace is `rep_ae2_bridge`\n\n" +
                "The optional **`exclude`** array (default empty) uses the same format. Any item matching an exclude entry is exempt from the rule even if it matches `items`. Example: `items` has `@minecraft`, `#c:ingots`, `iska_utils:dolly` and `exclude` has `minecraft:diamond`, `#c:ingots/lead`, `@iska_utils` — then diamond, lead ingots, and all iska_utils items are not affected.\n\n" +
                
                "## Other Case Contexts\n\n" +
                "The `other_case` field allows specifying additional contexts where restrictions should be applied:\n\n" +
                "- `right_click`: Block right-click usage of the item\n" +
                "- `left_click`: Block left-click usage of the item\n" +
                "- `main_hand`: Apply restriction when item is in main hand\n" +
                "- `off_hand`: Apply restriction when item is in off hand\n\n" +
                "These are all optional and can be used in any combination, in both regular restrictions and inside `if` conditions.\n\n" +
                
                "## Possible Consequences\n\n" +
                "- `drop`: Drops the item on the ground\n" +
                "- `delete`: Completely removes the item\n" +
                "- `block`: Blocks usage of the item without removing it (applies to other_case contexts)\n" +
                "- `block_drop`: Blocks usage of the item and drops it (for non-container contexts specified in other_case)\n" +
                "- `block_delete`: Blocks usage of the item and deletes it (for non-container contexts specified in other_case)\n\n" +
                
                "## Reloading Configurations\n\n" +
                "Configurations are loaded at server startup and can be reloaded using the `/reload` command.\n\n" +
                
                "## Messages\n\n" +
                "The system will display a message to the player when an item restriction is applied. This helps players understand why certain items are being removed from containers they access. The message will indicate whether items were dropped, deleted, or blocked based on the restriction settings.\n";
            
            Files.writeString(readmePath, readmeContent);
            LOGGER.info("Updated README file for item stages: {}", readmePath);
        }
    }
} 