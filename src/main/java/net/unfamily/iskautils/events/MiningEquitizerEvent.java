package net.unfamily.iskautils.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@EventBusSubscriber
public class MiningEquitizerEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiningEquitizerEvent.class);

    @SubscribeEvent
    public static void onPlayerBreakSpeed(PlayerEvent.BreakSpeed event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        boolean inAir = !entity.onGround();
        boolean inWater = entity.isInWater();
        if (!inAir && !inWater) {
            return;
        }

        if (!playerHasMiningEquitizer(player)) {
            return;
        }

        float originalSpeed = event.getOriginalSpeed();
        float multiplier;
        if (inAir && inWater) {
            multiplier = (float) Config.miningEquitizerAirAndWaterMultiplier;
        } else if (inWater) {
            multiplier = (float) Config.miningEquitizerWaterMultiplier;
        } else {
            multiplier = (float) Config.miningEquitizerAirMultiplier;
        }
        event.setNewSpeed(originalSpeed * multiplier);
    }

    private static boolean playerHasMiningEquitizer(Player player) {
        ItemStack needle = new ItemStack(ModItems.MINING_EQUITIZER.get());
        if (player.getInventory().contains(needle)) {
            return true;
        }
        if (ModUtils.isCuriosLoaded()) {
            return hasEquitizerInCurios(player);
        }
        return false;
    }

    private static boolean hasEquitizerInCurios(LivingEntity player) {
        try {
            Class<?> curioApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosHandlerMethod = curioApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHandlerMethod.invoke(null);
            Method getEquippedCurios = curiosHelper.getClass().getMethod("getEquippedCurios", LivingEntity.class);
            Object equippedCurios = getEquippedCurios.invoke(curiosHelper, player);

            if (equippedCurios instanceof Iterable<?> items) {
                for (Object itemPair : items) {
                    Method getStackMethod = itemPair.getClass().getMethod("getRight");
                    ItemStack stack = (ItemStack) getStackMethod.invoke(itemPair);
                    if (stack.is(ModItems.MINING_EQUITIZER.get())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.debug("Curios check for Mining Equitizer failed: {}", e.getMessage());
            return false;
        }
    }
}
