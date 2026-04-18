package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.unfamily.iskalib.structure.StructureDefinition;
import net.unfamily.iskalib.structure.StructureLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet per salvare la struttura selezionata nella Structure Placer Machine dal client al server
 */
public class StructurePlacerMachineSaveC2SPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructurePlacerMachineSaveC2SPacket.class);
    
    private final String structureId;
    private final BlockPos machinePos;
    
    /**
     * Crea un nuovo packet per salvare la struttura selezionata nella macchina
     * @param structureId L'ID della struttura da salvare
     * @param machinePos La posizione della macchina
     */
    public StructurePlacerMachineSaveC2SPacket(String structureId, BlockPos machinePos) {
        this.structureId = structureId;
        this.machinePos = machinePos;
    }
    
    /**
     * Gestisce il packet sul lato server
     * @param player Il giocatore che ha inviato il packet
     */
    public void handle(ServerPlayer player) {
        if (player == null) {
            LOGGER.error("Server player is null while handling StructurePlacerMachineSaveC2SPacket");
            return;
        }
        
        if (structureId == null || structureId.isEmpty()) {
            player.sendOverlayMessage(Component.literal("§cInvalid structure ID!"));
            return;
        }
        
        if (machinePos == null) {
            player.sendOverlayMessage(Component.literal("§cInvalid machine position!"));
            return;
        }
        
        // Verifica che la struttura esista
        StructureDefinition structure = StructureLoader.getStructure(structureId);
        if (structure == null) {
            player.sendOverlayMessage(Component.literal("§cStructure not found: " + structureId));
            return;
        }
        
        // Trova la macchina alla posizione specificata

        BlockEntity blockEntity = player.level().getBlockEntity(machinePos);
        
        if (blockEntity != null && !(blockEntity instanceof StructurePlacerMachineBlockEntity)) {
            LOGGER.warn("BlockEntity is not a StructurePlacerMachineBlockEntity, it's: {}", blockEntity.getClass().getName());
        }
        
        if (!(blockEntity instanceof StructurePlacerMachineBlockEntity machineEntity)) {
            player.sendOverlayMessage(Component.literal("§cStructure Placer Machine not found at position: " + machinePos + " (found: " + (blockEntity != null ? blockEntity.getClass().getSimpleName() : "null") + ")"));
            return;
        }
        
        // Salva la struttura selezionata nella macchina (sul server)
        machineEntity.setSelectedStructure(structureId);
        
        // Forza la sincronizzazione dei dati del menu se il player ha il menu aperto
        if (player.containerMenu instanceof net.unfamily.iskautils.client.gui.StructurePlacerMachineMenu menu) {
            menu.forceDataSync();
        }
        
        // Informa il giocatore del successo
        String structureName = structure.getName() != null ? structure.getName() : structure.getId();
        player.sendOverlayMessage(
            Component.literal("§aMachine structure set to: §f" + structureName + " §7(" + structure.getId() + ")"));
        
        LOGGER.debug("Player {} saved structure {} to Structure Placer Machine at {}", 
                    player.getName().getString(), structureId, machinePos);
    }
} 