package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
// import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.structure.StructureLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet per sincronizzare le strutture dal server al client
 */
public record StructureSyncS2CPacket(Map<String, String> structureData, boolean acceptClientStructures) implements CustomPacketPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSyncS2CPacket.class);
    public static final Type<StructureSyncS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "structure_sync"));
    
    public static final StreamCodec<FriendlyByteBuf, StructureSyncS2CPacket> STREAM_CODEC = StreamCodec.composite(
        // Serializza la mappa di strutture come stringhe JSON
        StreamCodec.<FriendlyByteBuf, Map<String, String>>of(
            (buf, map) -> {
                buf.writeInt(map.size());
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    buf.writeUtf(entry.getKey());
                    buf.writeUtf(entry.getValue());
                }
            },
            (buf) -> {
                int size = buf.readInt();
                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    String key = buf.readUtf();
                    String value = buf.readUtf();
                    map.put(key, value);
                }
                return map;
            }
        ),
        StructureSyncS2CPacket::structureData,
        // Serializza il flag acceptClientStructures
        StreamCodec.<FriendlyByteBuf, Boolean>of(
            (buf, flag) -> buf.writeBoolean(flag),
            (buf) -> buf.readBoolean()
        ),
        StructureSyncS2CPacket::acceptClientStructures,
        StructureSyncS2CPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * Gestisce il pacchetto lato client (versione semplificata)
     */
    public static void handle(StructureSyncS2CPacket packet) {
        try {
            LOGGER.info("Ricevuta sincronizzazione strutture dal server: {} strutture", packet.structureData.size());
            
            // Sincronizza le strutture ricevute dal server nel StructureLoader client-side
            StructureLoader.syncFromServer(packet.structureData, packet.acceptClientStructures);
            
            LOGGER.info("Strutture sincronizzate con successo dal server");
        } catch (Exception e) {
            LOGGER.error("Errore durante la sincronizzazione delle strutture: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Crea un pacchetto con le strutture del server
     */
    public static StructureSyncS2CPacket create(Map<String, StructureDefinition> serverStructures, boolean acceptClientStructures) {
        Map<String, String> structureData = new HashMap<>();
        
        for (Map.Entry<String, StructureDefinition> entry : serverStructures.entrySet()) {
            try {
                // Serializza la definizione della struttura in JSON
                String jsonData = entry.getValue().toJson();
                structureData.put(entry.getKey(), jsonData);
            } catch (Exception e) {
                LOGGER.error("Errore nella serializzazione della struttura {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        return new StructureSyncS2CPacket(structureData, acceptClientStructures);
    }
} 