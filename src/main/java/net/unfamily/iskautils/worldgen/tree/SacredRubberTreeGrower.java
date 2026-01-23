package net.unfamily.iskautils.worldgen.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;
import org.jetbrains.annotations.NotNull;

public class SacredRubberTreeGrower {
    // Trunk dimensions
    public static final int TRUNK_RADIUS = 3; // Radius 3 = 7x7 (from -3 to +3)
    public static final int TRUNK_HEIGHT = 30;
    
    // Leaves dimensions
    public static final int LEAVES_RADIUS = 45; // Leaves radius (91x91)
    public static final int LEAVES_HEIGHT = 45; // Total leaves height
    public static final int LEAVES_OFFSET = 0; // Vertical offset
    private static final int LEAVES_START_Y = TRUNK_HEIGHT + LEAVES_OFFSET; // Leaves start above trunk

    /**
     * Starts the growth of the sacred rubber tree
     */
    public static void startGrowth(@NotNull ServerLevel level, @NotNull BlockPos basePos, @NotNull RandomSource random) {
        // Replace the sapling with sacred rubber root
        level.setBlock(basePos, ModBlocks.SACRED_RUBBER_ROOT.get().defaultBlockState(), 3);
        
        // Generate trunk and leaves
        generateTrunk(level, basePos, random);
        generateLeaves(level, basePos, random);
    }
    
    /**
     * Generates a 7x7 circular trunk (radius 3) for TRUNK_HEIGHT levels
     * Center: stripped_rubber_log
     * Edge: rubber_log (or randomly rubber_log_filled facing outward)
     * Rest: sap_block
     */
    private static void generateTrunk(@NotNull ServerLevel level, @NotNull BlockPos basePos, @NotNull RandomSource random) {
        // States for different trunk parts
        BlockState strippedLogState = ModBlocks.STRIPPED_RUBBER_LOG.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState logState = ModBlocks.RUBBER_LOG.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState sapBlockState = ModBlocks.SAP_BLOCK.get().defaultBlockState();
        
        for (int y = 0; y < TRUNK_HEIGHT; y++) {
            BlockPos levelPos = basePos.above(y);
            
            // Check if this is the first or last layer
            boolean isFirstLayer = (y == 0);
            boolean isLastLayer = (y == TRUNK_HEIGHT - 1);
            
            // Generate circular trunk layer
            for (int x = -TRUNK_RADIUS; x <= TRUNK_RADIUS; x++) {
                for (int z = -TRUNK_RADIUS; z <= TRUNK_RADIUS; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    
                    // If inside the circle
                    if (distance <= TRUNK_RADIUS + 0.5) {
                        BlockPos pos = levelPos.offset(x, 0, z);
                        BlockState existingState = level.getBlockState(pos);
                        
                        if (existingState.isAir() || existingState.canBeReplaced()) {
                            BlockState blockToPlace;
                            
                            // Center: stripped rubber log
                            if (distance <= 0.5) {
                                blockToPlace = strippedLogState;
                            }
                            // Edge: rubber log (or randomly rubber_log_filled facing outward)
                            else if (distance >= TRUNK_RADIUS - 0.5) {
                                // Random chance for filled log on edge (e.g., 10% chance)
                                if (random.nextFloat() < 0.1f) {
                                    // Determine direction towards outside
                                    Direction facing;
                                    if (Math.abs(x) > Math.abs(z)) {
                                        facing = x > 0 ? Direction.EAST : Direction.WEST;
                                    } else {
                                        facing = z > 0 ? Direction.SOUTH : Direction.NORTH;
                                    }
                                    blockToPlace = ModBlocks.RUBBER_LOG_FILLED.get().defaultBlockState()
                                            .setValue(net.unfamily.iskautils.block.RubberLogFilledBlock.FACING, facing);
                                } else {
                                    blockToPlace = logState;
                                }
                            }
                            // Rest: sap block (or rubber_log for first/last layer)
                            else {
                                // First and last layer use rubber_log instead of sap_block
                                if (isFirstLayer || isLastLayer) {
                                    blockToPlace = logState;
                                } else {
                                    blockToPlace = sapBlockState;
                                }
                            }
                            
                            level.setBlock(pos, blockToPlace, 3);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Generates circular leaves above the trunk (scaled BlobFoliagePlacer shape)
     * Replicates the vanilla BlobFoliagePlacer shape but scaled 6x
     */
    private static void generateLeaves(@NotNull ServerLevel level, @NotNull BlockPos basePos, @NotNull RandomSource random) {
        // PERSISTENT = false (default) so leaves automatically decay if they no longer have the trunk
        BlockState leavesState = ModBlocks.RUBBER_LEAVES.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, false);
        
        // Sacred rubber log state for internal support branches (to keep leaves within 5 blocks of wood)
        BlockState woodState = ModBlocks.RUBBER_LOG_SACRED.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Y);
        
        int centerY = basePos.getY() + TRUNK_HEIGHT + LEAVES_OFFSET;
        
        // Generate leaves for each Y level
        for (int yOffset = 0; yOffset < LEAVES_HEIGHT; yOffset++) {
            int currentY = centerY + yOffset;
            
            // Calculate radius for this level (flatter shape, less pointed)
            // Maintains maximum radius for a larger central portion
            int centerOffset = Math.abs(yOffset - LEAVES_HEIGHT / 2);
            int maxRadius = LEAVES_RADIUS;
            
            // Use a gentler quadratic formula to make the canopy flatter
            // Maintains maximum radius for ~60% of central height
            double flatZone = LEAVES_HEIGHT * 0.3; // Flat zone at center (30% of height)
            if (centerOffset > flatZone) {
                // Reduce radius more gradually using a quadratic curve
                double normalizedOffset = (centerOffset - flatZone) / (LEAVES_HEIGHT / 2.0 - flatZone);
                double factor = 1.0 - (normalizedOffset * normalizedOffset); // Gentler quadratic curve
                maxRadius = (int)(LEAVES_RADIUS * factor);
                maxRadius = Math.max(1, maxRadius); // Minimum radius 1
            }
            
            // Generate leaves in a circle
            for (int x = -maxRadius; x <= maxRadius; x++) {
                for (int z = -maxRadius; z <= maxRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    
                    // If inside the circle (increased tolerance to include edge blocks)
                    if (distance <= maxRadius + 1.0) {
                        BlockPos pos = new BlockPos(basePos.getX() + x, currentY, basePos.getZ() + z);
                        BlockState existingState = level.getBlockState(pos);
                        
                        // Replace only air or non-solid blocks (don't replace trunk or existing sacred log)
                        if (existingState.isAir() || (existingState.canBeReplaced() && !existingState.is(ModBlocks.RUBBER_LOG.get()) && !existingState.is(ModBlocks.RUBBER_LOG_SACRED.get()))) {
                            // Place rubber_wood as sparse support branches
                            // Center column (x=0, z=0): continuous vertical grid
                            // Other positions: sparse pattern every 6 blocks, isolated blocks (no vertical continuity), can protrude horizontally
                            boolean shouldBeWood = false;
                            boolean isCenter = (x == 0 && z == 0);
                            
                            if (isCenter) {
                                // Center column: continuous vertical grid
                                shouldBeWood = true;
                            } else {
                                // Denser grid pattern: every 4 blocks, no horizontal distance limit (can protrude)
                                if (distance > TRUNK_RADIUS + 0.5) {
                                    // Check that there's no wood block directly above or below (prevent vertical continuity)
                                    BlockPos abovePos = pos.above();
                                    BlockPos belowPos = pos.below();
                                    boolean noWoodAbove = !level.getBlockState(abovePos).is(ModBlocks.RUBBER_LOG_SACRED.get());
                                    boolean noWoodBelow = !level.getBlockState(belowPos).is(ModBlocks.RUBBER_LOG_SACRED.get());
                                    
                                    if (noWoodAbove && noWoodBelow) {
                                        int gridX = Math.abs(x) % 4;
                                        int gridZ = Math.abs(z) % 4;
                                        shouldBeWood = (gridX == 0 && gridZ == 0); // On denser grid
                                    }
                                }
                            }
                            
                            if (shouldBeWood) {
                                // Place single isolated wood block to support leaves
                                level.setBlock(pos, woodState, 3);
                                // Set root position in BlockEntity (ensure it's created)
                                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                                if (be == null) {
                                    // Force creation of BlockEntity if it doesn't exist
                                    be = ModBlocks.RUBBER_LOG_SACRED.get().newBlockEntity(pos, woodState);
                                    if (be != null) {
                                        level.setBlockEntity(be);
                                    }
                                }
                                if (be instanceof net.unfamily.iskautils.block.entity.RubberLogSacredBlockEntity sacredEntity) {
                                    sacredEntity.setRootPos(basePos);
                                }
                            } else {
                                // Place leaves with 85% probability (15% chance to leave empty for natural gaps)
                                if (random.nextFloat() < 0.85f) {
                                    level.setBlock(pos, leavesState, 3);
                                }
                                // Otherwise leave as air (creates natural gaps in the canopy)
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Checks if a position is valid for tree growth
     */
    public static boolean canGrowAt(@NotNull ServerLevel level, @NotNull BlockPos basePos) {
        // Verify there is space for the 7x7 trunk
        // Skip y=0 (the sapling position itself)
        for (int y = 1; y < TRUNK_HEIGHT; y++) {
            for (int x = -TRUNK_RADIUS; x <= TRUNK_RADIUS; x++) {
                for (int z = -TRUNK_RADIUS; z <= TRUNK_RADIUS; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= TRUNK_RADIUS + 0.5) {
                        BlockPos pos = basePos.above(y).offset(x, 0, z);
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && !state.canBeReplaced()) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
