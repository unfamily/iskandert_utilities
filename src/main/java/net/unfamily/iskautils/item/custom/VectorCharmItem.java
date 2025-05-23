package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

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
     * Questo metodo viene chiamato ad ogni tick per ogni item nell'inventario
     */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // Eseguiamo solo sul client e solo per i giocatori
        if (level.isClientSide && entity instanceof Player player) {
            // Il movimento effettivo viene applicato tramite ClientEvents
            // Questo avviene in modo centralizzato ad ogni tick tramite ClientEvents.checkKeysInClientThread()
            // e VectorCharmMovement.applyMovement(player)
        }
    }
    
    /**
     * Controlla se il giocatore ha un Vector Charm equipaggiato o in mano
     * @param player Il giocatore da controllare
     * @return true se il giocatore ha un Vector Charm
     */
    public static boolean hasVectorCharm(Player player) {
        // Controlla le mani (priorità più alta)
        if (player.getMainHandItem().getItem() instanceof VectorCharmItem ||
            player.getOffhandItem().getItem() instanceof VectorCharmItem) {
            return true;
        }
        
        // Se Curios è caricato, controlla gli slot di Curios (seconda priorità)
        if (ModUtils.isCuriosLoaded() && checkCuriosSlots(player)) {
            return true;
        }
        
        // Controlla l'inventario del giocatore (priorità più bassa)
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof VectorCharmItem) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Utilizza reflection per controllare se il Vector Charm è equipaggiato in uno slot Curios
     */
    private static boolean checkCuriosSlots(Player player) {
        try {
            // Utilizziamo la reflection per accedere a Curios API
            Class<?> curioApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            
            // Ottieni il metodo per controllare un item specifico
            Method hasItemMethod = null;
            try {
                // Prima prova con un metodo più diretto se disponibile
                hasItemMethod = curioApiClass.getMethod("hasAnyEquipped", LivingEntity.class, java.util.function.Predicate.class);
                if (hasItemMethod != null) {
                    // Crea un predicate per VectorCharmItem
                    java.util.function.Predicate<ItemStack> predicate = 
                        (ItemStack stack) -> stack.getItem() instanceof VectorCharmItem;
                    
                    // Invoca il metodo
                    return (Boolean) hasItemMethod.invoke(null, player, predicate);
                }
            } catch (NoSuchMethodException e) {
                // Metodo non trovato, procedi con l'approccio alternativo
            }
            
            // Approccio alternativo: scansiona manualmente gli slot
            Method getCuriosMethod = curioApiClass.getMethod("getCurios", LivingEntity.class);
            Object curiosHelper = getCuriosMethod.invoke(null, player);

            if (curiosHelper != null) {
                // Controlla ogni slot curio
                Class<?> iCurioHelperClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICurioHelper");
                Method getEquippedCuriosMethod = iCurioHelperClass.getMethod("getEquippedCurios", LivingEntity.class);
                Object equippedCurios = getEquippedCuriosMethod.invoke(curiosHelper, player);

                if (equippedCurios instanceof Iterable<?> curios) {
                    for (Object curio : curios) {
                        // Ottieni l'ItemStack del curio
                        Method getStackMethod = curio.getClass().getMethod("getStack");
                        ItemStack stack = (ItemStack) getStackMethod.invoke(curio);
                        
                        if (stack.getItem() instanceof VectorCharmItem) {
                            LOGGER.debug("Found Vector Charm in Curios slot for player {}", player.getScoreboardName());
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Se c'è un errore nella reflection, logga e continua
            LOGGER.error("Error checking Curios slots: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        
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