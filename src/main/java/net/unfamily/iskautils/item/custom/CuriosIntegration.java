package net.unfamily.iskautils.item.custom;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.unfamily.iskautils.item.custom.relic.CursedRelicItem;
import net.unfamily.iskautils.item.custom.relic.TheDeceptionItem;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Class to handle integration with Curios API.
 * This code will only run when Curios is present.
 * Simplified implementation to avoid compatibility issues.
 */
public class CuriosIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(CuriosIntegration.class);

    /**
     * Registers the necessary events for Curios integration
     * @param eventBus Mod event bus
     */
    public static void register(IEventBus eventBus) {
        // Register this class only if Curios is loaded
        if (ModUtils.isCuriosLoaded()) {
            // LOGGER.info("Registering Curios integration");
            eventBus.addListener(CuriosIntegration::setupCurios);
        }
    }

    /**
     * Method called during setup to integrate with Curios
     * Uses FMLCommonSetupEvent which is a valid event for the mod bus
     */
    private static void setupCurios(FMLCommonSetupEvent event) {
        // Register Vector Charm as curio
        VectorCharmCurioHandler.register();
        
        // Register Portable Dislocator as curio
        PortableDislocatorCurioHandler.register();
        
        // Register Necrotic Crystal Heart as curio
        NecroticCrystalHeartCurioHandler.register();
        
        // Register Fanpack as curio
        FanpackCurioHandler.register();

        registerCursedRelicUnequipAllowance();
    }

    /**
     * Cursed relics must always be removable from Curios (no lock-on-equip behavior).
     */
    private static void registerCursedRelicUnequipAllowance() {
        try {
            Class<?> eventClass = Class.forName("top.theillusivec4.curios.api.event.CurioCanUnequipEvent");
            Class<?> triStateClass = Class.forName("net.neoforged.neoforge.common.util.TriState");
            Method getStackMethod = eventClass.getMethod("getStack");
            Method setResultMethod = eventClass.getMethod("setResult", triStateClass);
            @SuppressWarnings("unchecked")
            Object allowUnequip = Enum.valueOf((Class<Enum>) triStateClass, "TRUE");

            NeoForge.EVENT_BUS.addListener(LivingEvent.class, event -> {
                if (!eventClass.isInstance(event)) {
                    return;
                }
                try {
                    ItemStack stack = (ItemStack) getStackMethod.invoke(event);
                    if (stack.getItem() instanceof CursedRelicItem || stack.getItem() instanceof TheDeceptionItem) {
                        setResultMethod.invoke(event, allowUnequip);
                    }
                } catch (ReflectiveOperationException e) {
                    LOGGER.debug("Cursed relic curio unequip handler failed: {}", e.toString());
                }
            });
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Could not register cursed relic curio unequip handler", e);
        }
    }
} 