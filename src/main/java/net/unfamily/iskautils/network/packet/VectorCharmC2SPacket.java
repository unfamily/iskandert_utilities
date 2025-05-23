package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.data.VectorFactorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Pacchetto client-to-server per gestire il cambio di impostazioni del Vector Charm.
 * Versione semplificata che non dipende da NetworkEvent
 */
public class VectorCharmC2SPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmC2SPacket.class);
    private final boolean isVertical;

    /**
     * Costruttore per decodificare il pacchetto dal buffer
     */
    public VectorCharmC2SPacket(FriendlyByteBuf buf) {
        this.isVertical = buf.readBoolean();
    }

    /**
     * Costruttore standard
     * @param isVertical true se è il fattore verticale, false per quello orizzontale
     */
    public VectorCharmC2SPacket(boolean isVertical) {
        this.isVertical = isVertical;
    }

    /**
     * Codifica il pacchetto nel buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isVertical);
    }

    /**
     * Gestisce la ricezione del pacchetto sul server
     */
    public void handle(ServerPlayer player) {
        if (player == null) {
            LOGGER.error("Server player is null while handling VectorCharmC2SPacket");
            return;
        }
        
        ServerLevel level = player.serverLevel();

        // Ottieni i dati persistenti del Vector Charm
        VectorCharmData charmData = VectorCharmData.get(level);
        
        // Aggiorna il valore del fattore (verticale o orizzontale)
        VectorFactorType newFactor;
        if (isVertical) {
            // Cicla al fattore verticale successivo
            byte currentFactor = charmData.getVerticalFactor(player);
            byte nextFactor = (byte) ((currentFactor + 1) % 6); // 0-5 (None, Slow, Moderate, Fast, Extreme, Ultra)
            charmData.setVerticalFactor(player, nextFactor);
            newFactor = VectorFactorType.fromByte(nextFactor);
            
            // Invia un messaggio al giocatore
            player.sendSystemMessage(Component.translatable("message.iska_utils.vector_vertical_factor", 
                                 Component.translatable("vectorcharm.factor." + newFactor.getName())));
            
            LOGGER.info("Player {} set vertical factor to {}", player.getScoreboardName(), newFactor.getName());
        } else {
            // Cicla al fattore orizzontale successivo
            byte currentFactor = charmData.getHorizontalFactor(player);
            byte nextFactor = (byte) ((currentFactor + 1) % 6); // 0-5 (None, Slow, Moderate, Fast, Extreme, Ultra)
            charmData.setHorizontalFactor(player, nextFactor);
            newFactor = VectorFactorType.fromByte(nextFactor);
            
            // Invia un messaggio al giocatore
            player.sendSystemMessage(Component.translatable("message.iska_utils.vector_horizontal_factor", 
                                 Component.translatable("vectorcharm.factor." + newFactor.getName())));
            
            LOGGER.info("Player {} set horizontal factor to {}", player.getScoreboardName(), newFactor.getName());
        }
    }
    
    /**
     * Metodo di compatibilità per quando verranno implementati i pacchetti di rete completi
     * Questo è un metodo stub che consente di mantenere una firma simile a quella che sarà usata
     * con l'implementazione completa di NetworkEvent
     */
    public boolean handle(Supplier<?> contextSupplier) {
        LOGGER.debug("Handling VectorCharmC2SPacket (stub method)");
        return true;
    }
} 