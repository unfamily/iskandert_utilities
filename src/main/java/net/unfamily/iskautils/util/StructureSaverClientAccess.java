package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;

public final class StructureSaverClientAccess {
    private static final String BLUEPRINT_CLIENT = "net.unfamily.iskautils.client.StructureSaverBlueprintSyncClient";
    private static final String CLIENT_SAVE_HANDLER = "net.unfamily.iskautils.client.StructureSaverMachineClientSaveHandler";

    private StructureSaverClientAccess() {}

    public static void syncBlueprint(BlockPos machinePos, BlockPos vertex1, BlockPos vertex2, BlockPos center) {
        ClientRuntimeAccess.runOnClientThread(() -> invoke(
                BLUEPRINT_CLIENT,
                "apply",
                new Class<?>[] {BlockPos.class, BlockPos.class, BlockPos.class, BlockPos.class},
                machinePos, vertex1, vertex2, center));
    }

    public static void saveStructureOnClient(
            String structureName,
            String structureId,
            BlockPos vertex1,
            BlockPos vertex2,
            BlockPos center,
            boolean slower,
            boolean placeAsPlayer,
            boolean isModifyOperation,
            String oldStructureId) {
        ClientRuntimeAccess.runOnClientThread(() -> invoke(
                CLIENT_SAVE_HANDLER,
                "apply",
                new Class<?>[] {
                        String.class, String.class, BlockPos.class, BlockPos.class, BlockPos.class,
                        boolean.class, boolean.class, boolean.class, String.class
                },
                structureName, structureId, vertex1, vertex2, center, slower, placeAsPlayer, isModifyOperation, oldStructureId));
    }

    private static void invoke(String className, String method, Class<?>[] paramTypes, Object... args) {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return;
        }
        try {
            Class<?> clz = Class.forName(className);
            clz.getMethod(method, paramTypes).invoke(null, args);
        } catch (Throwable ignored) {
        }
    }
}
