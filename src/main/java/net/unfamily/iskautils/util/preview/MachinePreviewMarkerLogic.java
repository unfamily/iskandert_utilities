package net.unfamily.iskautils.util.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.unfamily.iskautils.block.FanBlock;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.unfamily.iskalib.structure.StructureDefinition;

import java.util.List;
import java.util.Map;

/**
 * Shared footprint marker rules for server preview packets and client reconcile.
 * Always emits baseline colors together with occupied when both apply.
 */
public final class MachinePreviewMarkerLogic {

    public static final int COLOR_FRAME_EDGE = 0x80FF00FF;
    public static final int COLOR_OCCUPIED_FAN = 0x80FF0000;
    public static final int COLOR_OCCUPIED_STRUCTURE = 0x80FF4444;
    public static final int COLOR_VALID = 0x804444FF;
    /** Linked / highlight markers: air cells are red, present blocks are green. */
    public static final int COLOR_MARKED_AIR = 0x80FF0000;
    public static final int COLOR_MARKED_SOLID = 0x8000FF00;

    @FunctionalInterface
    public interface MarkerSink {
        void accept(BlockPos worldPos, int color);
    }

    private MachinePreviewMarkerLogic() {}

    public static int colorForMarkedPresence(BlockState state) {
        return state.isAir() ? COLOR_MARKED_AIR : COLOR_MARKED_SOLID;
    }

    public static void forEachFanMarker(Level level, FanBlockEntity fan, BlockPos fanPos, MarkerSink sink) {
        BlockState state = level.getBlockState(fanPos);
        if (!(state.getBlock() instanceof FanBlock)) {
            return;
        }
        var facing = state.getValue(FanBlock.FACING);
        boolean hasGhostModule = fan.hasGhostModule();
        AABB aabb = FanBlockEntity.calculatePushArea(fanPos, facing, fan);

        int minX = (int) Math.floor(aabb.minX);
        int minY = (int) Math.floor(aabb.minY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxX = (int) Math.floor(aabb.maxX);
        int maxY = (int) Math.floor(aabb.maxY);
        int maxZ = (int) Math.floor(aabb.maxZ);

        // Outer shell is drawn via AreaBorderRenderer on the client; only occupied cells here.
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    if (FanBlockEntity.isBlockObstacle(level, blockPos, hasGhostModule)) {
                        sink.accept(blockPos, COLOR_OCCUPIED_FAN);
                    }
                }
            }
        }
    }

    private static void emitFanEdgeCell(Level level, MarkerSink sink, BlockPos pos, boolean hasGhostModule) {
        boolean obstacle = FanBlockEntity.isBlockObstacle(level, pos, hasGhostModule);
        if (obstacle) {
            sink.accept(pos, COLOR_OCCUPIED_FAN);
        }
        sink.accept(pos, COLOR_FRAME_EDGE);
    }

    public static void forEachStructurePlacerMarker(
            Level level,
            BlockPos machinePos,
            StructureDefinition structure,
            int rotation,
            MarkerSink sink) {
        if (structure == null) {
            return;
        }
        String[][][][] pattern = structure.getPattern();
        if (pattern == null || pattern.length == 0) {
            return;
        }

        BlockPos center = structure.findCenter();
        if (center == null) {
            center = BlockPos.ZERO;
        }

        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    if (cellChars == null) {
                        continue;
                    }
                    for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                        String patternChar = cellChars[charIndex];
                        if (patternChar == null || patternChar.equals(" ")) {
                            continue;
                        }
                        if (patternChar.equals("@")) {
                            Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
                            if (key == null || !key.containsKey("@")) {
                                continue;
                            }
                        }

                        int effectiveZ = z * cellChars.length + charIndex;
                        int offsetX = x - center.getX();
                        int offsetY = y - center.getY();
                        int offsetZ = effectiveZ - center.getZ();
                        BlockPos rotatedOffset = applyRotation(offsetX, offsetY, offsetZ, rotation);
                        BlockPos finalPos = machinePos.offset(
                                rotatedOffset.getX(), rotatedOffset.getY() + 1, rotatedOffset.getZ());

                        emitStructureCellMarker(level, finalPos, structure, sink);
                    }
                }
            }
        }
    }

    public static void emitStructureCellMarker(
            Level level, BlockPos worldPos, StructureDefinition structure, MarkerSink sink) {
        sink.accept(worldPos, COLOR_VALID);
        if (!canReplaceBlock(level.getBlockState(worldPos), structure)) {
            sink.accept(worldPos, COLOR_OCCUPIED_STRUCTURE);
        }
    }

    public static int resolveStructureCellColor(Level level, BlockPos worldPos, StructureDefinition structure) {
        if (!canReplaceBlock(level.getBlockState(worldPos), structure)) {
            return COLOR_OCCUPIED_STRUCTURE;
        }
        return COLOR_VALID;
    }

    public static AABB getStructurePlacerFootprintAABB(
            BlockPos machinePos, StructurePlacerMachineBlockEntity machine) {
        String selectedStructure = machine.getSelectedStructure();
        if (selectedStructure == null || selectedStructure.isEmpty()) {
            return null;
        }
        StructureDefinition structure = net.unfamily.iskalib.structure.StructureLoader.getStructure(selectedStructure);
        if (structure == null) {
            return null;
        }
        return getStructureFootprintAABB(machinePos, structure, machine.getRotation());
    }

    public static AABB getStructureFootprintAABB(BlockPos machinePos, StructureDefinition structure, int rotation) {
        String[][][][] pattern = structure.getPattern();
        if (pattern == null || pattern.length == 0) {
            return null;
        }
        BlockPos center = structure.findCenter();
        if (center == null) {
            center = BlockPos.ZERO;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean any = false;

        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    if (cellChars == null) {
                        continue;
                    }
                    for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                        String patternChar = cellChars[charIndex];
                        if (patternChar == null || patternChar.equals(" ")) {
                            continue;
                        }
                        if (patternChar.equals("@")) {
                            Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
                            if (key == null || !key.containsKey("@")) {
                                continue;
                            }
                        }
                        int effectiveZ = z * cellChars.length + charIndex;
                        int offsetX = x - center.getX();
                        int offsetY = y - center.getY();
                        int offsetZ = effectiveZ - center.getZ();
                        BlockPos rotatedOffset = applyRotation(offsetX, offsetY, offsetZ, rotation);
                        BlockPos finalPos = machinePos.offset(
                                rotatedOffset.getX(), rotatedOffset.getY() + 1, rotatedOffset.getZ());
                        minX = Math.min(minX, finalPos.getX());
                        minY = Math.min(minY, finalPos.getY());
                        minZ = Math.min(minZ, finalPos.getZ());
                        maxX = Math.max(maxX, finalPos.getX());
                        maxY = Math.max(maxY, finalPos.getY());
                        maxZ = Math.max(maxZ, finalPos.getZ());
                        any = true;
                    }
                }
            }
        }
        if (!any) {
            return null;
        }
        return new AABB(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
    }

    public static BlockPos applyRotation(int x, int y, int z, int rotation) {
        return switch (rotation) {
            case 90 -> new BlockPos(-z, y, x);
            case 180 -> new BlockPos(-x, y, -z);
            case 270 -> new BlockPos(z, y, -x);
            default -> new BlockPos(x, y, z);
        };
    }

    public static boolean canReplaceBlock(BlockState state, StructureDefinition structure) {
        Block block = state.getBlock();
        if (state.isAir() || state.canBeReplaced()) {
            return true;
        }
        if (structure.getCanReplace() != null) {
            for (String replaceableBlock : structure.getCanReplace()) {
                try {
                    Identifier blockLocation = Identifier.parse(replaceableBlock);
                    Block allowedBlock = BuiltInRegistries.BLOCK.getOptional(blockLocation).orElse(null);
                    if (allowedBlock != null && block == allowedBlock) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }
}
