package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StructureSaverMachineClientSaveHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSaverMachineClientSaveHandler.class);

    private StructureSaverMachineClientSaveHandler() {}

    public static void apply(
            String structureName,
            String structureId,
            BlockPos vertex1,
            BlockPos vertex2,
            BlockPos center,
            boolean slower,
            boolean placeAsPlayer,
            boolean isModifyOperation,
            String oldStructureId) {
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.error("Client level is null, cannot save structure");
                return;
            }

            ClientStructureSaver.saveStructure(
                    structureName, structureId, vertex1, vertex2, center, level,
                    slower, placeAsPlayer, isModifyOperation, oldStructureId);

            var player = Minecraft.getInstance().player;
            if (player != null) {
                String operationType = isModifyOperation ? "modificata" : "salvata";
                player.sendOverlayMessage(
                        Component.translatable("gui.iska_utils.structure_saver.client_success", structureName, operationType));
            }
        } catch (Exception e) {
            LOGGER.error("Error saving structure on client: {}", e.getMessage(), e);
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendOverlayMessage(
                        Component.translatable("gui.iska_utils.structure_saver.error.client_save_failed", e.getMessage()));
            }
        }
    }
}
