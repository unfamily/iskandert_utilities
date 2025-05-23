package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Item speciale per il Vector Charm che può essere indossato come Curio quando disponibile.
 * Quando indossato o tenuto in mano, fornisce una maggiore velocità alle placche vettoriali.
 */
public class VectorCharmItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmItem.class);

    /**
     * Moltiplicatore di forza quando il ciondolo è indossato o in mano
     */
    private static final float BOOST_MULTIPLIER = 1.5f;

    public VectorCharmItem(Properties properties) {
        super(properties);
        LOGGER.debug("Vector Charm item created");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Aggiungi l'effetto 'enchanted' al ciondolo
        return true;
    }
    
    /**
     * Controlla se il giocatore ha un Vector Charm equipaggiato o in mano
     * @param player Il giocatore da controllare
     * @return true se il giocatore ha un Vector Charm
     */
    public static boolean hasVectorCharm(Player player) {
        // Controlla l'inventario del giocatore
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof VectorCharmItem) {
                return true;
            }
        }
        
        // Controlla le mani
        if (player.getMainHandItem().getItem() instanceof VectorCharmItem ||
            player.getOffhandItem().getItem() instanceof VectorCharmItem) {
            return true;
        }
        
        // Se Curios è caricato, controlla gli slot di Curios, ma in questa interfaccia
        // base non possiamo accedere direttamente ai metodi di Curios
        // Questo dovrà essere implementato tramite un hook condizionale
        
        return false;
    }
    
    /**
     * Applica l'effetto boost alla velocità del vettore
     * @param entity L'entità che attraversa la placca vettoriale
     * @param motion Il vettore movimento originale
     * @return Il vettore movimento potenziato
     */
    public static Vec3 applyBoost(LivingEntity entity, Vec3 motion) {
        if (entity instanceof Player player && hasVectorCharm(player)) {
            return motion.multiply(BOOST_MULTIPLIER, BOOST_MULTIPLIER, BOOST_MULTIPLIER);
        }
        return motion;
    }
} 