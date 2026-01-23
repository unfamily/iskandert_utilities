package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.SacredRubberSaplingBlock;
import net.unfamily.iskautils.worldgen.tree.SacredRubberTreeGrower;

public class SacredRubberSaplingBlockEntity extends BlockEntity {
    private static final int GROWTH_INTERVAL = 2; // Add blocks every 2 ticks (faster)

    private boolean isGrowing = false;
    private int growthTick = 0;
    private int currentTrunkY = 0;
    private int currentLeavesY = 0;
    private BlockPos basePos;
    
    public SacredRubberSaplingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SACRED_RUBBER_SAPLING_BE.get(), pos, state);
        this.basePos = pos;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SacredRubberSaplingBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        
        // Ensure basePos is initialized
        if (blockEntity.basePos == null) {
            blockEntity.basePos = pos;
            blockEntity.setChanged();
        }
        
        if (!blockEntity.isGrowing) {
            int bonemealCount = state.getValue(SacredRubberSaplingBlock.BONEMEAL_COUNT);
            if (bonemealCount >= 15) {
                if (SacredRubberTreeGrower.canGrowAt((ServerLevel) level, pos)) {
                    blockEntity.isGrowing = true;
                    blockEntity.basePos = pos;
                    blockEntity.currentTrunkY = 0;
                    blockEntity.currentLeavesY = 0;
                    blockEntity.growthTick = 0;
                    blockEntity.setChanged();
                }
            }
            return;
        }
        blockEntity.growthTick++;
        if (blockEntity.growthTick >= GROWTH_INTERVAL) {
            blockEntity.growthTick = 0;
            blockEntity.growStep((ServerLevel) level, state);
        }
    }

    private void growStep(ServerLevel level, BlockState state) {
        RandomSource random = level.getRandom();
        if (currentTrunkY < SacredRubberTreeGrower.TRUNK_HEIGHT) {
            growTrunkLayer(level, random);
            currentTrunkY++;
        } else {
            growLeavesLayer(level, random);
            currentLeavesY++;
            if (currentLeavesY >= SacredRubberTreeGrower.LEAVES_HEIGHT) {
                // Replace sapling with sacred rubber root when growth completes
                level.setBlock(basePos, ModBlocks.SACRED_RUBBER_ROOT.get().defaultBlockState(), 3);
                isGrowing = false;
            }
        }
        setChanged();
    }

    private void growTrunkLayer(ServerLevel level, RandomSource random) {
        int y = currentTrunkY;
        BlockPos levelPos = basePos.above(y);
        
        // Check if this is the first or last layer
        boolean isFirstLayer = (y == 0);
        boolean isLastLayer = (y == SacredRubberTreeGrower.TRUNK_HEIGHT - 1);
        
        // States for different trunk parts
        net.minecraft.world.level.block.state.BlockState strippedLogState = ModBlocks.STRIPPED_RUBBER_LOG.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
        net.minecraft.world.level.block.state.BlockState logState = ModBlocks.RUBBER_LOG.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
        net.minecraft.world.level.block.state.BlockState sapBlockState = ModBlocks.SAP_BLOCK.get().defaultBlockState();
        
        for (int x = -SacredRubberTreeGrower.TRUNK_RADIUS; x <= SacredRubberTreeGrower.TRUNK_RADIUS; x++) {
            for (int z = -SacredRubberTreeGrower.TRUNK_RADIUS; z <= SacredRubberTreeGrower.TRUNK_RADIUS; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance <= SacredRubberTreeGrower.TRUNK_RADIUS + 0.5) {
                    BlockPos pos = levelPos.offset(x, 0, z);
                    net.minecraft.world.level.block.state.BlockState existingState = level.getBlockState(pos);
                    if (existingState.isAir() || existingState.canBeReplaced()) {
                        net.minecraft.world.level.block.state.BlockState blockToPlace;
                        
                        // Center: stripped rubber log
                        if (distance <= 0.5) {
                            blockToPlace = strippedLogState;
                        }
                        // Edge: rubber log (or randomly rubber_log_filled facing outward)
                        else if (distance >= SacredRubberTreeGrower.TRUNK_RADIUS - 0.5) {
                            // Random chance for filled log on edge (e.g., 10% chance)
                            if (random.nextFloat() < 0.1f) {
                                // Determine direction towards outside
                                net.minecraft.core.Direction facing;
                                if (Math.abs(x) > Math.abs(z)) {
                                    facing = x > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
                                } else {
                                    facing = z > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
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
    
    private void growLeavesLayer(ServerLevel level, RandomSource random) {
        int yOffset = currentLeavesY;
        int centerY = basePos.getY() + SacredRubberTreeGrower.TRUNK_HEIGHT + SacredRubberTreeGrower.LEAVES_OFFSET;
        int currentY = centerY + yOffset;
        
        // Calculate radius for this level (flatter shape, less pointed)
        // Maintains maximum radius for a larger central portion
        int centerOffset = Math.abs(yOffset - SacredRubberTreeGrower.LEAVES_HEIGHT / 2);
        int maxRadius = SacredRubberTreeGrower.LEAVES_RADIUS;
        
        // Use a gentler quadratic formula to make the canopy flatter
        // Maintains maximum radius for ~60% of central height
        double flatZone = SacredRubberTreeGrower.LEAVES_HEIGHT * 0.3; // Flat zone at center (30% of height)
        if (centerOffset > flatZone) {
            // Reduce radius more gradually using a quadratic curve
            double normalizedOffset = (centerOffset - flatZone) / (SacredRubberTreeGrower.LEAVES_HEIGHT / 2.0 - flatZone);
            double factor = 1.0 - (normalizedOffset * normalizedOffset); // Gentler quadratic curve
            maxRadius = (int)(SacredRubberTreeGrower.LEAVES_RADIUS * factor);
            maxRadius = Math.max(1, maxRadius); // Minimum radius 1
        }
        
        // PERSISTENT = false (default) so leaves automatically decay if they no longer have the trunk
        net.minecraft.world.level.block.state.BlockState leavesState = ModBlocks.RUBBER_LEAVES.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, false);
        
        // Sacred rubber log state for internal support branches (to keep leaves within 5 blocks of wood)
        net.minecraft.world.level.block.state.BlockState woodState = ModBlocks.RUBBER_LOG_SACRED.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
        
        // Generate all leaves for this level
        for (int x = -maxRadius; x <= maxRadius; x++) {
            for (int z = -maxRadius; z <= maxRadius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                
                // If inside the circle (increased tolerance to include edge blocks)
                if (distance <= maxRadius + 1.0) {
                    BlockPos pos = new BlockPos(basePos.getX() + x, currentY, basePos.getZ() + z);
                    net.minecraft.world.level.block.state.BlockState existingState = level.getBlockState(pos);
                    
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
                            if (distance > SacredRubberTreeGrower.TRUNK_RADIUS + 0.5) {
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
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isGrowing", isGrowing);
        tag.putInt("growthTick", growthTick);
        tag.putInt("currentTrunkY", currentTrunkY);
        tag.putInt("currentLeavesY", currentLeavesY);
        tag.putLong("basePos", basePos.asLong());
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isGrowing = tag.getBoolean("isGrowing");
        growthTick = tag.getInt("growthTick");
        currentTrunkY = tag.getInt("currentTrunkY");
        currentLeavesY = tag.getInt("currentLeavesY");
        if (tag.contains("basePos")) {
            basePos = BlockPos.of(tag.getLong("basePos"));
        } else {
            basePos = worldPosition; // Fallback to current position if not saved
        }
    }
}
