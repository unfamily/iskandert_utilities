package net.unfamily.iskautils.network.packet;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.structure.StructureLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Packet per aprire la GUI di selezione struttura dal client al server
 */
public class StructurePlacerGuiOpenC2SPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructurePlacerGuiOpenC2SPacket.class);
    
    /**
     * Crea un nuovo packet per aprire la GUI
     */
    public StructurePlacerGuiOpenC2SPacket() {
        // Packet vuoto, serve solo per segnalare l'apertura della GUI
    }
    
    /**
     * Gestisce il packet sul lato server
     * @param player Il giocatore che ha inviato il packet
     */
    public void handle(ServerPlayer player) {
        if (player == null) {
            LOGGER.error("Server player is null while handling StructurePlacerGuiOpenC2SPacket");
            return;
        }
        
        // Invia un messaggio temporaneo - la GUI verrà aperta lato client
        player.displayClientMessage(Component.literal("§6Opening Structure Selector GUI..."), true);
        
        // TODO: Implementare packet S2C per aprire la GUI personalizzata lato client
        // Per ora usiamo il sistema di lista testuale come fallback
        showStructureList(player);
    }
    
    /**
     * Mostra la lista delle strutture disponibili come messaggi di chat
     */
    private void showStructureList(ServerPlayer player) {
        Map<String, StructureDefinition> structures = StructureLoader.getAllStructures();
        
        if (structures.isEmpty()) {
            player.displayClientMessage(Component.literal("§cNo structures available!"), false);
            return;
        }
        
        player.displayClientMessage(Component.literal("§6===== Available Structures ====="), false);
        
        for (StructureDefinition structure : structures.values()) {
            String name = structure.getName() != null ? structure.getName() : structure.getId();
            
            // Crea un messaggio cliccabile che seleziona la struttura
            Component message = Component.literal("§a[SELECT] §f" + name)
                .append(Component.literal(" §7(" + structure.getId() + ")"));
            
            if (structure.getDescription() != null && !structure.getDescription().isEmpty()) {
                String desc = String.join(" ", structure.getDescription());
                message = message.copy().append(Component.literal(" §8- " + desc));
            }
            
            player.displayClientMessage(message, false);
        }
        
        player.displayClientMessage(Component.literal("§7Use /iska_utils_structure info <id> for details"), false);
        player.displayClientMessage(Component.literal("§7TODO: Implement proper GUI"), false);
    }
    
    /**
     * Metodo di compatibilità per quando i packet completi saranno implementati
     */
    public static void handlePacket(StructurePlacerGuiOpenC2SPacket packet, ServerPlayer player) {
        packet.handle(player);
    }
} 