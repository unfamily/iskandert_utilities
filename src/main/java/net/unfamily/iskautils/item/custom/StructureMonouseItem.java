package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.client.MarkRenderer;
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.structure.StructureLoader;
import net.unfamily.iskautils.structure.StructureMonouseDefinition;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item for placing specific structures without consuming materials from inventory
 * and giving items to the player after placement
 */
public class StructureMonouseItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Colors for markers
    public static final int PREVIEW_COLOR = 0x804444FF; // Semi-transparent blue
    public static final int CONFLICT_COLOR = 0x80FF4444; // Semi-transparent red
    public static final int SUCCESS_COLOR = 0x8044FF44; // Semi-transparent green
    
    // Double-click system constants
    public static final String LAST_CLICK_TIME_KEY = "LastClickTime";
    public static final String LAST_CLICK_POS_KEY = "LastClickPos";
    private static final long DOUBLE_CLICK_TIMEOUT = 5000; // 5 seconds in milliseconds
    
    // Fixed definition assigned at registration
    private final StructureMonouseDefinition definition;
    
    public StructureMonouseItem(Properties properties, StructureMonouseDefinition definition) {
        super(properties);
        this.definition = definition;
    }
    
    /**
     * Gets the definition for this monouse item
     */
    public StructureMonouseDefinition getDefinition() {
        return definition;
    }
    
    /**
     * Gets the ID of this monouse item
     */
    public String getDefinitionId() {
        return definition.getId();
    }
    
    /**
     * Gets the current rotation of the structure
     */
    public static int getRotation(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                 net.minecraft.world.item.component.CustomData.EMPTY)
                   .copyTag().getInt("Rotation");
    }
    
    /**
     * Sets the rotation of the structure
     */
    private static void setRotation(ItemStack stack, int rotation) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                                              net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt("Rotation", rotation);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                 net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    /**
     * Gets the last click time for double-click detection
     */
    private static long getLastClickTime(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                 net.minecraft.world.item.component.CustomData.EMPTY)
                   .copyTag().getLong(LAST_CLICK_TIME_KEY);
    }
    
    /**
     * Sets the last click time for double-click detection
     */
    private static void setLastClickTime(ItemStack stack, long time) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                                              net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putLong(LAST_CLICK_TIME_KEY, time);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                 net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    /**
     * Gets the last click position for double-click detection
     */
    private static long getLastClickPos(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                 net.minecraft.world.item.component.CustomData.EMPTY)
                   .copyTag().getLong(LAST_CLICK_POS_KEY);
    }
    
    /**
     * Sets the last click position for double-click detection
     */
    private static void setLastClickPos(ItemStack stack, long pos) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                                              net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putLong(LAST_CLICK_POS_KEY, pos);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                 net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    /**
     * Resets the double-click timer to prevent accidental placements
     */
    private static void resetDoubleClickTimer(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                                              net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putLong(LAST_CLICK_TIME_KEY, 0L);
        tag.putLong(LAST_CLICK_POS_KEY, 0L);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                 net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        // Prevents damage to entities
        return true;
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().above(); // Place one block above the clicked block
        
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        
        // Get the structure to place
        StructureDefinition structure = StructureLoader.getStructure(definition.getPlaceName());
        if (structure == null) {
            player.displayClientMessage(Component.translatable("item.iska_utils.structure_monouse.message.structure_not_found", definition.getPlaceName()), true);
            return InteractionResult.FAIL;
        }
        
        // Shift + right click disabled (rotation now on left click like Structure Placer)
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        
        // Normal right-click: preview or place based on double-click system
        long currentTime = System.currentTimeMillis();
        long lastClickTime = getLastClickTime(stack);
        long lastClickPos = getLastClickPos(stack);
        long currentPosHash = pos.asLong();
        
        boolean isDoubleClick = (currentTime - lastClickTime < DOUBLE_CLICK_TIMEOUT) && 
                               (currentPosHash == lastClickPos);
        
        if (isDoubleClick) {
            // Second click within timeout: place structure (no force placement allowed)
            int rotation = getRotation(stack);
            boolean success = attemptStructurePlacement(serverPlayer, pos, structure, rotation);
            
            if (success) {
                // Give the items specified in the definition
                giveItemsToPlayer(serverPlayer);
                
                String structureName = structure.getName() != null ? structure.getName() : structure.getId();
                player.displayClientMessage(Component.translatable("item.iska_utils.structure_monouse.message.placed_successfully", structureName), true);
                
                // Show green success markers
                showSuccessMarkers(serverPlayer, pos, structure, rotation);
                
                // Consume the item (monouse = single use)
                stack.shrink(1);
                
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.translatable("item.iska_utils.structure_monouse.message.placement_failed"), true);
                return InteractionResult.FAIL;
            }
        } else {
            // First click: show preview
            int rotation = getRotation(stack);
            showPreview(serverPlayer, pos, structure, rotation);
            
            // Save click time and position for double-click detection
            setLastClickTime(stack, currentTime);
            setLastClickPos(stack, currentPosHash);
            
            return InteractionResult.SUCCESS;
        }
    }
    
    /**
     * Shows a preview of the structure with blue markers
     */
    private void showPreview(ServerPlayer player, BlockPos centerPos, StructureDefinition structure, int rotation) {
        Map<BlockPos, String> positions = calculateStructurePositions(centerPos, structure, rotation);
        
        int blueMarkers = 0;
        int redMarkers = 0;
        
        for (Map.Entry<BlockPos, String> entry : positions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState currentState = player.level().getBlockState(blockPos);
            
            if (canReplaceBlock(currentState, structure)) {
                // Empty/replaceable space: blue marker at block position (5 seconds, no text)
                MarkRenderer.getInstance().addBillboardMarker(blockPos, PREVIEW_COLOR, 100); // 5 seconds
                blueMarkers++;
            } else {
                // Occupied space: red marker at block position (5 seconds, no text)
                MarkRenderer.getInstance().addBillboardMarker(blockPos, CONFLICT_COLOR, 100); // 5 seconds
                redMarkers++;
            }
        }
        
        // Inform the player about conflicts and empty spaces
        String structureName = structure.getName() != null ? structure.getName() : structure.getId();
        player.displayClientMessage(Component.translatable("item.iska_utils.structure_monouse.message.preview", structureName), true);
        
        if (redMarkers > 0) {
            player.displayClientMessage(Component.literal("§a" + blueMarkers + " §7empty spaces, §c" + redMarkers + " §7occupied spaces"), true);
        } else {
            player.displayClientMessage(Component.literal("§a" + blueMarkers + " §7empty spaces, all clear!"), true);
        }
    }
    
    /**
     * Attempts to place the structure with material checking (no force placement)
     */
    private boolean attemptStructurePlacement(ServerPlayer player, BlockPos centerPos, StructureDefinition structure, int rotation) {
        try {
            // Calculate structure block positions with rotation
            Map<BlockPos, String> blockPositions = calculateStructurePositions(centerPos, structure, rotation);
            Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
            
            // First pass: check if placement is possible (space available)
            for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
                BlockPos blockPos = entry.getKey();
                String character = entry.getValue();
                List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
                
                if (blockDefs != null && !blockDefs.isEmpty()) {
                    // Check if position is valid and replaceable
                    if (!player.level().isInWorldBounds(blockPos)) {
                        return false; // Out of world bounds
                    }
                    
                    BlockState existingState = player.level().getBlockState(blockPos);
                    
                    // Use the same replacement logic as preview
                    if (!canReplaceBlock(existingState, structure)) {
                        return false; // Cannot replace existing block
                    }
                }
            }
            
            // Second pass: place all blocks with delay between layers
            placeStructureWithLayerDelay(player, blockPositions, key, structure);
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Error placing structure for monouse item {}: {}", definition.getId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Places the structure with 5 tick delay between layers
     */
    private void placeStructureWithLayerDelay(ServerPlayer player, Map<BlockPos, String> blockPositions, 
                                            Map<String, List<StructureDefinition.BlockDefinition>> key, 
                                            StructureDefinition structure) {
        // Check if structure has slower flag enabled
        if (structure.isSlower()) {
            placeStructureWithBlockDelay(player, blockPositions, key, structure);
            return;
        }
        
        // Group blocks by layer (Y coordinate)
        Map<Integer, Map<BlockPos, String>> blocksByLayer = new HashMap<>();
        
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos pos = entry.getKey();
            int layer = pos.getY();
            blocksByLayer.computeIfAbsent(layer, k -> new HashMap<>()).put(pos, entry.getValue());
        }
        
        // Place first layer immediately
        List<Integer> sortedLayers = blocksByLayer.keySet().stream().sorted().toList();
        
        if (!sortedLayers.isEmpty()) {
            int firstLayer = sortedLayers.get(0);
            placeLayer(player, blocksByLayer.get(firstLayer), key, structure);
            
            // Schedule remaining layers with 5 tick delay between each
            for (int i = 1; i < sortedLayers.size(); i++) {
                final int layerY = sortedLayers.get(i);
                final int delayTicks = i * 5; // 5 ticks for each layer after the first
                final Map<BlockPos, String> layerBlocks = blocksByLayer.get(layerY);
                
                // Schedule layer placement after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(delayTicks * 50); // Convert ticks to milliseconds
                        ((ServerLevel) player.level()).getServer().execute(() -> {
                            placeLayer(player, layerBlocks, key, structure);
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    /**
     * Places the structure with 5 tick delay between each individual block (when slower is enabled)
     */
    private void placeStructureWithBlockDelay(ServerPlayer player, Map<BlockPos, String> blockPositions, 
                                            Map<String, List<StructureDefinition.BlockDefinition>> key, 
                                            StructureDefinition structure) {
        List<Map.Entry<BlockPos, String>> blockList = new ArrayList<>(blockPositions.entrySet());
        
        // Place first block immediately
        if (!blockList.isEmpty()) {
            placeSingleBlock(player, blockList.get(0), key, structure);
            
            // Schedule remaining blocks with 5 tick delay between each
            for (int i = 1; i < blockList.size(); i++) {
                final Map.Entry<BlockPos, String> blockEntry = blockList.get(i);
                final int delayTicks = i * 5; // 5 ticks between each block
                
                // Schedule block placement after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(delayTicks * 50); // Convert ticks to milliseconds
                        ((ServerLevel) player.level()).getServer().execute(() -> {
                            placeSingleBlock(player, blockEntry, key, structure);
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    /**
     * Places a single layer of blocks
     */
    private void placeLayer(ServerPlayer player, Map<BlockPos, String> layerBlocks, 
                          Map<String, List<StructureDefinition.BlockDefinition>> key, 
                          StructureDefinition structure) {
        for (Map.Entry<BlockPos, String> entry : layerBlocks.entrySet()) {
            placeSingleBlock(player, entry, key, structure);
        }
    }
    
    /**
     * Places a single block with all necessary checks
     */
    private void placeSingleBlock(ServerPlayer player, Map.Entry<BlockPos, String> blockEntry, 
                                Map<String, List<StructureDefinition.BlockDefinition>> key, 
                                StructureDefinition structure) {
        BlockPos blockPos = blockEntry.getKey();
        String character = blockEntry.getValue();
        List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
        
        if (blockDefs != null && !blockDefs.isEmpty()) {
            // Use the first available block definition
            StructureDefinition.BlockDefinition blockDef = blockDefs.get(0);
            
            // Get block from registry
            ResourceLocation blockLocation = ResourceLocation.parse(blockDef.getBlock());
            Block block = BuiltInRegistries.BLOCK.get(blockLocation);
            
            if (block != null && block != Blocks.AIR) {
                BlockState blockState = block.defaultBlockState();
                
                // Apply properties if specified
                if (blockDef.getProperties() != null) {
                    for (Map.Entry<String, String> propEntry : blockDef.getProperties().entrySet()) {
                        try {
                            blockState = applyBlockProperty(blockState, propEntry.getKey(), propEntry.getValue());
                        } catch (Exception e) {
                            // Ignore invalid properties
                        }
                    }
                }
                
                // Check if we should place as player
                if (structure.isPlaceAsPlayer()) {
                    // Place as if done by player - create temporary ItemStack and use BlockItem
                    try {
                        Item blockItem = block.asItem();
                        if (blockItem != null && blockItem != Items.AIR) {
                            ItemStack blockStack = new ItemStack(blockItem);
                            
                            // Create a fake UseOnContext to simulate player placement
                            var context = new net.minecraft.world.item.context.UseOnContext(
                                player, 
                                net.minecraft.world.InteractionHand.MAIN_HAND, 
                                new net.minecraft.world.phys.BlockHitResult(
                                    net.minecraft.world.phys.Vec3.atCenterOf(blockPos), 
                                    net.minecraft.core.Direction.UP, 
                                    blockPos, // Use the actual position, not below
                                    false
                                )
                            );
                            
                            // Try to use BlockItem.useOn to place it like a player would
                            if (blockItem instanceof net.minecraft.world.item.BlockItem blockItemInstance) {
                                blockItemInstance.place(new net.minecraft.world.item.context.BlockPlaceContext(context));
                            } else {
                                // Fallback to normal placement
                                ((ServerLevel) player.level()).setBlock(blockPos, blockState, 3);
                            }
                        } else {
                            // Fallback to normal placement
                            ((ServerLevel) player.level()).setBlock(blockPos, blockState, 3);
                        }
                    } catch (Exception e) {
                        // If player-like placement fails, fallback to normal placement
                        LOGGER.debug("Player-like placement failed for {}, using normal placement: {}", blockDef.getBlock(), e.getMessage());
                        ((ServerLevel) player.level()).setBlock(blockPos, blockState, 3);
                    }
                } else {
                    // Normal placement
                    ((ServerLevel) player.level()).setBlock(blockPos, blockState, 3);
                }
            }
        }
    }

    /**
     * Calculates positions of all structure blocks with rotation
     */
    private Map<BlockPos, String> calculateStructurePositions(BlockPos centerPos, StructureDefinition structure, int rotation) {
        Map<BlockPos, String> positions = new HashMap<>();
        
        String[][][][] pattern = structure.getPattern();
        if (pattern == null) return positions;
        
        // Find structure center (@ symbol)
        BlockPos relativeCenter = structure.findCenter();
        if (relativeCenter == null) relativeCenter = BlockPos.ZERO;
        
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    
                    if (cellChars != null) {
                        for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                            String character = cellChars[charIndex];
                            
                            // Skip empty spaces
                            if (character == null || character.equals(" ")) continue;
                            
                            // If it's @, check if it's defined in the key
                            if (character.equals("@")) {
                                Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
                                if (key == null || !key.containsKey("@")) {
                                    // @ is not defined in key, treat as empty space
                                    continue;
                                }
                                // If we get here, @ is defined in key, so process as normal block
                            }
                            
                            int originalX = x;
                            int originalY = y;
                            int originalZ = z * cellChars.length + charIndex;
                            
                            // Calculate relative position from center @
                            int relX = originalX - relativeCenter.getX();
                            int relY = originalY - relativeCenter.getY();
                            int relZ = originalZ - relativeCenter.getZ();
                            
                            // Apply rotation to relative coordinates
                            BlockPos rotatedRelativePos = applyRotation(relX, relY, relZ, rotation);
                            
                            // Calculate final position in world
                            BlockPos blockPos = centerPos.offset(rotatedRelativePos.getX(), rotatedRelativePos.getY(), rotatedRelativePos.getZ());
                            positions.put(blockPos, character);
                        }
                    }
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Applies rotation transformation to coordinates
     */
    private static BlockPos applyRotation(int x, int y, int z, int rotation) {
        return switch (rotation) {
            case 90 -> new BlockPos(-z, y, x);   // 90° clockwise
            case 180 -> new BlockPos(-x, y, -z); // 180°
            case 270 -> new BlockPos(z, y, -x);  // 270° clockwise (90° counter-clockwise)
            default -> new BlockPos(x, y, z);    // 0° (no rotation)
        };
    }
    
    /**
     * Applies a block property with the given name and value
     */
    private BlockState applyBlockProperty(BlockState state, String propertyName, String value) {
        for (var property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return applyPropertyValue(state, property, value);
            }
        }
        return state;
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> BlockState applyPropertyValue(BlockState state, 
                                                                   net.minecraft.world.level.block.state.properties.Property<T> property, 
                                                                   String value) {
        try {
            var optionalValue = property.getValue(value);
            if (optionalValue.isPresent()) {
                return state.setValue(property, optionalValue.get());
            }
        } catch (Exception e) {
            // Ignore conversion errors
        }
        return state;
    }
    
    /**
     * Gives the specified items to the player
     */
    private void giveItemsToPlayer(ServerPlayer player) {
        for (StructureMonouseDefinition.GiveItem giveItem : definition.getGiveItems()) {
            try {
                ResourceLocation itemLocation = ResourceLocation.parse(giveItem.getItem());
                Item item = BuiltInRegistries.ITEM.get(itemLocation);
                
                if (item != null) {
                    ItemStack stack = new ItemStack(item, giveItem.getCount());
                    
                    // Try to add to inventory, drop if full
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error giving item {} to player: {}", giveItem.getItem(), e.getMessage());
            }
        }
    }
    
    /**
     * Shows green success markers for the placed structure
     */
    private void showSuccessMarkers(ServerPlayer player, BlockPos centerPos, StructureDefinition structure, int rotation) {
        Map<BlockPos, String> positions = calculateStructurePositions(centerPos, structure, rotation);
        
        // Show green markers for 5 seconds without text
        for (BlockPos pos : positions.keySet()) {
            MarkRenderer.getInstance().addBillboardMarker(pos, SUCCESS_COLOR, 100); // 5 seconds, without text
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Struttura
        StructureDefinition structure = StructureLoader.getStructure(definition.getPlaceName());
        String displayName = definition.getPlaceName(); // fallback to ID
        if (structure != null && structure.getName() != null && !structure.getName().isEmpty()) {
            displayName = structure.getName();
        }
        tooltip.add(Component.translatable("item.iska_utils.structure_monouse.tooltip.structure", displayName));
        
        // Direzione impostata
        int rotation = getRotation(stack);
        String rotationText = switch (rotation) {
            case 0 -> Component.translatable("direction.iska_utils.north").getString();
            case 90 -> Component.translatable("direction.iska_utils.east").getString(); 
            case 180 -> Component.translatable("direction.iska_utils.south").getString();
            case 270 -> Component.translatable("direction.iska_utils.west").getString();
            default -> rotation + "°";
        };
        tooltip.add(Component.translatable("item.iska_utils.structure_monouse.tooltip.rotation", rotationText));
        
        // Fatto che è monouso
        tooltip.add(Component.translatable("item.iska_utils.structure_monouse.tooltip.single_use"));
        
        // Lista degli oggetti che otterrai
        if (!definition.getGiveItems().isEmpty()) {
            tooltip.add(Component.translatable("item.iska_utils.structure_monouse.tooltip.will_give"));
            for (StructureMonouseDefinition.GiveItem giveItem : definition.getGiveItems()) {
                tooltip.add(Component.translatable("item.iska_utils.structure_monouse.tooltip.give_item", giveItem.getCount(), giveItem.getItem()));
            }
        }
        
        tooltip.add(Component.literal(""));
        
        // Istruzioni d'uso (tutte tranne shift+right click per forzare)
        tooltip.add(Component.translatable("item.iska_utils.structure_monouse.tooltip.right_click"));
        tooltip.add(Component.translatable("item.iska_utils.structure_monouse.tooltip.double_click"));
        tooltip.add(Component.translatable("item.iska_utils.structure_monouse.tooltip.left_click_rotate"));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Prevent right-click in air usage
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    /**
     * Check if a block can be replaced
     */
    private boolean canReplaceBlock(BlockState state, StructureDefinition structure) {
        // Air can always be replaced
        if (state.isAir()) return true;
        
        // Check structure can_replace list
        List<String> canReplace = structure.getCanReplace();
        if (canReplace != null) {
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            
            for (String replaceableId : canReplace) {
                if (replaceableId.equals(blockId)) {
                    return true;
                }
                
                // Handle special cases
                if (handleSpecialReplaceableCase(replaceableId, state)) {
                    return true;
                }
                
                // Handle tags with # prefix
                if (replaceableId.startsWith("#") && handleTagReplacement(replaceableId, state)) {
                    return true;
                }
            }
            
            return false; // If there's a can_replace list and block is not in list, it cannot be replaced
        }
        
        return false;
    }
    
    /**
     * Handles special replacement cases like $replaceable, $fluids, etc.
     */
    private boolean handleSpecialReplaceableCase(String replaceableId, BlockState existingState) {
        return switch (replaceableId.toLowerCase()) {
            case "$replaceable" -> existingState.canBeReplaced();
            case "$fluids", "$fluid" -> existingState.getFluidState().isSource() || !existingState.getFluidState().isEmpty();
            case "$air" -> existingState.isAir();
            case "$water" -> existingState.is(net.minecraft.world.level.block.Blocks.WATER);
            case "$lava" -> existingState.is(net.minecraft.world.level.block.Blocks.LAVA);
            case "$plants", "$plant" -> existingState.is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES) || 
                                      existingState.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS) ||
                                      existingState.is(net.minecraft.tags.BlockTags.TALL_FLOWERS) ||
                                      existingState.is(net.minecraft.tags.BlockTags.SAPLINGS);
            case "$dirt" -> existingState.is(net.minecraft.tags.BlockTags.DIRT);
            case "$logs", "$log" -> existingState.is(net.minecraft.tags.BlockTags.LOGS);
            case "$leaves" -> existingState.is(net.minecraft.tags.BlockTags.LEAVES);
            case "$stone" -> existingState.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES) ||
                           existingState.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES);
            case "$ores", "$ore" -> existingState.is(net.minecraft.tags.BlockTags.COAL_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.IRON_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.GOLD_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.DIAMOND_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.EMERALD_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.LAPIS_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.REDSTONE_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.COPPER_ORES);
            default -> false;
        };
    }
    
    /**
     * Handles tag-based replacement with # prefix
     */
    private boolean handleTagReplacement(String tagId, BlockState existingState) {
        try {
            // Remove # prefix
            String cleanTagId = tagId.substring(1);
            
            // Parse as ResourceLocation
            net.minecraft.resources.ResourceLocation tagLocation = net.minecraft.resources.ResourceLocation.parse(cleanTagId);
            
            // Get tag from registry
            net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> blockTag = 
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tagLocation);
            
            // Check if block is in the tag
            return existingState.is(blockTag);
            
        } catch (Exception e) {
            // Silent failure for invalid tags in item context
            return false;
        }
    }
} 