package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Il Necrotic Crystal Heart è un item che, quando indossato come curio,
 * intercetta gli eventi di danno e può modificarne il comportamento.
 * Blocca completamente tutti i danni quando è equipaggiato.
 */
public class NecroticCrystalHeartItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(NecroticCrystalHeartItem.class);
    
    // Impostazione per la riduzione del danno
    private static final float DAMAGE_REDUCTION = 0.0f; // Riduzione completa (0% del danno originale)
    
    public NecroticCrystalHeartItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // Aggiungi l'effetto 'incantato' al cuore
        return true;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        tooltipComponents.add(Component.literal("§5Protegge dai danni quando equipaggiato"));
        tooltipComponents.add(Component.literal("§7Nessun consumo di energia richiesto"));
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
            // Riduce il danno in base alla costante DAMAGE_REDUCTION (0% = nessun danno)
            float reducedDamage = damageAmount * DAMAGE_REDUCTION;
            
            // Visualizza messaggio di protezione
            if (player.level().isClientSide) {
                player.displayClientMessage(
                    Component.literal("§5Il Necrotic Crystal Heart ti ha protetto!"), 
                    true // actionbar
                );
            }
            
            LOGGER.debug("Necrotic Crystal Heart ha ridotto il danno da {} a {} per {}", 
                    damageAmount, reducedDamage, player.getName().getString());
            
            return reducedDamage;
        }
        
        // Per entità non giocatore, lascia il danno invariato
        return damageAmount;
    }
} 