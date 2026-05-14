package net.unfamily.iskautils.structure;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Stable placement order for the structure placer machine. HashMap iteration is undefined.
 * Order: bottom to top (ascending world Y), then within each horizontal layer X then Z — same as
 * the pattern array traversal in {@code calculateStructurePositions} (loops y, x, z).
 */
public final class StructureBlockPlaceOrder {

    public static final Comparator<BlockPos> BOTTOM_UP_THEN_X_THEN_Z =
            Comparator.<BlockPos>comparingInt(p -> p.getY())
                    .thenComparingInt(p -> p.getX())
                    .thenComparingInt(p -> p.getZ());

    private StructureBlockPlaceOrder() {}

    public static <V> List<Map.Entry<BlockPos, V>> sortedEntries(Map<BlockPos, V> map) {
        List<Map.Entry<BlockPos, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByKey(BOTTOM_UP_THEN_X_THEN_Z));
        return list;
    }
}
