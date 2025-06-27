package net.unfamily.iskautils.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.*;

/**
 * Manages the history of structure placements for undo functionality
 */
public class StructurePlacementHistory {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, LinkedList<StructurePlacementEntry>> PLAYER_HISTORY = new HashMap<>();
    private static final int MAX_HISTORY_SIZE = 3;

    /**
     * Represents a single structure placement entry
     */
    public static class StructurePlacementEntry {
        private final BlockPos centerPos;
        private final String structureId;
        private final int rotation;
        private final long timestamp;
        private final UUID playerId;

        public StructurePlacementEntry(BlockPos centerPos, String structureId, int rotation, UUID playerId) {
            this.centerPos = centerPos;
            this.structureId = structureId;
            this.rotation = rotation;
            this.timestamp = System.currentTimeMillis();
            this.playerId = playerId;
        }

        public BlockPos getCenterPos() { return centerPos; }
        public String getStructureId() { return structureId; }
        public int getRotation() { return rotation; }
        public long getTimestamp() { return timestamp; }
        public UUID getPlayerId() { return playerId; }
    }

    /**
     * Adds a new structure placement to the player's history
     */
    public static void addPlacement(ServerPlayer player, BlockPos centerPos, String structureId, int rotation) {
        LinkedList<StructurePlacementEntry> history = PLAYER_HISTORY.computeIfAbsent(player.getUUID(), k -> new LinkedList<>());
        
        // Add new entry at the beginning (most recent first)
        history.addFirst(new StructurePlacementEntry(centerPos, structureId, rotation, player.getUUID()));
        
        // Keep only the last MAX_HISTORY_SIZE entries
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeLast();
        }
        
        LOGGER.debug("Added structure placement to history for player {}: {} at {}", 
                    player.getName().getString(), structureId, centerPos);
    }

    /**
     * Performs undo of the last structure placement
     */
    public static boolean undoLastPlacement(ServerPlayer player) {
        LinkedList<StructurePlacementEntry> history = PLAYER_HISTORY.get(player.getUUID());
        if (history == null || history.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.iska_utils.structure_undo.no_history"), true);
            return false;
        }

        StructurePlacementEntry entry = history.removeFirst();
        return performUndo(player, entry);
    }

    /**
     * Gets the number of structures in history for a player
     */
    public static int getHistorySize(ServerPlayer player) {
        LinkedList<StructurePlacementEntry> history = PLAYER_HISTORY.get(player.getUUID());
        return history != null ? history.size() : 0;
    }

    /**
     * Clears all history for a player (when they disconnect)
     */
    public static void clearPlayerHistory(UUID playerId) {
        PLAYER_HISTORY.remove(playerId);
    }

    /**
     * Performs the actual undo operation using reverse engineering
     */
    private static boolean performUndo(ServerPlayer player, StructurePlacementEntry entry) {
        // Get the structure definition
        StructureDefinition structure = StructureLoader.getStructure(entry.getStructureId());
        if (structure == null) {
            player.displayClientMessage(Component.translatable("message.iska_utils.structure_undo.structure_not_found", entry.getStructureId()), true);
            return false;
        }

        // Recalculate the positions using the same logic as placement
        Map<BlockPos, String> expectedPositions = calculateStructurePositions(
            entry.getCenterPos(), structure, entry.getRotation()
        );

        if (expectedPositions.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.iska_utils.structure_undo.no_positions"), true);
            return false;
        }

        Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
        Map<String, Integer> recoveredMaterials = new HashMap<>();
        int removedBlocks = 0;
        int skippedBlocks = 0;

        // For each expected position, check if the block matches and remove it
        for (Map.Entry<BlockPos, String> posEntry : expectedPositions.entrySet()) {
            BlockPos pos = posEntry.getKey();
            String character = posEntry.getValue();

            List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
            if (blockDefs == null || blockDefs.isEmpty()) continue;

            BlockState currentState = player.level().getBlockState(pos);

            // Check if current block matches any of the expected block definitions
            boolean matches = false;
            StructureDefinition.BlockDefinition matchedDef = null;

            for (StructureDefinition.BlockDefinition blockDef : blockDefs) {
                if (blockMatchesDefinition(currentState, blockDef)) {
                    matches = true;
                    matchedDef = blockDef;
                    break;
                }
            }

            if (matches && matchedDef != null) {
                // Remove the block and count recovered materials
                ((ServerLevel)player.level()).setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

                String materialKey = matchedDef.getDisplay() != null ? 
                    matchedDef.getDisplay() : matchedDef.getBlock();
                recoveredMaterials.merge(materialKey, 1, Integer::sum);
                removedBlocks++;
            } else {
                skippedBlocks++;
            }
        }

        // Give materials back to player (only in survival mode)
        if (!player.isCreative()) {
            giveMaterialsToPlayer(player, recoveredMaterials, structure);
        }

        // Feedback to player
        String structureName = structure.getName() != null ? structure.getName() : structure.getId();
        if (removedBlocks > 0) {
            player.displayClientMessage(Component.translatable("message.iska_utils.structure_undo.success", 
                structureName, removedBlocks, skippedBlocks), true);
            return true;
        } else {
            player.displayClientMessage(Component.translatable("message.iska_utils.structure_undo.no_blocks", 
                structureName), true);
            return false;
        }
    }

    /**
     * Calculates structure positions (reuses logic from StructurePlacerItem)
     */
    private static Map<BlockPos, String> calculateStructurePositions(BlockPos centerPos, StructureDefinition structure, int rotation) {
        Map<BlockPos, String> positions = new HashMap<>();
        
        String[][][][] pattern = structure.getPattern();
        if (pattern == null) return positions;
        
        // Find structure center (symbol @)
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
                                    continue;
                                }
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
     * Apply rotation to relative coordinates (reuses logic from StructurePlacerItem)
     */
    private static BlockPos applyRotation(int x, int y, int z, int rotation) {
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
     * Checks if a block state matches a block definition
     * For undo purposes, we only check the block type and ignore all properties
     */
    private static boolean blockMatchesDefinition(BlockState currentState, StructureDefinition.BlockDefinition blockDef) {
        try {
            // Get expected block
            ResourceLocation blockLocation = ResourceLocation.parse(blockDef.getBlock());
            Block expectedBlock = BuiltInRegistries.BLOCK.get(blockLocation);
            
            // Only check block type, ignore all properties for undo
            return currentState.getBlock() == expectedBlock;
            
        } catch (Exception e) {
            return false;
        }
    }



    /**
     * Gives recovered materials back to the player
     */
    private static void giveMaterialsToPlayer(ServerPlayer player, Map<String, Integer> materials, StructureDefinition structure) {
        Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
        
        for (Map.Entry<String, Integer> materialEntry : materials.entrySet()) {
            String materialKey = materialEntry.getKey();
            int count = materialEntry.getValue();
            
            // Find block definition for this material
            StructureDefinition.BlockDefinition blockDef = findBlockDefinitionByMaterialKey(key, materialKey);
            if (blockDef != null) {
                try {
                    ResourceLocation blockLocation = ResourceLocation.parse(blockDef.getBlock());
                    Block block = BuiltInRegistries.BLOCK.get(blockLocation);
                    Item item = block.asItem();
                    
                    if (item != Items.AIR) {
                        ItemStack itemStack = new ItemStack(item, count);
                        
                        // Try to add to inventory
                        if (!player.getInventory().add(itemStack)) {
                            // If inventory is full, drop items
                            player.drop(itemStack, false);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to give material {} to player {}: {}", materialKey, player.getName().getString(), e.getMessage());
                }
            }
        }
        
        player.getInventory().setChanged();
    }

    /**
     * Finds a block definition by material key (display name or block name)
     */
    private static StructureDefinition.BlockDefinition findBlockDefinitionByMaterialKey(
            Map<String, List<StructureDefinition.BlockDefinition>> key, String materialKey) {
        for (List<StructureDefinition.BlockDefinition> blockDefs : key.values()) {
            for (StructureDefinition.BlockDefinition blockDef : blockDefs) {
                String defKey = blockDef.getDisplay() != null ? blockDef.getDisplay() : blockDef.getBlock();
                if (materialKey.equals(defKey)) {
                    return blockDef;
                }
            }
        }
        return null;
    }
} 