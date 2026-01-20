package net.unfamily.iskautils.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.events.FanpackFlightHandler;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Class that handles Curios integration for the Fanpack.
 * This implements ICurio using reflection to avoid direct dependencies on Curios.
 */
public class FanpackCurioHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FanpackCurioHandler.class);
    
    /**
     * Registers the Fanpack as a curio.
     * Called during mod initialization if Curios is present.
     */
    public static void register() {
        if (!ModUtils.isCuriosLoaded()) return;
        
        try {
            // Registration happens through Curios API, but it's handled
            // automatically by JSON tags, so nothing special is needed here
            LOGGER.info("Fanpack registered as Curio");
        } catch (Exception e) {
            LOGGER.error("Failed to register Fanpack as Curio", e);
        }
    }
    
    /**
     * Creates an ICurio implementation for Fanpack using reflection
     */
    public static Object createCurioInstance() {
        if (!ModUtils.isCuriosLoaded()) return null;
        
        try {
            // Get ICurio interface
            Class<?> iCurioClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICurio");
            Class<?> slotContextClass = Class.forName("top.theillusivec4.curios.api.SlotContext");
            
            // Create a proxy that implements ICurio
            return java.lang.reflect.Proxy.newProxyInstance(
                FanpackCurioHandler.class.getClassLoader(),
                new Class[]{iCurioClass},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    
                    if ("curioTick".equals(methodName)) {
                        // Handle curioTick
                        Object slotContext = args[0];
                        ItemStack stack = (ItemStack) args[1];
                        
                        // Get entity from SlotContext
                        Method entityMethod = slotContextClass.getMethod("entity");
                        LivingEntity entity = (LivingEntity) entityMethod.invoke(slotContext);
                        
                        if (entity.level().isClientSide) {
                            return null;
                        }
                        
                        if (entity instanceof ServerPlayer player) {
                            // The flight handler already manages flight, but we can add additional logic here if needed
                            // For now, we rely on the existing FanpackFlightHandler
                        }
                        
                        return null;
                    } else if ("onEquip".equals(methodName)) {
                        // Handle onEquip
                        Object slotContext = args[0];
                        ItemStack prevStack = (ItemStack) args[1];
                        ItemStack stack = (ItemStack) args[2];
                        
                        Method entityMethod = slotContextClass.getMethod("entity");
                        LivingEntity entity = (LivingEntity) entityMethod.invoke(entityMethod);
                        
                        if (entity.level().isClientSide) {
                            return null;
                        }
                        
                        if (entity instanceof ServerPlayer player) {
                            // Flight is handled by FanpackFlightHandler, but we can add logic here if needed
                        }
                        
                        return null;
                    } else if ("onUnequip".equals(methodName)) {
                        // Handle onUnequip
                        Object slotContext = args[0];
                        ItemStack newStack = (ItemStack) args[1];
                        ItemStack stack = (ItemStack) args[2];
                        
                        Method entityMethod = slotContextClass.getMethod("entity");
                        LivingEntity entity = (LivingEntity) entityMethod.invoke(slotContext);
                        
                        if (entity.level().isClientSide) {
                            return null;
                        }
                        
                        if (entity instanceof ServerPlayer player) {
                            // Check if player still has a Fanpack equipped
                            if (!hasFanpackEquipped(player)) {
                                // Flight will be disabled by FanpackFlightHandler when no Fanpack is found
                            }
                        }
                        
                        return null;
                    } else if ("canEquipFromUse".equals(methodName)) {
                        return true;
                    }
                    
                    // Default return for other methods
                    return null;
                }
            );
        } catch (Exception e) {
            LOGGER.error("Failed to create Curio instance for Fanpack", e);
            return null;
        }
    }
    
    /**
     * Checks if player has a Fanpack equipped in any Curios slot
     */
    private static boolean hasFanpackEquipped(ServerPlayer player) {
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosInventoryMethod = curiosApiClass.getMethod("getCuriosInventory", LivingEntity.class);
            Object curiosInventoryOpt = getCuriosInventoryMethod.invoke(null, player);
            
            if (curiosInventoryOpt instanceof java.util.Optional<?> opt && opt.isPresent()) {
                Object curiosInventory = opt.get();
                Method getCuriosMethod = curiosInventory.getClass().getMethod("getCurios");
                Object curiosMap = getCuriosMethod.invoke(curiosInventory);
                
                if (curiosMap instanceof java.util.Map<?, ?> map) {
                    for (Object slotInventory : map.values()) {
                        Method getStacksMethod = slotInventory.getClass().getMethod("getStacks");
                        Object stacksHandler = getStacksMethod.invoke(slotInventory);
                        
                        Method getSlotsMethod = stacksHandler.getClass().getMethod("getSlots");
                        int slots = (Integer) getSlotsMethod.invoke(stacksHandler);
                        
                        for (int i = 0; i < slots; i++) {
                            Method getStackInSlotMethod = stacksHandler.getClass().getMethod("getStackInSlot", int.class);
                            ItemStack stack = (ItemStack) getStackInSlotMethod.invoke(stacksHandler, i);
                            
                            if (stack.getItem() instanceof FanpackItem) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Curios not available or error accessing
        }
        return false;
    }
}
