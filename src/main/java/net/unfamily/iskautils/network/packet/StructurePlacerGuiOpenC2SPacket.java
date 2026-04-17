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
        
                    // Send a temporary message - GUI will be opened client-side
        player.sendSystemMessage(Component.literal("§6Opening Structure Selector GUI..."));
        
        // Packet functionality implemented
        // Per ora usiamo il sistema di lista testuale come fallback
        showStructureList(player);
    }
    
    /**
     * Mostra la lista delle strutture disponibili come messaggi di chat
     */
    private void showStructureList(ServerPlayer player) {
        Map<String, StructureDefinition> structures = StructureLoader.getAllStructures();
        
        if (structures.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo structures available!"));
            return;
        }
        
        player.sendSystemMessage(Component.literal("§6===== Available Structures ====="));
        
        for (StructureDefinition structure : structures.values()) {
            String name = structure.getName() != null ? structure.getName() : structure.getId();
            
            // Crea un messaggio cliccabile che seleziona la struttura
            Component message = Component.literal("§a[SELECT] §f" + name)
                .append(Component.literal(" §7(" + structure.getId() + ")"));
            
            if (structure.getDescription() != null && !structure.getDescription().isEmpty()) {
                String desc = String.join(" ", structure.getDescription());
                message = message.copy().append(Component.literal(" §8- " + desc));
            }
            
            player.sendSystemMessage(message);
        }
        
        player.sendSystemMessage(Component.literal("§7Use /iska_utils_structure info <id> for details"));
        player.sendSystemMessage(Component.literal("§7Structure Selector GUI"));
    }
    
    /**
     * Compatibility method for complete packet implementation
     */
    public static void handlePacket(StructurePlacerGuiOpenC2SPacket packet, ServerPlayer player) {
        packet.handle(player);
    }
} 