package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.util.StructureSaverClientAccess;

/**
 * Simplified blueprint sync packet for integrated-server fallback.
 */
public class StructureSaverBlueprintSyncS2CPacket {

    private final BlockPos machinePos;
    private final BlockPos vertex1;
    private final BlockPos vertex2;
    private final BlockPos center;

    public StructureSaverBlueprintSyncS2CPacket(BlockPos machinePos, BlockPos vertex1, BlockPos vertex2, BlockPos center) {
        this.machinePos = machinePos;
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.center = center;
    }

    public static void send(ServerPlayer player, BlockPos machinePos, BlockPos vertex1, BlockPos vertex2, BlockPos center) {
        StructureSaverClientAccess.syncBlueprint(machinePos, vertex1, vertex2, center);
    }
}
