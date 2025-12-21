package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.DeepDrawersBlockEntity;

/**
 * Event handler per prevenire la rottura dei Deep Drawers quando contengono oggetti.
 * Usa PlayerInteractEvent.LeftClickBlock per intercettare quando il giocatore inizia a rompere il blocco
 * e cancellare l'evento prima che inizi il processo di rottura, prevenendo il problema dove il blocco
 * potrebbe svuotarsi durante la rottura e poi scomparire senza droppare.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class DeepDrawerBreakEventHandler {
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // Controlla se il blocco cliccato è un Deep Drawer
        BlockState blockState = event.getLevel().getBlockState(event.getPos());
        if (!blockState.is(ModBlocks.DEEP_DRAWERS.get())) {
            return;
        }

        // Solo lato server
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);
        
        // Controlla se il BlockEntity è un DeepDrawersBlockEntity e contiene oggetti
        if (blockEntity instanceof DeepDrawersBlockEntity deepDrawers) {
            if (deepDrawers.hasItems()) {
                // Cancella l'evento per prevenire completamente l'inizio della rottura
                event.setCanceled(true);
                
                // Mostra messaggio al giocatore
                player.displayClientMessage(
                    Component.translatable("message.iska_utils.deep_drawers.cannot_break"),
                    true // actionbar
                );
            }
        }
    }
}

