package net.unfamily.iskautils.network.packet;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.item.custom.StructurePlacerItem;
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.structure.StructureLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet per salvare la struttura selezionata dal client al server
 */
public class StructurePlacerSaveC2SPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructurePlacerSaveC2SPacket.class);
    
    private final String structureId;
    
    /**
     * Crea un nuovo packet per salvare la struttura selezionata
     * @param structureId L'ID della struttura da salvare
     */
    public StructurePlacerSaveC2SPacket(String structureId) {
        this.structureId = structureId;
    }
    
    /**
     * Gestisce il packet sul lato server
     * @param player Il giocatore che ha inviato il packet
     */
    public void handle(ServerPlayer player) {
        if (player == null) {
            LOGGER.error("Server player is null while handling StructurePlacerSaveC2SPacket");
            return;
        }
        
        if (structureId == null || structureId.isEmpty()) {
            player.displayClientMessage(Component.literal("§cInvalid structure ID!"), true);
            return;
        }
        
        // Verifica che la struttura esista
        StructureDefinition structure = StructureLoader.getStructure(structureId);
        if (structure == null) {
            player.displayClientMessage(Component.literal("§cStructure not found: " + structureId), true);
            return;
        }
        
        // Trova l'item StructurePlacerItem nella mano del giocatore
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();
        
        ItemStack targetStack = null;
        if (mainHandItem.getItem() instanceof StructurePlacerItem) {
            targetStack = mainHandItem;
        } else if (offHandItem.getItem() instanceof StructurePlacerItem) {
            targetStack = offHandItem;
        }
        
        if (targetStack == null) {
            player.displayClientMessage(Component.literal("§cNo Structure Placer Item in hand!"), true);
            return;
        }
        
        // Salva la struttura selezionata nell'item (sul server)
        StructurePlacerItem.setSelectedStructure(targetStack, structureId);
        
        // Informa il giocatore del successo
        String structureName = structure.getName() != null ? structure.getName() : structure.getId();
        player.displayClientMessage(
            Component.literal("§aSaved structure: §f" + structureName + " §7(" + structure.getId() + ")"), 
            true);
        
        LOGGER.debug("Player {} saved structure {} to StructurePlacerItem", player.getName().getString(), structureId);
    }
    
    /**
     * Metodo di compatibilità per quando i packet completi saranno implementati
     */
    public static void handlePacket(StructurePlacerSaveC2SPacket packet, ServerPlayer player) {
        packet.handle(player);
    }
    
    /**
     * Getter per l'ID della struttura
     */
    public String getStructureId() {
        return structureId;
    }
} 