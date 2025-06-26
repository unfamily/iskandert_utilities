package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet semplificato per sincronizzare i dati blueprint della Structure Saver Machine
 * Usa il sistema semplificato di ModMessages per funzionare in single player
 */
public class StructureSaverBlueprintSyncS2CPacket {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSaverBlueprintSyncS2CPacket.class);
    
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
    
    /**
     * Invia il packet al client usando il sistema semplificato (identico a ModMessages)
     */
    public static void send(ServerPlayer player, BlockPos machinePos, BlockPos vertex1, BlockPos vertex2, BlockPos center) {

        
        // Sistema semplificato identico a quello usato in ModMessages
        // Works in single player and will be ignored on dedicated server
        try {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                handleClient(machinePos, vertex1, vertex2, center);
            });
        } catch (Exception e) {
            // Ignora errori quando si esegue su server dedicato
            LOGGER.debug("Packet not sent in dedicated server mode: {}", e.getMessage());
        }
    }
    
    /**
     * Gestisce il packet lato client
     */
    private static void handleClient(BlockPos machinePos, BlockPos vertex1, BlockPos vertex2, BlockPos center) {
        try {
    
            
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) {
                var blockEntity = level.getBlockEntity(machinePos);
                if (blockEntity instanceof StructureSaverMachineBlockEntity structureSaver) {
                    // Aggiorna i dati blueprint nel BlockEntity client-side
                    structureSaver.setBlueprintDataClientSide(vertex1, vertex2, center);
            
                } else {
                    LOGGER.warn("BlockEntity at {} is not a StructureSaverMachineBlockEntity", machinePos);
                }
            } else {
                LOGGER.warn("Client level is null, cannot sync blueprint data");
            }
        } catch (Exception e) {
            LOGGER.error("Errore durante l'applicazione della sincronizzazione blueprint: {}", e.getMessage(), e);
        }
    }
} 