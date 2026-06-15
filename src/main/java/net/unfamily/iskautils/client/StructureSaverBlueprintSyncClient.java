package net.unfamily.iskautils.client;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity;

public final class StructureSaverBlueprintSyncClient {
    private static final ModLogger LOGGER = ModLogger.of(StructureSaverBlueprintSyncClient.class);

    private StructureSaverBlueprintSyncClient() {}

    public static void apply(BlockPos machinePos, BlockPos vertex1, BlockPos vertex2, BlockPos center) {
        try {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                var blockEntity = level.getBlockEntity(machinePos);
                if (blockEntity instanceof StructureSaverMachineBlockEntity structureSaver) {
                    structureSaver.setBlueprintDataClientSide(vertex1, vertex2, center);
                } else {
                    LOGGER.warn("BlockEntity at {} is not a StructureSaverMachineBlockEntity", machinePos);
                }
            } else {
                LOGGER.warn("Client level is null, cannot sync blueprint data");
            }
        } catch (Exception e) {
            LOGGER.error("Error applying blueprint sync on client: {}", e.getMessage(), e);
        }
    }
}
