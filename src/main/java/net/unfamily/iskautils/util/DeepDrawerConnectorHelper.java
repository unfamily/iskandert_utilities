package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.block.entity.DeepDrawerExtenderBlockEntity;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;
import net.unfamily.iskautils.block.entity.DeepDrawerInterfaceBlockEntity;
import net.unfamily.iskautils.block.entity.DeepDrawersBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Helper class for finding Deep Drawer through connector network
 * All connectors (extender, interface, extractor) can connect to each other to find the drawer
 */
public class DeepDrawerConnectorHelper {
    
    private static final int MAX_SEARCH_DISTANCE = 16; // Maximum blocks to search through
    
    /**
     * Finds a Deep Drawer by searching through connector network
     * Searches directly adjacent blocks and through connectors (extender, interface, extractor)
     * 
     * @param level The world level
     * @param startPos Starting position (usually the connector's position)
     * @return The found Deep Drawer BlockEntity, or null if not found
     */
    @Nullable
    public static DeepDrawersBlockEntity findConnectedDrawer(@NotNull Level level, @NotNull BlockPos startPos) {
        if (level == null || startPos == null) {
            return null;
        }
        
        // Use BFS to search through connector network
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        
        queue.offer(startPos);
        visited.add(startPos);
        
        int distance = 0;
        
        while (!queue.isEmpty() && distance < MAX_SEARCH_DISTANCE) {
            int levelSize = queue.size();
            
            for (int i = 0; i < levelSize; i++) {
                BlockPos currentPos = queue.poll();
                if (currentPos == null) {
                    continue;
                }
                
                // Check all 6 directions from current position
                for (@NotNull Direction direction : Direction.values()) {
                    @NotNull BlockPos checkPos = currentPos.relative(direction);
                    
                    // Skip if already visited
                    if (visited.contains(checkPos)) {
                        continue;
                    }
                    visited.add(checkPos);
                    
                    BlockEntity be = level.getBlockEntity(checkPos);
                    if (be == null) {
                        continue;
                    }
                    
                    // Found the drawer directly!
                    if (be instanceof DeepDrawersBlockEntity drawer) {
                        return drawer;
                    }
                    
                    // Found a connector - add to queue to search through it
                    // (starting position is already visited, so it won't be added again)
                    if (be instanceof DeepDrawerExtenderBlockEntity ||
                        be instanceof DeepDrawerInterfaceBlockEntity ||
                        be instanceof DeepDrawerExtractorBlockEntity) {
                        queue.offer(checkPos);
                    }
                }
            }
            
            distance++;
        }
        
        return null;
    }
}
