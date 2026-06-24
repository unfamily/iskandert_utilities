package net.unfamily.iskautils.worldgen.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;
import org.jetbrains.annotations.NotNull;

public final class SacredRubberTreeGrower {
  public static final int LEAVES_OFFSET = 0;

  // Backward-compatible constants (normal scale)
  public static final int TRUNK_RADIUS = SacredRubberTreeScale.NORMAL.trunkRadius;
  public static final int TRUNK_HEIGHT = SacredRubberTreeScale.NORMAL.trunkHeight;
  public static final int LEAVES_RADIUS = SacredRubberTreeScale.NORMAL.leavesRadius;
  public static final int LEAVES_HEIGHT = SacredRubberTreeScale.NORMAL.leavesHeight;

  private SacredRubberTreeGrower() {}

  public static void startGrowth(
      @NotNull ServerLevel level,
      @NotNull BlockPos basePos,
      @NotNull RandomSource random,
      SacredRubberTreeScale scale) {
    level.setBlock(basePos, ModBlocks.SACRED_RUBBER_ROOT.get().defaultBlockState(), 3);
    generateTrunk(level, basePos, random, scale);
    generateLeaves(level, basePos, random, scale);
  }

  public static void startGrowth(
      @NotNull ServerLevel level, @NotNull BlockPos basePos, @NotNull RandomSource random) {
    startGrowth(level, basePos, random, SacredRubberTreeScale.NORMAL);
  }

  private static void generateTrunk(
      @NotNull ServerLevel level,
      @NotNull BlockPos basePos,
      @NotNull RandomSource random,
      SacredRubberTreeScale scale) {
    BlockState strippedLogState =
        ModBlocks.STRIPPED_RUBBER_LOG
            .get()
            .defaultBlockState()
            .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Y);
    BlockState logState =
        ModBlocks.RUBBER_LOG
            .get()
            .defaultBlockState()
            .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Y);
    BlockState sapBlockState = ModBlocks.SAP_BLOCK.get().defaultBlockState();

    for (int y = 0; y < scale.trunkHeight; y++) {
      BlockPos levelPos = basePos.above(y);
      boolean isFirstLayer = (y == 0);
      boolean isLastLayer = (y == scale.trunkHeight - 1);

      for (int x = -scale.trunkRadius; x <= scale.trunkRadius; x++) {
        for (int z = -scale.trunkRadius; z <= scale.trunkRadius; z++) {
          double distance = Math.sqrt(x * x + z * z);
          if (distance <= scale.trunkRadius + 0.5) {
            BlockPos pos = levelPos.offset(x, 0, z);
            BlockState existingState = level.getBlockState(pos);
            if (existingState.isAir() || existingState.canBeReplaced()) {
              BlockState blockToPlace;
              if (distance <= 0.5) {
                blockToPlace = strippedLogState;
              } else if (distance >= scale.trunkRadius - 0.5) {
                if (random.nextFloat() < 0.1f) {
                  Direction facing;
                  if (Math.abs(x) > Math.abs(z)) {
                    facing = x > 0 ? Direction.EAST : Direction.WEST;
                  } else {
                    facing = z > 0 ? Direction.SOUTH : Direction.NORTH;
                  }
                  blockToPlace =
                      ModBlocks.RUBBER_LOG_FILLED
                          .get()
                          .defaultBlockState()
                          .setValue(
                              net.unfamily.iskautils.block.RubberLogFilledBlock.FACING, facing);
                } else {
                  blockToPlace = logState;
                }
              } else if (isFirstLayer || isLastLayer) {
                blockToPlace = logState;
              } else {
                blockToPlace = sapBlockState;
              }
              level.setBlock(pos, blockToPlace, 3);
            }
          }
        }
      }
    }
  }

  private static void generateLeaves(
      @NotNull ServerLevel level,
      @NotNull BlockPos basePos,
      @NotNull RandomSource random,
      SacredRubberTreeScale scale) {
    BlockState leavesState =
        ModBlocks.RUBBER_LEAVES
            .get()
            .defaultBlockState()
            .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, false);
    BlockState woodState =
        ModBlocks.RUBBER_LOG_SACRED
            .get()
            .defaultBlockState()
            .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Y);

    int centerY = basePos.getY() + scale.trunkHeight + LEAVES_OFFSET;

    for (int yOffset = 0; yOffset < scale.leavesHeight; yOffset++) {
      int currentY = centerY + yOffset;
      int centerOffset = Math.abs(yOffset - scale.leavesHeight / 2);
      int maxRadius = scale.leavesRadius;
      double flatZone = scale.leavesHeight * 0.3;
      if (centerOffset > flatZone) {
        double normalizedOffset = (centerOffset - flatZone) / (scale.leavesHeight / 2.0 - flatZone);
        double factor = 1.0 - (normalizedOffset * normalizedOffset);
        maxRadius = (int) (scale.leavesRadius * factor);
        maxRadius = Math.max(1, maxRadius);
      }

      for (int x = -maxRadius; x <= maxRadius; x++) {
        for (int z = -maxRadius; z <= maxRadius; z++) {
          double distance = Math.sqrt(x * x + z * z);
          if (distance <= maxRadius + 1.0) {
            BlockPos pos = new BlockPos(basePos.getX() + x, currentY, basePos.getZ() + z);
            BlockState existingState = level.getBlockState(pos);
            if (existingState.isAir()
                || (existingState.canBeReplaced()
                    && !existingState.is(ModBlocks.RUBBER_LOG.get())
                    && !existingState.is(ModBlocks.RUBBER_LOG_SACRED.get()))) {
              boolean shouldBeWood = false;
              if (x == 0 && z == 0) {
                shouldBeWood = true;
              } else if (distance > scale.trunkRadius + 0.5) {
                BlockPos abovePos = pos.above();
                BlockPos belowPos = pos.below();
                boolean noWoodAbove =
                    !level.getBlockState(abovePos).is(ModBlocks.RUBBER_LOG_SACRED.get());
                boolean noWoodBelow =
                    !level.getBlockState(belowPos).is(ModBlocks.RUBBER_LOG_SACRED.get());
                if (noWoodAbove && noWoodBelow) {
                  shouldBeWood = Math.abs(x) % 4 == 0 && Math.abs(z) % 4 == 0;
                }
              }
              if (shouldBeWood) {
                level.setBlock(pos, woodState, 3);
                var be = level.getBlockEntity(pos);
                if (be == null) {
                  be = ModBlocks.RUBBER_LOG_SACRED.get().newBlockEntity(pos, woodState);
                  if (be != null) {
                    level.setBlockEntity(be);
                  }
                }
                if (be
                    instanceof net.unfamily.iskautils.block.entity.RubberLogSacredBlockEntity
                        sacredEntity) {
                  sacredEntity.setRootPos(basePos);
                }
              } else if (random.nextFloat() < 0.85f) {
                level.setBlock(pos, leavesState, 3);
              }
            }
          }
        }
      }
    }
  }

  public static boolean canGrowAt(
      @NotNull ServerLevel level, @NotNull BlockPos basePos, SacredRubberTreeScale scale) {
    for (int y = 1; y < scale.trunkHeight; y++) {
      for (int x = -scale.trunkRadius; x <= scale.trunkRadius; x++) {
        for (int z = -scale.trunkRadius; z <= scale.trunkRadius; z++) {
          double distance = Math.sqrt(x * x + z * z);
          if (distance <= scale.trunkRadius + 0.5) {
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

  public static boolean canGrowAt(@NotNull ServerLevel level, @NotNull BlockPos basePos) {
    return canGrowAt(level, basePos, SacredRubberTreeScale.NORMAL);
  }
}
