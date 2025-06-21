package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet per dire al client di salvare la struttura localmente dopo la validazione del server
 */
public class StructureSaverMachineClientSaveS2CPacket {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSaverMachineClientSaveS2CPacket.class);
    
    private final String structureName;
    private final String structureId;
    private final BlockPos vertex1;
    private final BlockPos vertex2;
    private final BlockPos center;
    
    public StructureSaverMachineClientSaveS2CPacket(String structureName, String structureId, 
                                                   BlockPos vertex1, BlockPos vertex2, BlockPos center) {
        this.structureName = structureName;
        this.structureId = structureId;
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.center = center;
    }
    
    /**
     * Invia il packet al client per il salvataggio locale
     */
    public static void send(ServerPlayer player, String structureName, String structureId, 
                           BlockPos vertex1, BlockPos vertex2, BlockPos center) {
        LOGGER.info("Sending client save packet for structure: {} ({})", structureName, structureId);
        
        // Sistema semplificato per compatibilità single player
        try {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                handleClient(structureName, structureId, vertex1, vertex2, center);
            });
        } catch (Exception e) {
            // Ignora errori quando si esegue su server dedicato
            LOGGER.debug("Packet not sent in dedicated server mode: {}", e.getMessage());
        }
    }
    
    /**
     * Gestisce il packet lato client - salva la struttura localmente
     */
    private static void handleClient(String structureName, String structureId, 
                                   BlockPos vertex1, BlockPos vertex2, BlockPos center) {
        try {
            LOGGER.info("Processing client save request for structure: {} ({})", structureName, structureId);
            
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.error("Client level is null, cannot save structure");
                return;
            }
            
            // Esegui il salvataggio della struttura lato client
            net.unfamily.iskautils.client.ClientStructureSaver.saveStructure(
                structureName, structureId, vertex1, vertex2, center, level);
                
            LOGGER.info("Structure saved successfully on client");
            
            // Mostra messaggio di successo al giocatore
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§aStruttura salvata: §f" + structureName), 
                    true);
            }
            
        } catch (Exception e) {
            LOGGER.error("Errore durante il salvataggio client della struttura: {}", e.getMessage(), e);
            
            // Mostra messaggio di errore al giocatore
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cErrore nel salvataggio: " + e.getMessage()), 
                    true);
            }
        }
    }
} 