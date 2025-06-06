package net.unfamily.iskautils.worldgen.tree;

import net.minecraft.world.level.block.grower.TreeGrower;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.worldgen.ModConfiguredFeatures;

import java.util.Optional;

/**
 * Class that contains all the TreeGrower used in the mod. 
 */
public class ModTreeGrowers {
    /**
     * TreeGrower for the rubber tree (rubber tree).
     * Note: the TreeGrower class is final, so we cannot extend it or override it.
     * To customize the generation, we use the BlockEvent.NeighborNotifyEvent
     * in ModEvents.java to intercept when a sapling grows.
     */
    public static final TreeGrower RUBBER = new TreeGrower(
            IskaUtils.MOD_ID + ":rubber",  // ID of the TreeGrower
            Optional.empty(),               // No mega jungle feature
            Optional.of(ModConfiguredFeatures.RUBBER_KEY), // Use the standard tree configuration
            Optional.empty()                // No fancy configuration
    );
} 