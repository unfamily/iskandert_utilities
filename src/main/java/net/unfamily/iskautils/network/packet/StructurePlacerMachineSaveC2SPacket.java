package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.structure.StructureLoader;
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
            player.displayClientMessage(Component.literal("§cInvalid structure ID!"), true);
            return;
        }
        
        if (machinePos == null) {
            player.displayClientMessage(Component.literal("§cInvalid machine position!"), true);
            return;
        }
        
        // Verifica che la struttura esista
        StructureDefinition structure = StructureLoader.getStructure(structureId);
        if (structure == null) {
            player.displayClientMessage(Component.literal("§cStructure not found: " + structureId), true);
            return;
        }
        
        // Trova la macchina alla posizione specificata
        LOGGER.info("Looking for Structure Placer Machine at position: {}", machinePos);
        LOGGER.info("Player level: {}", player.level().dimension().location());
        LOGGER.info("Player position: {}", player.blockPosition());
        
        BlockEntity blockEntity = player.level().getBlockEntity(machinePos);
        LOGGER.info("Found BlockEntity: {} (type: {})", 
                   blockEntity != null ? "YES" : "NO", 
                   blockEntity != null ? blockEntity.getClass().getSimpleName() : "NULL");
        
        if (blockEntity != null && !(blockEntity instanceof StructurePlacerMachineBlockEntity)) {
            LOGGER.warn("BlockEntity is not a StructurePlacerMachineBlockEntity, it's: {}", blockEntity.getClass().getName());
        }
        
        if (!(blockEntity instanceof StructurePlacerMachineBlockEntity machineEntity)) {
            player.displayClientMessage(Component.literal("§cStructure Placer Machine not found at position: " + machinePos + " (found: " + (blockEntity != null ? blockEntity.getClass().getSimpleName() : "null") + ")"), true);
            return;
        }
        
        // Salva la struttura selezionata nella macchina (sul server)
        machineEntity.setSelectedStructure(structureId);
        
        // Informa il giocatore del successo
        String structureName = structure.getName() != null ? structure.getName() : structure.getId();
        player.displayClientMessage(
            Component.literal("§aMachine structure set to: §f" + structureName + " §7(" + structure.getId() + ")"), 
            true);
        
        LOGGER.debug("Player {} saved structure {} to Structure Placer Machine at {}", 
                    player.getName().getString(), structureId, machinePos);
    }
} 