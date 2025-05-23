package net.unfamily.iskautils.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.data.VectorFactorType;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestisce i keybindings per la mod
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT)
public class KeyBindings {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyBindings.class);
    
    public static final String KEY_CATEGORY_ISKA_UTILS = "key.category.iska_utils";
    public static final String KEY_VECTOR_VERTICAL = "key.iska_utils.vector_vertical";
    public static final String KEY_VECTOR_HORIZONTAL = "key.iska_utils.vector_horizontal";

    public static final KeyMapping VECTOR_VERTICAL_KEY = new KeyMapping(
            KEY_VECTOR_VERTICAL,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,  // Tasto V per la regolazione verticale
            KEY_CATEGORY_ISKA_UTILS
    );

    public static final KeyMapping VECTOR_HORIZONTAL_KEY = new KeyMapping(
            KEY_VECTOR_HORIZONTAL,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,  // Tasto B per la regolazione orizzontale
            KEY_CATEGORY_ISKA_UTILS
    );

    /**
     * Registra i key bindings
     */
    @EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class KeybindHandler {
        @SubscribeEvent
        public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
            LOGGER.info("Registrando i keybindings per Vector Charm");
            event.register(VECTOR_VERTICAL_KEY);
            event.register(VECTOR_HORIZONTAL_KEY);
        }
    }

    /**
     * Gestisce la pressione dei tasti e mostra messaggi al client
     */
    public static void checkKeys() {
        // Verifica se il giocatore ha premuto i tasti del Vector Charm
        if (Minecraft.getInstance().player != null) {
            Player player = Minecraft.getInstance().player;
            
            // Tasto per la regolazione verticale
            if (VECTOR_VERTICAL_KEY.consumeClick()) {
                LOGGER.debug("Vector Charm vertical key pressed");
                
                // Simuliamo il cambiamento del valore verticale (in un'implementazione completa,
                // questo invierebbe un pacchetto al server e il server risponderebbe col nuovo valore)
                byte currentFactor = VectorCharmData.getInstance().getVerticalFactor(player);
                byte nextFactor = (byte) ((currentFactor + 1) % 6); // 0-5 (None, Slow, Moderate, Fast, Extreme, Ultra)
                VectorCharmData.getInstance().setVerticalFactor(player, nextFactor);
                VectorFactorType newFactor = VectorFactorType.fromByte(nextFactor);
                
                // Invia un messaggio al giocatore
                player.displayClientMessage(Component.translatable("message.iska_utils.vector_vertical_factor", 
                                     Component.translatable("vectorcharm.factor." + newFactor.getName())), true);
            }

            // Tasto per la regolazione orizzontale
            if (VECTOR_HORIZONTAL_KEY.consumeClick()) {
                LOGGER.debug("Vector Charm horizontal key pressed");
                
                // Simuliamo il cambiamento del valore orizzontale
                byte currentFactor = VectorCharmData.getInstance().getHorizontalFactor(player);
                byte nextFactor = (byte) ((currentFactor + 1) % 6); // 0-5 (None, Slow, Moderate, Fast, Extreme, Ultra)
                VectorCharmData.getInstance().setHorizontalFactor(player, nextFactor);
                VectorFactorType newFactor = VectorFactorType.fromByte(nextFactor);
                
                // Invia un messaggio al giocatore
                player.displayClientMessage(Component.translatable("message.iska_utils.vector_horizontal_factor", 
                                     Component.translatable("vectorcharm.factor." + newFactor.getName())), true);
            }
        }
    }
} 