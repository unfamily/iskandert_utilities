package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Il Necrotic Crystal Heart è un item che, quando indossato come curio,
 * intercetta gli eventi di danno e può modificarne il comportamento.
 */
public class NecroticCrystalHeartItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(NecroticCrystalHeartItem.class);
    
    public NecroticCrystalHeartItem(Properties properties) {
        super(properties);
    }
    
    /**
     * Gestisce la logica quando un'entità che indossa il cuore subisce danno.
     * Questo metodo è chiamato dall'handler degli eventi quando viene rilevato che
     * l'entità che subisce danno indossa il Necrotic Crystal Heart.
     * 
     * @param stack Lo stack dell'item
     * @param entity L'entità che indossa il cuore
     * @param damageAmount La quantità di danno originale
     * @return La nuova quantità di danno da applicare, o -1 per annullare l'evento
     */
    public static float onDamageReceived(ItemStack stack, LivingEntity entity, float damageAmount) {
        if (entity instanceof Player player) {
            // Esempio di logica: riduce il danno del 25%
            float reducedDamage = damageAmount * 0.0f;
            
            // Applica un effetto visivo per mostrare l'attivazione
            // In un'implementazione reale, qui potresti aggiungere particelle, suoni, ecc.
            
            LOGGER.debug("Necrotic Crystal Heart ha ridotto il danno da {} a {} per {}", 
                    damageAmount, reducedDamage, player.getName().getString());
            
            return reducedDamage;
        }
        
        // Per entità non giocatore, lascia il danno invariato
        return damageAmount;
    }
} 