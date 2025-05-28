package net.unfamily.iskautils.iska_utils_stages;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.Config;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
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
            Path stageItemsPath = externalScriptsPath.resolve("stage_items");
            ensureDirectoryExists(stageItemsPath);
            createReadmeFile(stageItemsPath);
            
            StageItemHandler.loadItemRestrictions(stageItemsPath);
            
            initialized = true;
            LOGGER.info("Item stage system successfully initialized (path: {})", stageItemsPath);
        } catch (Exception e) {
            LOGGER.error("Error initializing item stage system: {}", e.getMessage());
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
     */
    private static void createReadmeFile(Path directory) throws IOException {
        Path readmePath = directory.resolve("README.md");
        
        if (!Files.exists(readmePath)) {
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
                "      \"items\":[\n" +
                "        \"minecraft:example_item\",\n" +
                "        \"#minecraft:example_tag\"\n" +
                "      ],\n" +
                "      \"consequence\":\"drop\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                
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
                "      \"containers_whitelist\": true,\n" +
                "      \"if\":[\n" +
                "        {\n" +
                "          \"conditions\":[0],\n" +
                "          \"containers_list\": [\n" +
                "              \"net.minecraft.world.inventory.CraftingMenu\"\n" +
                "          ],\n" +
                "          \"items\":[\n" +
                "            \"minecraft:diamond_sword\"\n" +
                "          ],\n" +
                "          \"consequence\":\"drop\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"conditions\":[1],\n" +
                "          \"containers_list\": [\n" +
                "              \"net.minecraft.world.inventory.EnderChestMenu\"\n" +
                "          ],\n" +
                "          \"items\":[\n" +
                "            \"minecraft:nether_star\"\n" +
                "          ],\n" +
                "          \"consequence\":\"delete\"\n" +
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
                
                "## Possible Consequences\n\n" +
                "- `drop`: Drops the item on the ground\n" +
                "- `delete`: Completely removes the item\n" +
                "- `return`: Attempts to return the item to the player's inventory, or drops it if there's no space\n\n" +
                
                "## Reloading Configurations\n\n" +
                "Configurations are loaded at server startup and can be reloaded using the `/reload` command.\n\n" +
                
                "## Messages\n\n" +
                "The system will display a message to the player when an item restriction is applied. This helps players understand why certain items are being removed from containers they access. The message will indicate whether items were dropped, deleted, or returned to inventory based on the restriction settings.\n";
            
            Files.writeString(readmePath, readmeContent);
            LOGGER.info("Created README file for item stages: {}", readmePath);
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
     * Reloads item restrictions (useful for admin commands)
     */
    public static void reloadItemRestrictions() {
        try {
            Path externalScriptsPath = Paths.get(Config.externalScriptsPath);
            Path stageItemsPath = externalScriptsPath.resolve("stage_items");
            ensureDirectoryExists(stageItemsPath);
            StageItemHandler.loadItemRestrictions(stageItemsPath);
            LOGGER.info("Item restrictions reloaded (path: {})", stageItemsPath);
        } catch (Exception e) {
            LOGGER.error("Error reloading item restrictions: {}", e.getMessage());
        }
    }
} 