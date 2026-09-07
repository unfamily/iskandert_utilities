package net.unfamily.iskautils.util;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Soft-dep rotation property registry for Swiss Wrench radial mode.
 * Matches BlockState properties by name (vanilla / Create / AE2) without hard mod deps.
 * Nested flow: face first, then spin / axle / other properties as needed.
 */
public final class SwissWrenchRotationProperties {

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    /** Create uses BooleanProperty.create("axis_along_first"); keep legacy alias too. */
    private static final Set<String> AXLE_PROPERTY_NAMES = Set.of(
            "axis_along_first",
            "axis_along_first_coordinate");

    /**
     * Soft-dep copy of AE2 SpinMapping: for each facing, spin 0..3 → world direction of local up.
     * Used only for UI labels when property name is {@code spin}; values still apply as integers.
     */
    private static final Direction[][] SPIN_UP_DIRECTIONS = {
            // DOWN
            {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST},
            // UP
            {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST},
            // NORTH
            {Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST},
            // SOUTH
            {Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST},
            // WEST
            {Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH},
            // EAST
            {Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH},
    };

    static {
        // Vanilla / shared
        LABELS.put("facing", "Facing");
        LABELS.put("horizontal_facing", "Facing");
        LABELS.put("axis", "Axis");
        // Create
        LABELS.put("axis_along_first", "Axle");
        LABELS.put("axis_along_first_coordinate", "Axle");
        LABELS.put("target", "Target");
        LABELS.put("vertical", "Vertical");
        // AE2
        LABELS.put("spin", "Spin");
        LABELS.put("push_direction", "Push");
    }

    /** One nested radial step: which property to pick, with a UI title. */
    public record RotationStep(String propertyName, Component title) {
    }

    private SwissWrenchRotationProperties() {
    }

    public static boolean isRegistered(String propertyName) {
        return LABELS.containsKey(propertyName);
    }

    public static boolean isAxleProperty(String propertyName) {
        return AXLE_PROPERTY_NAMES.contains(propertyName);
    }

    public static Component labelFor(String propertyName) {
        if (isAxleProperty(propertyName)) {
            return Component.translatable("item.iska_utils.swiss_wrench.radial.step.axle");
        }
        return switch (propertyName) {
            case "facing", "horizontal_facing" ->
                    Component.translatable("item.iska_utils.swiss_wrench.radial.step.facing");
            case "spin" ->
                    Component.translatable("item.iska_utils.swiss_wrench.radial.step.spin");
            case "axis" ->
                    Component.translatable("item.iska_utils.swiss_wrench.radial.step.axis");
            default -> Component.literal(LABELS.getOrDefault(propertyName, propertyName));
        };
    }

    public static List<Property<?>> findRotatableProperties(BlockState state) {
        List<Property<?>> result = new ArrayList<>();
        for (Property<?> property : state.getProperties()) {
            if (LABELS.containsKey(property.getName())) {
                result.add(property);
            }
        }
        return result;
    }

    public static boolean canRotate(BlockState state) {
        return !findRotatableProperties(state).isEmpty();
    }

    /**
     * Ordered nested steps: face first when present, then spin/axle, then other whitelisted props.
     * Axis-only blocks get axis as the first (only) step.
     */
    public static List<RotationStep> buildSteps(BlockState state) {
        List<RotationStep> steps = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();

        Property<?> facing = findProperty(state, "facing");
        Property<?> horizontalFacing = findProperty(state, "horizontal_facing");
        Property<?> spin = findProperty(state, "spin");
        Property<?> axle = findAxleProperty(state);
        Property<?> axis = findProperty(state, "axis");

        if (facing != null) {
            addStep(steps, used, facing.getName());
            if (spin != null) {
                addStep(steps, used, spin.getName());
            }
            if (axle != null) {
                addStep(steps, used, axle.getName());
            }
        } else if (horizontalFacing != null) {
            addStep(steps, used, horizontalFacing.getName());
            if (axle != null) {
                addStep(steps, used, axle.getName());
            }
        } else if (axis != null) {
            addStep(steps, used, axis.getName());
        }

        for (Property<?> property : state.getProperties()) {
            String name = property.getName();
            if (LABELS.containsKey(name) && used.add(name)) {
                steps.add(new RotationStep(name, labelFor(name)));
            }
        }
        return steps;
    }

    private static void addStep(List<RotationStep> steps, Set<String> used, String propertyName) {
        if (used.add(propertyName)) {
            steps.add(new RotationStep(propertyName, labelFor(propertyName)));
        }
    }

    public static Property<?> findProperty(BlockState state, String propertyName) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }
        return null;
    }

    public static Property<?> findAxleProperty(BlockState state) {
        for (String name : AXLE_PROPERTY_NAMES) {
            Property<?> property = findProperty(state, name);
            if (property != null) {
                return property;
            }
        }
        return null;
    }

    public static Property<?> propertyForStep(BlockState state, RotationStep step) {
        return findProperty(state, step.propertyName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<BlockState> cycleValues(BlockState base, Property<?> property) {
        List<BlockState> states = new ArrayList<>();
        for (Comparable value : property.getPossibleValues()) {
            states.add(base.setValue((Property) property, value));
        }
        return states;
    }

    /**
     * Sector label for UI. Axle / axis / spin / facing use friendly direction names.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Component sectorLabel(Property<?> property, BlockState candidate, BlockState pendingBase) {
        String name = property.getName();
        if (isAxleProperty(name)) {
            boolean alongFirst = (Boolean) candidate.getValue(property);
            Direction facing = extractFacing(pendingBase);
            return axleLabel(facing, alongFirst);
        }
        if ("axis".equals(name)) {
            Object value = candidate.getValue(property);
            if (value instanceof Direction.Axis axis) {
                return axisLabel(axis);
            }
        }
        if ("spin".equals(name)) {
            Object value = candidate.getValue(property);
            int spinInt = value instanceof Number number ? number.intValue() : -1;
            Direction up = spinUpDirection(extractFacing(pendingBase), spinInt);
            if (up != null) {
                return directionLabel(up);
            }
        }
        Object value = candidate.getValue(property);
        if (value instanceof Direction direction) {
            return directionLabel(direction);
        }
        return Component.literal(((Property) property).getName(candidate.getValue(property)));
    }

    /**
     * Local-up world direction for AE2-style spin 0..3, or null if out of range.
     */
    public static Direction spinUpDirection(Direction facing, int spin) {
        if (facing == null || spin < 0 || spin > 3) {
            return null;
        }
        return SPIN_UP_DIRECTIONS[facing.ordinal()][spin];
    }

    public static Component directionLabel(Direction direction) {
        return Component.translatable("item.iska_utils.swiss_wrench.radial.dir." + direction.getSerializedName());
    }

    /**
     * Shaft axis label matching Create DirectionalAxisKineticBlock#getRotationAxis:
     * facing X → Y (up-down) or Z (N-S); facing Y → X (E-W) or Z (N-S); facing Z → X (E-W) or Y (up-down).
     */
    public static Component axleLabel(Direction facing, boolean alongFirst) {
        Direction.Axis faceAxis = facing != null ? facing.getAxis() : Direction.Axis.Y;
        Direction.Axis shaftAxis = switch (faceAxis) {
            case X -> alongFirst ? Direction.Axis.Y : Direction.Axis.Z;
            case Y -> alongFirst ? Direction.Axis.X : Direction.Axis.Z;
            case Z -> alongFirst ? Direction.Axis.X : Direction.Axis.Y;
        };
        return axisLabel(shaftAxis);
    }

    public static Component axisLabel(Direction.Axis axis) {
        return switch (axis) {
            case X -> Component.translatable("item.iska_utils.swiss_wrench.radial.axle.east_west");
            case Z -> Component.translatable("item.iska_utils.swiss_wrench.radial.axle.north_south");
            case Y -> Component.translatable("item.iska_utils.swiss_wrench.radial.axle.up_down");
        };
    }

    private static Direction extractFacing(BlockState state) {
        Property<?> facing = findProperty(state, "facing");
        if (facing != null) {
            Object value = state.getValue(facing);
            if (value instanceof Direction direction) {
                return direction;
            }
        }
        Property<?> horizontal = findProperty(state, "horizontal_facing");
        if (horizontal != null) {
            Object value = state.getValue(horizontal);
            if (value instanceof Direction direction) {
                return direction;
            }
        }
        return Direction.NORTH;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<BlockState> applyNamedValue(BlockState state, String propertyName, String valueName) {
        for (Property<?> property : state.getProperties()) {
            if (!property.getName().equals(propertyName)) {
                continue;
            }
            Optional<? extends Comparable<?>> parsed = property.getValue(valueName);
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(state.setValue((Property) property, (Comparable) parsed.get()));
        }
        return Optional.empty();
    }

    /**
     * Apply multiple property/value pairs in order (nested radial final submit).
     */
    public static Optional<BlockState> applyNamedValues(BlockState state, List<String> propertyNames, List<String> valueNames) {
        if (propertyNames == null || valueNames == null || propertyNames.size() != valueNames.size()) {
            return Optional.empty();
        }
        BlockState current = state;
        for (int i = 0; i < propertyNames.size(); i++) {
            String propertyName = propertyNames.get(i);
            if (!isRegistered(propertyName)) {
                return Optional.empty();
            }
            Optional<BlockState> next = applyNamedValue(current, propertyName, valueNames.get(i));
            if (next.isEmpty()) {
                return Optional.empty();
            }
            current = next.get();
        }
        return Optional.of(current);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String valueName(Property<?> property, BlockState state) {
        return ((Property) property).getName(state.getValue(property));
    }
}
