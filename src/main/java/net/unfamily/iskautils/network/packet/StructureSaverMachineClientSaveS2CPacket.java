package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.util.StructureSaverClientAccess;

/**
 * Packet that asks the local client to save a structure after server validation.
 */
public class StructureSaverMachineClientSaveS2CPacket {

    private final String structureName;
    private final String structureId;
    private final BlockPos vertex1;
    private final BlockPos vertex2;
    private final BlockPos center;
    private final boolean slower;
    private final boolean placeAsPlayer;
    private final boolean isModifyOperation;
    private final String oldStructureId;

    public StructureSaverMachineClientSaveS2CPacket(String structureName, String structureId,
                                                   BlockPos vertex1, BlockPos vertex2, BlockPos center,
                                                   boolean slower, boolean placeAsPlayer,
                                                   boolean isModifyOperation, String oldStructureId) {
        this.structureName = structureName;
        this.structureId = structureId;
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.center = center;
        this.slower = slower;
        this.placeAsPlayer = placeAsPlayer;
        this.isModifyOperation = isModifyOperation;
        this.oldStructureId = oldStructureId;
    }

    public static void send(ServerPlayer player, String structureName, String structureId,
                           BlockPos vertex1, BlockPos vertex2, BlockPos center,
                           boolean slower, boolean placeAsPlayer,
                           boolean isModifyOperation, String oldStructureId) {
        StructureSaverClientAccess.saveStructureOnClient(
                structureName, structureId, vertex1, vertex2, center, slower, placeAsPlayer, isModifyOperation, oldStructureId);
    }
}
