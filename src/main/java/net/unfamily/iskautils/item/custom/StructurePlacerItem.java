package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.unfamily.iskautils.client.MarkRenderer;
import net.unfamily.iskautils.network.packet.StructurePlacerGuiOpenC2SPacket;
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.structure.StructureLoader;
import net.unfamily.iskautils.structure.StructurePlacer;
import net.unfamily.iskautils.util.ModUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item for placing structures in the world with selection GUI
 */
public class StructurePlacerItem extends Item {
    
    public static final String SELECTED_STRUCTURE_KEY = "selected_structure";
    public static final String PREVIEW_MODE_KEY = "preview_mode";
    public static final String LAST_PREVIEW_POS_KEY = "last_preview_pos";
    public static final String LAST_CLICK_TIME_KEY = "last_click_time";
    public static final String LAST_CLICK_POS_KEY = "last_click_pos";
    public static final String ROTATION_KEY = "rotation";
    
    // Colors for markers
    public static final int PREVIEW_COLOR = 0x804444FF; // Semi-transparent blue
    public static final int CONFLICT_COLOR = 0x80FF4444; // Semi-transparent red
    public static final int SUCCESS_COLOR = 0x8044FF44; // Semi-transparent green
    
    public StructurePlacerItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Use text system for structure selection
            openStructureSelectionGui(serverPlayer, stack);
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        // Prevents damage to entities when used for rotation
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
        
        String structureId = getSelectedStructure(stack);
        if (structureId == null || structureId.isEmpty()) {
            player.displayClientMessage(Component.translatable("item.iska_utils.structure_placer.no_structure_selected"), true);
            return InteractionResult.FAIL;
        }
        
        StructureDefinition structure = StructureLoader.getStructure(structureId);
        if (structure == null) {
            player.displayClientMessage(Component.literal("§cStructure not found: " + structureId), true);
            return InteractionResult.FAIL;
        }
        
        // New system: check if this is the second click within 5 seconds at the same position
        long currentTime = level.getGameTime();
        long lastClickTime = getLastClickTime(stack);
        BlockPos lastClickPos = getLastClickPos(stack);
        
        boolean isSecondClick = (currentTime - lastClickTime <= 100) && // 5 seconds = 100 ticks
                               pos.equals(lastClickPos);
        
        if (isSecondClick) {
            // Second click: attempt to place the structure
            return attemptStructurePlacement(serverPlayer, pos, structure, stack);
        } else {
            // First click: show preview with specific markers
            showDetailedPreview(serverPlayer, pos, structure, stack);
            
            // Save click time and position
            saveLastClick(stack, currentTime, pos);
            
            return InteractionResult.SUCCESS;
        }
    }
    
    /**
     * Opens the structure selection GUI
     */
    private void openStructureSelectionGui(ServerPlayer player, ItemStack stack) {
        // Opens the Structure Placer GUI
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return Component.translatable("gui.iska_utils.structure_placer.title");
            }
            
            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, 
                    net.minecraft.world.entity.player.Inventory playerInventory, 
                    net.minecraft.world.entity.player.Player player) {
                return new net.unfamily.iskautils.client.gui.StructurePlacerMenu(containerId, playerInventory);
            }
        });
    }
    
    /**
     * Shows detailed structure preview with blue markers for empty spaces and red markers for occupied spaces
     */
    private void showDetailedPreview(ServerPlayer player, BlockPos centerPos, StructureDefinition structure, ItemStack stack) {
        // Remove previous markers if they exist
        clearPreviousMarkers(player, stack);
        
        // Calculate structure block positions
        Map<BlockPos, String> blockPositions = calculateStructurePositions(centerPos, structure, stack);
        
        if (blockPositions.isEmpty()) {
            player.displayClientMessage(Component.literal("§cError: No blocks to place in structure!"), true);
            return;
        }
        
        // Show specific markers for each position
        int blueMarkers = 0;
        int redMarkers = 0;
        
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState currentState = player.level().getBlockState(blockPos);
            
            if (canReplaceBlock(currentState, structure)) {
                // Empty/replaceable space: blue marker at block position (15 seconds, no text)
                MarkRenderer.getInstance().addBillboardMarker(blockPos, PREVIEW_COLOR, 300); // 15 seconds
                blueMarkers++;
            } else {
                // Occupied space: red marker at block position (15 seconds, no text)
                MarkRenderer.getInstance().addBillboardMarker(blockPos, CONFLICT_COLOR, 300); // 15 seconds
                redMarkers++;
            }
        }
        
        // Save position for future reference
        saveLastPreviewPos(stack, centerPos);
        
        // Inform the player
        String structureName = structure.getName() != null ? structure.getName() : structure.getId();
        player.displayClientMessage(Component.literal("§bPreview: §f" + structureName), true);
        player.displayClientMessage(Component.literal("§a" + blueMarkers + " §7empty spaces, §c" + redMarkers + " §7occupied spaces"), true);
        player.displayClientMessage(Component.literal("§7Click again within 5 seconds to place"), true);
        
        if (redMarkers > 0 && structure.isCanForce()) {
            player.displayClientMessage(Component.literal("§7Hold shift to force placement over occupied spaces"), true);
        }
    }
    
    /**
     * Attempts to place the structure by checking materials
     */
    private InteractionResult attemptStructurePlacement(ServerPlayer player, BlockPos centerPos, StructureDefinition structure, ItemStack stack) {
        // Calculate positions and create a specific block allocation map
        Map<BlockPos, String> blockPositions = calculateStructurePositions(centerPos, structure, stack);
        Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
        
        // Create an allocation map that tracks exactly which block to use for each position
        Map<BlockPos, AllocatedBlock> blockAllocation = new HashMap<>();
        Map<String, Integer> missingMaterials = new HashMap<>();
        
        // Track alternatives for each material type
        Map<String, List<StructureDefinition.BlockDefinition>> alternativesMap = new HashMap<>();
        
        // Allocate specific blocks from inventory
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            String character = entry.getValue();
            List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
            
            if (blockDefs != null && !blockDefs.isEmpty()) {
                AllocatedBlock allocated = allocateBlockFromInventory(player, blockDefs);
                if (allocated != null) {
                    blockAllocation.put(blockPos, allocated);
                } else {
                    // Not found, add to missing materials
                    String displayName = blockDefs.get(0).getDisplay() != null && !blockDefs.get(0).getDisplay().isEmpty() 
                        ? blockDefs.get(0).getDisplay() 
                        : blockDefs.get(0).getBlock();
                    missingMaterials.merge(displayName, 1, Integer::sum);
                    
                    // Track available alternatives for this material
                    alternativesMap.put(displayName, blockDefs);
                }
            }
        }
        
        if (!missingMaterials.isEmpty()) {
            // Restore allocated items that were not used
            restoreAllocatedBlocks(player, blockAllocation.values());
            showMissingMaterialsWithAlternatives(player, missingMaterials, alternativesMap);
            return InteractionResult.FAIL;
        }
        
        // Check space - check at actual position, not above a block
        boolean hasConflicts = false;
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState currentState = player.level().getBlockState(blockPos);
            
            if (!canReplaceBlock(currentState, structure)) {
                hasConflicts = true;
                break;
            }
        }
        
        // If there are conflicts, show red markers
        if (hasConflicts) {
            if (!structure.isCanForce()) {
                showConflictMarkers(player, blockPositions, structure);
                restoreAllocatedBlocks(player, blockAllocation.values());
                player.displayClientMessage(Component.literal("§cSpace is occupied! Structure cannot be placed."), true);
                return InteractionResult.FAIL;
            } else {
                // If can force, ask for confirmation
                if (!player.isShiftKeyDown()) {
                    showConflictMarkers(player, blockPositions, structure);
                    restoreAllocatedBlocks(player, blockAllocation.values());
                    player.displayClientMessage(Component.literal("§cSpace is occupied! Use shift+right-click to force placement."), true);
                    return InteractionResult.FAIL;
                }
            }
        }
        
        // Materials have already been consumed during allocation, now place the structure
        boolean isForced = hasConflicts && structure.isCanForce() && player.isShiftKeyDown();
        boolean success = placeStructureWithAllocatedBlocks((ServerLevel) player.level(), blockAllocation, structure, player, isForced);
        
        if (success) {
            // Show green success markers UN ONE BLOCK MORE UP ONLY FOR THE ACTUALLY PLACED BLOCKS
            showSuccessMarkers((ServerLevel) player.level(), blockAllocation, structure, isForced);
            
            String structureName = structure.getName() != null ? structure.getName() : structure.getId();
            if (isForced) {
                player.displayClientMessage(Component.literal("§aStructure §f" + structureName + " §apartially placed! (Some blocks skipped)"), true);
            } else {
                player.displayClientMessage(Component.literal("§aStructure §f" + structureName + " §aplaced successfully!"), true);
            }
            return InteractionResult.SUCCESS;
        } else {
            // If placement fails, restore blocks
            restoreAllocatedBlocks(player, blockAllocation.values());
            player.displayClientMessage(Component.literal("§cFailed to place structure!"), true);
            return InteractionResult.FAIL;
        }
    }
    
    /**
     * Calculate all block positions of the structure with rotation around the center @
     */
    private Map<BlockPos, String> calculateStructurePositions(BlockPos centerPos, StructureDefinition structure, ItemStack stack) {
        Map<BlockPos, String> positions = new HashMap<>();
        
        String[][][][] pattern = structure.getPattern();
        if (pattern == null) return positions;
        
        // Find structure center (symbol @)
        BlockPos relativeCenter = structure.findCenter();
        if (relativeCenter == null) relativeCenter = BlockPos.ZERO;
        
        // Get rotation from ItemStack
        int rotation = getRotation(stack);
        
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
                                    // @ is not defined in the key, treat it as empty space
                                    continue;
                                }
                                // If we get here, @ is defined in the key, so process it as a normal block
                            }
                                int originalX = x;
                                int originalY = y;
                                int originalZ = z * cellChars.length + charIndex;
                                
                                // Calculate relative position relative to the center @
                                int relX = originalX - relativeCenter.getX();
                                int relY = originalY - relativeCenter.getY();
                                int relZ = originalZ - relativeCenter.getZ();
                                
                                // Apply rotation to relative coordinates
                                BlockPos rotatedRelativePos = applyRotation(relX, relY, relZ, rotation);
                                
                                // Calculate final position in the world
                                BlockPos blockPos = centerPos.offset(
                                    rotatedRelativePos.getX(), 
                                    rotatedRelativePos.getY(), 
                                    rotatedRelativePos.getZ()
                                );
                                
                                positions.put(blockPos, character);
                        }
                    }
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Apply rotation to relative coordinates
     */
    private BlockPos applyRotation(int x, int y, int z, int rotation) {
        // Rotation only horizontal (X and Z), Y remains the same
        int newX = x;
        int newZ = z;
        
        switch (rotation) {
            case 0:   // North (no rotation)
                newX = x;
                newZ = z;
                break;
            case 90:  // East (90° clockwise)
                newX = -z;
                newZ = x;
                break;
            case 180: // South (180°)
                newX = -x;
                newZ = -z;
                break;
            case 270: // West (270° clockwise = 90° counterclockwise)
                newX = z;
                newZ = -x;
                break;
        }
        
        return new BlockPos(newX, y, newZ);
    }
    
    /**
     * Calculate required materials for the structure (grouped by display name)
     */
    private Map<String, MaterialRequirement> calculateRequiredMaterials(BlockPos centerPos, StructureDefinition structure) {
        Map<String, MaterialRequirement> materials = new HashMap<>();
        
        Map<BlockPos, String> positions = calculateStructurePositions(centerPos, structure, ItemStack.EMPTY);
        Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
        
        for (String character : positions.values()) {
            List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
            if (blockDefs != null && !blockDefs.isEmpty()) {
                // Use display name or first block ID as key
                final String displayName = blockDefs.get(0).getDisplay() != null && !blockDefs.get(0).getDisplay().isEmpty() 
                    ? blockDefs.get(0).getDisplay() 
                    : blockDefs.get(0).getBlock();
                
                if (displayName != null) {
                    MaterialRequirement req = materials.computeIfAbsent(displayName, k -> new MaterialRequirement(displayName, blockDefs));
                    req.incrementCount();
                }
            }
        }
        
        return materials;
    }
    
    /**
     * Helper class to group material requirements with alternatives
     */
    private static class MaterialRequirement {
        private final String displayName;
        private final List<StructureDefinition.BlockDefinition> alternatives;
        private int count = 0;
        
        public MaterialRequirement(String displayName, List<StructureDefinition.BlockDefinition> alternatives) {
            this.displayName = displayName;
            this.alternatives = alternatives;
        }
        
        public void incrementCount() {
            this.count++;
        }
        
        public String getDisplayName() { return displayName; }
        public List<StructureDefinition.BlockDefinition> getAlternatives() { return alternatives; }
        public int getCount() { return count; }
    }
    
    /**
     * Helper class to track an allocated block from inventory
     */
    private static class AllocatedBlock {
        private final StructureDefinition.BlockDefinition blockDefinition;
        private final int inventorySlot;
        private final ItemStack originalStack;
        private final ItemStack consumedStack;
        
        public AllocatedBlock(StructureDefinition.BlockDefinition blockDefinition, int inventorySlot, 
                            ItemStack originalStack, ItemStack consumedStack) {
            this.blockDefinition = blockDefinition;
            this.inventorySlot = inventorySlot;
            this.originalStack = originalStack;
            this.consumedStack = consumedStack;
        }
        
        public StructureDefinition.BlockDefinition getBlockDefinition() { return blockDefinition; }
        public int getInventorySlot() { return inventorySlot; }
        public ItemStack getOriginalStack() { return originalStack; }
        public ItemStack getConsumedStack() { return consumedStack; }
    }
    
    /**
     * Allocate a single block from player inventory
     */
    private AllocatedBlock allocateBlockFromInventory(ServerPlayer player, List<StructureDefinition.BlockDefinition> blockDefinitions) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            
            if (!stack.isEmpty()) {
                // Get block from item if possible
                Block block = null;
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                
                if (stack.getItem() instanceof BlockItem blockItem) {
                    block = blockItem.getBlock();
                } else {
                    // Try to find a matching block for item that are not BlockItem
                    try {
                        net.minecraft.resources.ResourceLocation itemLocation = net.minecraft.resources.ResourceLocation.parse(itemId);
                        net.minecraft.resources.ResourceLocation blockLocation = 
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(itemLocation.getNamespace(), itemLocation.getPath());
                        
                        if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(blockLocation)) {
                            block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                        }
                    } catch (Exception e) {
                        // Ignore if unable to find matching block
                    }
                }
                
                if (block != null) {
                    String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
                    
                    // Check if this block matches any of the required definitions
                    for (StructureDefinition.BlockDefinition blockDef : blockDefinitions) {
                        if (blockDef.getBlock() != null && blockDef.getBlock().equals(blockId)) {
                            // Found! Consume an item and create allocation
                            ItemStack consumedStack = stack.copy();
                            consumedStack.setCount(1);
                            
                            stack.shrink(1);
                            if (stack.isEmpty()) {
                                player.getInventory().items.set(i, ItemStack.EMPTY);
                            }
                            
                            player.getInventory().setChanged();
                            return new AllocatedBlock(blockDef, i, stack.copy(), consumedStack);
                        }
                    }
                }
            }
        }
        
        return null; // Not found
    }
    
    /**
     * Restore allocated blocks to player inventory
     */
    private void restoreAllocatedBlocks(ServerPlayer player, java.util.Collection<AllocatedBlock> allocatedBlocks) {
        for (AllocatedBlock allocated : allocatedBlocks) {
            ItemStack consumedStack = allocated.getConsumedStack();
            
            // Try to put item back in original slot
            ItemStack currentStack = player.getInventory().items.get(allocated.getInventorySlot());
            if (currentStack.isEmpty()) {
                player.getInventory().items.set(allocated.getInventorySlot(), consumedStack.copy());
            } else if (ItemStack.isSameItemSameComponents(currentStack, consumedStack)) {
                currentStack.grow(consumedStack.getCount());
            } else {
                // If original slot is occupied, find another slot
                if (!player.getInventory().add(consumedStack.copy())) {
                    // If inventory is full, drop item
                    player.drop(consumedStack.copy(), false);
                }
            }
        }
        
        player.getInventory().setChanged();
    }
    
    /**
     * Place structure using specifically allocated blocks with delay between layers
     */
    private boolean placeStructureWithAllocatedBlocks(ServerLevel level, Map<BlockPos, AllocatedBlock> blockAllocation, 
                                                    StructureDefinition structure, ServerPlayer player, boolean isForced) {
        try {
            // Group blocks by layer (Y coordinate)
            Map<Integer, Map<BlockPos, AllocatedBlock>> blocksByLayer = new HashMap<>();
            
            for (Map.Entry<BlockPos, AllocatedBlock> entry : blockAllocation.entrySet()) {
                BlockPos pos = entry.getKey();
                int layer = pos.getY();
                blocksByLayer.computeIfAbsent(layer, k -> new HashMap<>()).put(pos, entry.getValue());
            }
            
            // Place first layer immediately
            Map<Integer, Boolean> layerResults = new HashMap<>();
            List<Integer> sortedLayers = blocksByLayer.keySet().stream().sorted().toList();
            
            if (!sortedLayers.isEmpty()) {
                int firstLayer = sortedLayers.get(0);
                boolean firstLayerSuccess = placeLayer(level, blocksByLayer.get(firstLayer), structure, player, isForced);
                layerResults.put(firstLayer, firstLayerSuccess);
                
                            // Schedule remaining layers with delay between each
            for (int i = 1; i < sortedLayers.size(); i++) {
                final int layerY = sortedLayers.get(i);
                final int delayTicks = i * 5; // 5 ticks per layer after the first
                final Map<BlockPos, AllocatedBlock> layerBlocks = blocksByLayer.get(layerY);
                    
                    // Schedule layer placement after delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(delayTicks * 50); // Convert ticks to milliseconds
                            level.getServer().execute(() -> {
                                boolean layerSuccess = placeLayer(level, layerBlocks, structure, player, isForced);
                                layerResults.put(layerY, layerSuccess);
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            }
            
            // Return true if at least the first layer was placed successfully
            return !layerResults.isEmpty() && layerResults.values().stream().anyMatch(success -> success);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Place a single layer of blocks
     */
    private boolean placeLayer(ServerLevel level, Map<BlockPos, AllocatedBlock> layerBlocks, 
                             StructureDefinition structure, ServerPlayer player, boolean isForced) {
        try {
            // Check if structure has slower flag enabled
            if (structure.isSlower()) {
                return placeLayerWithBlockDelay(level, layerBlocks, structure, player, isForced);
            }
            
            int placedBlocks = 0;
            Map<BlockPos, AllocatedBlock> skippedBlocks = new HashMap<>();
            
            for (Map.Entry<BlockPos, AllocatedBlock> entry : layerBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                AllocatedBlock allocated = entry.getValue();
                StructureDefinition.BlockDefinition blockDef = allocated.getBlockDefinition();
                
                // If in forced mode, check if position can be replaced
                if (isForced) {
                    BlockState currentState = level.getBlockState(pos);
                    if (!canReplaceBlock(currentState, structure)) {
                        // Skip this position and keep block for restoration
                        skippedBlocks.put(pos, allocated);
                        continue;
                    }
                }
                
                // Get block from registry
                net.minecraft.resources.ResourceLocation blockLocation = 
                    net.minecraft.resources.ResourceLocation.parse(blockDef.getBlock());
                Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                
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
                            if (blockItem != null && blockItem != net.minecraft.world.item.Items.AIR) {
                                ItemStack blockStack = new ItemStack(blockItem);
                                
                                // Create a fake UseOnContext to simulate player placement
                                var context = new net.minecraft.world.item.context.UseOnContext(
                                    player, 
                                    net.minecraft.world.InteractionHand.MAIN_HAND, 
                                    new net.minecraft.world.phys.BlockHitResult(
                                        net.minecraft.world.phys.Vec3.atCenterOf(pos), 
                                        net.minecraft.core.Direction.UP, 
                                        pos, // Use the actual position, not below
                                        false
                                    )
                                );
                                
                                // Try to use BlockItem.useOn to place it like a player would
                                if (blockItem instanceof net.minecraft.world.item.BlockItem blockItemInstance) {
                                    blockItemInstance.place(new net.minecraft.world.item.context.BlockPlaceContext(context));
                                } else {
                                    // Fallback to normal placement
                                    level.setBlock(pos, blockState, 3);
                                }
                            } else {
                                // Fallback to normal placement
                                level.setBlock(pos, blockState, 3);
                            }
                        } catch (Exception e) {
                            // If player-like placement fails, fallback to normal placement
                            level.setBlock(pos, blockState, 3);
                        }
                    } else {
                        // Normal placement
                        level.setBlock(pos, blockState, 3);
                    }
                    placedBlocks++;
                } else {
                    // Invalid block - keep for restoration
                    skippedBlocks.put(pos, allocated);
                }
            }
            
            // If in forced mode and there are skipped blocks, restore them in inventory
            if (isForced && !skippedBlocks.isEmpty()) {
                restoreAllocatedBlocks(player, skippedBlocks.values());
            }
            
            // Success if at least one block is placed
            return placedBlocks > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Place a layer with delay between each individual block (when slower is enabled)
     */
    private boolean placeLayerWithBlockDelay(ServerLevel level, Map<BlockPos, AllocatedBlock> layerBlocks, 
                                           StructureDefinition structure, ServerPlayer player, boolean isForced) {
        try {
            List<Map.Entry<BlockPos, AllocatedBlock>> blockList = new ArrayList<>(layerBlocks.entrySet());
            Map<BlockPos, AllocatedBlock> skippedBlocks = new HashMap<>();
            
            // Place first block immediately
            if (!blockList.isEmpty()) {
                boolean firstBlockPlaced = placeSingleBlock(level, blockList.get(0), structure, player, isForced, skippedBlocks);
                
                // Schedule remaining blocks with 5 tick delay between each
                for (int i = 1; i < blockList.size(); i++) {
                    final Map.Entry<BlockPos, AllocatedBlock> blockEntry = blockList.get(i);
                    final int delayTicks = i * 5; // 5 ticks between each block
                    
                    // Schedule block placement after delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(delayTicks * 50); // Convert ticks to milliseconds
                            level.getServer().execute(() -> {
                                placeSingleBlock(level, blockEntry, structure, player, isForced, skippedBlocks);
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
                
                // Restore skipped blocks at the end
                if (isForced && !skippedBlocks.isEmpty()) {
                    // Schedule restoration after all blocks are processed
                    int totalDelay = blockList.size() * 5 + 10; // Extra delay to ensure all blocks are processed
                    new Thread(() -> {
                        try {
                            Thread.sleep(totalDelay * 50);
                            level.getServer().execute(() -> {
                                restoreAllocatedBlocks(player, skippedBlocks.values());
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
                
                return firstBlockPlaced || !layerBlocks.isEmpty();
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Place a single block with all necessary checks
     */
    private boolean placeSingleBlock(ServerLevel level, Map.Entry<BlockPos, AllocatedBlock> blockEntry, 
                                   StructureDefinition structure, ServerPlayer player, boolean isForced,
                                   Map<BlockPos, AllocatedBlock> skippedBlocks) {
        try {
            BlockPos pos = blockEntry.getKey();
            AllocatedBlock allocated = blockEntry.getValue();
            StructureDefinition.BlockDefinition blockDef = allocated.getBlockDefinition();
            
            // If in forced mode, check if position can be replaced
            if (isForced) {
                BlockState currentState = level.getBlockState(pos);
                if (!canReplaceBlock(currentState, structure)) {
                    // Skip this position and keep block for restoration
                    synchronized (skippedBlocks) {
                        skippedBlocks.put(pos, allocated);
                    }
                    return false;
                }
            }
            
            // Get block from registry
            net.minecraft.resources.ResourceLocation blockLocation = 
                net.minecraft.resources.ResourceLocation.parse(blockDef.getBlock());
            Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
            
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
                        if (blockItem != null && blockItem != net.minecraft.world.item.Items.AIR) {
                            ItemStack blockStack = new ItemStack(blockItem);
                            
                            // Create a fake UseOnContext to simulate player placement
                            var context = new net.minecraft.world.item.context.UseOnContext(
                                player, 
                                net.minecraft.world.InteractionHand.MAIN_HAND, 
                                new net.minecraft.world.phys.BlockHitResult(
                                    net.minecraft.world.phys.Vec3.atCenterOf(pos), 
                                    net.minecraft.core.Direction.UP, 
                                    pos, // Use the actual position, not below
                                    false
                                )
                            );
                            
                            // Try to use BlockItem.useOn to place it like a player would
                            if (blockItem instanceof net.minecraft.world.item.BlockItem blockItemInstance) {
                                blockItemInstance.place(new net.minecraft.world.item.context.BlockPlaceContext(context));
                            } else {
                                // Fallback to normal placement
                                level.setBlock(pos, blockState, 3);
                            }
                        } else {
                            // Fallback to normal placement
                            level.setBlock(pos, blockState, 3);
                        }
                    } catch (Exception e) {
                        // If player-like placement fails, fallback to normal placement
                        level.setBlock(pos, blockState, 3);
                    }
                } else {
                    // Normal placement
                    level.setBlock(pos, blockState, 3);
                }
                return true;
            } else {
                // Invalid block - keep for restoration
                synchronized (skippedBlocks) {
                    skippedBlocks.put(pos, allocated);
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Apply a property to a BlockState
     */
    private BlockState applyBlockProperty(BlockState state, String propertyName, String value) {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return applyPropertyValue(state, property, value);
            }
        }
        return state; // Property not found, return original state
    }
    
    /**
     * Apply the value of a specific property
     */
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> BlockState applyPropertyValue(BlockState state, 
                                                                   net.minecraft.world.level.block.state.properties.Property<T> property, 
                                                                   String value) {
        try {
            java.util.Optional<T> optionalValue = property.getValue(value);
            if (optionalValue.isPresent()) {
                return state.setValue(property, optionalValue.get());
            }
        } catch (Exception e) {
            // Ignore invalid values
        }
        return state;
    }

    /**
     * Scan player inventory for available materials
     */
    private Map<String, Integer> scanPlayerInventory(ServerPlayer player, Map<String, MaterialRequirement> requiredMaterials) {
        Map<String, Integer> available = new HashMap<>();
        
        // Scan main inventory + hotbar
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                // Get block from item if possible
                Block block = null;
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                
                if (stack.getItem() instanceof BlockItem blockItem) {
                    block = blockItem.getBlock();
                } else {
                    // Try to find a matching block for item that are not BlockItem
                    // but that still correspond to a block (like lever, button, etc.)
                    try {
                        net.minecraft.resources.ResourceLocation itemLocation = net.minecraft.resources.ResourceLocation.parse(itemId);
                        net.minecraft.resources.ResourceLocation blockLocation = 
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(itemLocation.getNamespace(), itemLocation.getPath());
                        
                        if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(blockLocation)) {
                            block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                        }
                    } catch (Exception e) {
                        // Ignore if unable to find matching block
                    }
                }
                
                if (block != null) {
                    String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
                    
                    // Check if this block is one of the required alternatives
                    for (MaterialRequirement requirement : requiredMaterials.values()) {
                        for (StructureDefinition.BlockDefinition blockDef : requirement.getAlternatives()) {
                            if (blockDef.getBlock() != null && blockDef.getBlock().equals(blockId)) {
                                available.merge(requirement.getDisplayName(), stack.getCount(), Integer::sum);
                                break; // Exit inner loop once found
                            }
                        }
                    }
                }
            }
        }
        
        return available;
    }
    
    /**
     * Consume materials from player inventory based on requirements
     */
    private void consumeMaterialsFromRequirements(ServerPlayer player, Map<String, MaterialRequirement> requirements, Map<String, Integer> availableMaterials) {
        for (Map.Entry<String, MaterialRequirement> entry : requirements.entrySet()) {
            String displayName = entry.getKey();
            MaterialRequirement requirement = entry.getValue();
            int needed = requirement.getCount();
            int remaining = needed;
            
            // Search inventory slots
            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                
                if (!stack.isEmpty()) {
                    // Get block from item if possible
                    Block block = null;
                    String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    
                    if (stack.getItem() instanceof BlockItem blockItem) {
                        block = blockItem.getBlock();
                    } else {
                        // Try to find a matching block for item that are not BlockItem
                        try {
                            net.minecraft.resources.ResourceLocation itemLocation = net.minecraft.resources.ResourceLocation.parse(itemId);
                            net.minecraft.resources.ResourceLocation blockLocation = 
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(itemLocation.getNamespace(), itemLocation.getPath());
                            
                            if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(blockLocation)) {
                                block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                            }
                        } catch (Exception e) {
                            // Ignore if unable to find matching block
                        }
                    }
                    
                    if (block != null) {
                        String stackBlockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
                        
                        // Check if this block is one of the required alternatives
                        for (StructureDefinition.BlockDefinition blockDef : requirement.getAlternatives()) {
                            if (blockDef.getBlock() != null && blockDef.getBlock().equals(stackBlockId)) {
                                int toConsume = Math.min(remaining, stack.getCount());
                                stack.shrink(toConsume);
                                remaining -= toConsume;
                                
                                if (stack.isEmpty()) {
                                    player.getInventory().items.set(i, ItemStack.EMPTY);
                                }
                                break; // Exit inner loop of alternatives
                            }
                        }
                    }
                }
            }
        }
        
        // Update player inventory
        player.getInventory().setChanged();
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
            return canReplace.contains(blockId);
        }
        
        return false;
    }
    
    /**
     * Show red markers for conflicts
     */
    private void showConflictMarkers(ServerPlayer player, Map<BlockPos, String> positions, StructureDefinition structure) {
        for (Map.Entry<BlockPos, String> entry : positions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState currentState = player.level().getBlockState(blockPos);
            
            if (!canReplaceBlock(currentState, structure)) {
                // Marker at block position (15 seconds, no text)
                MarkRenderer.getInstance().addBillboardMarker(blockPos, CONFLICT_COLOR, 300); // 15 seconds
            }
        }
    }
    
    /**
     * Show green success markers only for actually placed blocks
     */
    private void showSuccessMarkers(ServerLevel level, Map<BlockPos, AllocatedBlock> blockAllocation, 
                                  StructureDefinition structure, boolean isForced) {
        for (Map.Entry<BlockPos, AllocatedBlock> entry : blockAllocation.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState currentState = level.getBlockState(pos);
            
            // If in forced mode, show markers only for blocks that were actually placed
            boolean wasPlaced = true;
            if (isForced) {
                // Check if at position there is still the original block (not placed)
                // or if there is the block we were supposed to place (placed)
                AllocatedBlock allocated = entry.getValue();
                try {
                    net.minecraft.resources.ResourceLocation blockLocation = 
                        net.minecraft.resources.ResourceLocation.parse(allocated.getBlockDefinition().getBlock());
                    Block expectedBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                    
                    // If current block is not the one we were supposed to place, it means it was skipped
                    wasPlaced = currentState.getBlock() == expectedBlock;
                } catch (Exception e) {
                    wasPlaced = false;
                }
            }
            
            if (wasPlaced) {
                // Success marker (15 seconds, no text)
                MarkRenderer.getInstance().addBillboardMarker(pos, SUCCESS_COLOR, 300); // 15 seconds
            }
        }
    }
    
    /**
     * Shows a simplified message of missing materials with specific alternatives
     */
    private void showMissingMaterialsMessageSimple(ServerPlayer player, Map<String, Integer> missingMaterials, int totalBlocks) {
        player.displayClientMessage(Component.literal("§cMissing materials:"), false);
        
        // For each missing material, find alternatives from allocation system
        // We no longer use calculateStructurePositions here as it's only needed for display names
        
        for (Map.Entry<String, Integer> entry : missingMaterials.entrySet()) {
            String displayName = entry.getKey();
            int missing = entry.getValue();
            
            // Use translated display name if available, otherwise use formatted name
            String translatedDisplayName = Component.translatable(displayName).getString();
            if (translatedDisplayName.equals(displayName)) {
                // If no translation, use formatted name
                translatedDisplayName = getFormattedDisplayName(displayName);
            }
            
            Component message = Component.literal("§c- " + missing + " §f")
                .append(Component.literal(translatedDisplayName));
            
            player.displayClientMessage(message, false);
        }
    }
    
    /**
     * Shows missing materials with all available alternatives
     */
    private void showMissingMaterialsWithAlternatives(ServerPlayer player, Map<String, Integer> missingMaterials, 
                                                     Map<String, List<StructureDefinition.BlockDefinition>> alternativesMap) {
        player.displayClientMessage(Component.literal("§cMissing materials:"), false);
        
        for (Map.Entry<String, Integer> entry : missingMaterials.entrySet()) {
            String displayName = entry.getKey();
            int missing = entry.getValue();
            
            // Show main name of group
            String translatedDisplayName = getFormattedDisplayName(displayName);
            Component message = Component.literal("§c- " + missing + " §f" + translatedDisplayName + ":");
            player.displayClientMessage(message, false);
            
            // Show all available alternatives
            List<StructureDefinition.BlockDefinition> alternatives = alternativesMap.get(displayName);
            if (alternatives != null) {
                for (StructureDefinition.BlockDefinition blockDef : alternatives) {
                    if (blockDef.getBlock() != null && blockExists(blockDef.getBlock())) {
                        String formattedBlockName = getFormattedBlockName(blockDef.getBlock());
                        player.displayClientMessage(Component.literal("  §a- " + formattedBlockName), false);
                    }
                }
            }
        }
    }
    
    /**
     * Remove previous markers
     */
    private void clearPreviousMarkers(ServerPlayer player, ItemStack stack) {
        // Markers expire automatically, but we could implement explicit removal
        // if needed in the future
    }
    
    /**
     * Shows a formatted message of missing materials
     */
    private void showMissingMaterialsMessage(ServerPlayer player, Map<String, Integer> missingMaterials, int totalBlocks, Map<String, MaterialRequirement> requirements) {
        // No longer shows "required_blocks" generic, but specific display names
        
        for (Map.Entry<String, Integer> entry : missingMaterials.entrySet()) {
            String displayName = entry.getKey();
            int missing = entry.getValue();
            
            // Use translated display name if available, otherwise use raw display name
            String translatedDisplayName = Component.translatable(displayName).getString();
            if (translatedDisplayName.equals(displayName)) {
                // If no translation, use formatted name
                translatedDisplayName = getTranslatedBlockName(displayName);
            }
            
            Component message = Component.literal("§c" + missing + " §f")
                .append(Component.literal(translatedDisplayName));
            
            player.displayClientMessage(message, false);
        }
        
        // Remove "total blocks needed" - now each group has its own specific name
        
        // Show available alternatives
        for (Map.Entry<String, MaterialRequirement> reqEntry : requirements.entrySet()) {
            MaterialRequirement requirement = reqEntry.getValue();
            if (requirement.getAlternatives().size() > 1) {
                String translatedDisplayName = Component.translatable(requirement.getDisplayName()).getString();
                if (translatedDisplayName.equals(requirement.getDisplayName())) {
                    translatedDisplayName = getTranslatedBlockName(requirement.getDisplayName());
                }
                
                player.displayClientMessage(Component.literal("  §7- alternatives for §f" + translatedDisplayName + "§7:"), false);
                for (StructureDefinition.BlockDefinition blockDef : requirement.getAlternatives()) {
                    if (blockDef.getBlock() != null) {
                        String translatedName = getTranslatedBlockName(blockDef.getBlock());
                        // Check if block exists in game
                        if (blockExists(blockDef.getBlock())) {
                            player.displayClientMessage(Component.literal("    §a- " + translatedName), false);
                        } else {
                            player.displayClientMessage(Component.literal("    §8- " + translatedName + " §7(not available)"), false);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check if a block exists in registry
     */
    private boolean blockExists(String blockId) {
        try {
            net.minecraft.resources.ResourceLocation resourceLocation = net.minecraft.resources.ResourceLocation.parse(blockId);
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(resourceLocation);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get translated name of a block from its ID
     */
    private String getTranslatedBlockName(String blockId) {
        return getFormattedBlockName(blockId);
    }
    
    /**
     * Format block name in "Mod Name: Block Name" format
     */
    private String getFormattedBlockName(String blockId) {
        try {
            // Convert block ID (namespace:name) to modname format
            String[] parts = blockId.split(":");
            if (parts.length == 2) {
                String namespace = parts[0];
                String blockName = parts[1];
                
                // Map common namespace to mod names
                String modName = switch (namespace) {
                    case "minecraft" -> "Minecraft";
                    case "iska_utils" -> "Iskandert's Utilities";
                    case "industrialforegoing" -> "Industrial Foregoing";
                    case "mob_grinding_utils" -> "Mob Grinding Utils";
                    case "thermal" -> "Thermal Series";
                    case "mekanism" -> "Mekanism";
                    case "create" -> "Create";
                    case "ae2" -> "Applied Energistics 2";
                    case "botania" -> "Botania";
                    case "tconstruct" -> "Tinkers' Construct";
                    default -> {
                        // Capitalize namespace as fallback
                        yield formatModName(namespace);
                    }
                };
                
                // Convert block name to readable format
                String readableName = formatBlockName(blockName);
                
                return modName + ": " + readableName;
            }
        } catch (Exception e) {
            // If fails, use original ID
        }
        
        return blockId;
    }
    
    /**
     * Format display name in correct format
     */
    private String getFormattedDisplayName(String displayName) {
        // If already a translation key, try to translate it
        if (displayName.contains(".")) {
            String translated = Component.translatable(displayName).getString();
            if (!translated.equals(displayName)) {
                return translated;
            }
        }
        
        // Otherwise format as a block ID
        return getFormattedBlockName(displayName);
    }
    
    /**
     * Format mod name from namespace
     */
    private String formatModName(String namespace) {
        if (namespace.length() <= 1) return namespace.toUpperCase();
        
        // Split by underscore and capitalize each word
        String[] parts = namespace.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (result.length() > 0) result.append(" ");
            if (part.length() > 0) {
                result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Format block name from its internal ID
     */
    private String formatBlockName(String blockName) {
        String readableName = blockName.replace("_", " ");
        String[] words = readableName.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            if (word.length() > 0) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
        }
        
        return result.toString();
    }
    
    // ===== NBT Helper Methods =====
    
    public static String getSelectedStructure(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.getString(SELECTED_STRUCTURE_KEY);
    }
    
    public static void setSelectedStructure(ItemStack stack, String structureId) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putString(SELECTED_STRUCTURE_KEY, structureId);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    public static boolean isPreviewMode(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.getBoolean(PREVIEW_MODE_KEY);
    }
    
    public static void setPreviewMode(ItemStack stack, boolean previewMode) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putBoolean(PREVIEW_MODE_KEY, previewMode);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    private void saveLastPreviewPos(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putLong(LAST_PREVIEW_POS_KEY, pos.asLong());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    private long getLastClickTime(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.getLong(LAST_CLICK_TIME_KEY);
    }
    
    private BlockPos getLastClickPos(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        long posLong = tag.getLong(LAST_CLICK_POS_KEY);
        if (posLong == 0) return null;
        return BlockPos.of(posLong);
    }
    
    private void saveLastClick(ItemStack stack, long time, BlockPos pos) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putLong(LAST_CLICK_TIME_KEY, time);
        tag.putLong(LAST_CLICK_POS_KEY, pos.asLong());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    public static int getRotation(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.getInt(ROTATION_KEY);
    }
    
    public static void setRotation(ItemStack stack, int rotation) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt(ROTATION_KEY, rotation);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String structureId = getSelectedStructure(stack);
        if (structureId != null && !structureId.isEmpty()) {
            StructureDefinition structure = StructureLoader.getStructure(structureId);
            if (structure != null) {
                tooltip.add(Component.literal("§bSelected: §f" + structure.getName()));
                
                // Show current rotation
                int rotation = getRotation(stack);
                String rotationText = switch (rotation) {
                    case 0 -> Component.translatable("direction.iska_utils.north").getString();
                    case 90 -> Component.translatable("direction.iska_utils.east").getString(); 
                    case 180 -> Component.translatable("direction.iska_utils.south").getString();
                    case 270 -> Component.translatable("direction.iska_utils.west").getString();
                    default -> String.valueOf(rotation) + "°";
                };
                tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.rotation", rotationText));
                
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.right_click_block"));
                tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.right_click_air"));
                tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.left_click_rotate"));
                
                if (structure.isCanForce()) {
                    tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.shift_force"));
                }
            } else {
                tooltip.add(Component.literal("§cInvalid structure: " + structureId));
            }
        } else {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.no_selection"));
        }
        
        if (isPreviewMode(stack)) {
            tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.preview_enabled"));
        }
    }
} 