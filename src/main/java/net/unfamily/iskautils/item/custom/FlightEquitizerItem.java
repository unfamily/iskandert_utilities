package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Amuleto Gravitazionale - annulla il rallentamento della velocità di mining quando si vola in Minecraft
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class FlightEquitizerItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlightEquitizerItem.class);
    
    public FlightEquitizerItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // Aggiungiamo l'effetto "incantato" all'amuleto
        return true;
    }
    
    /**
     * Questo metodo viene chiamato ogni tick per ogni oggetto nell'inventario
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // Non facciamo nulla qui, la funzionalità è gestita attraverso l'evento
    }
    
    /**
     * Gestisce l'evento di mining quando il giocatore sta volando
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        
        // Se il giocatore sta a terra o è in acqua, non facciamo nulla
        if (player.onGround() || player.isInWater()) {
            return;
        }
        
        // Controlla se il giocatore ha l'amuleto nell'inventario
        if (hasGravityAmulet(player)) {
            // Annulla la penalità di velocità applicata quando il giocatore vola
            // Minecraft normalmente applica una penalità 5x, quindi moltiplichiamo per 5
            event.setNewSpeed(event.getOriginalSpeed() * 5.0F);
        }
    }
    
    /**
     * Controlla se il giocatore ha l'amuleto nell'inventario
     */
    private static boolean hasGravityAmulet(Player player) {
        // Controlla mano principale
        if (player.getMainHandItem().getItem() instanceof FlightEquitizerItem) {
            return true;
        }
        
        // Controlla mano secondaria
        if (player.getOffhandItem().getItem() instanceof FlightEquitizerItem) {
            return true;
        }
        
        // Controlla inventario
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof FlightEquitizerItem) {
                return true;
            }
        }
        
        return false;
    }
} 