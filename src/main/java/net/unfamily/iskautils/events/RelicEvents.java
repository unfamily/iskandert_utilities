package net.unfamily.iskautils.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Small first-pass implementation of relic effects from reliquie.txt.
 * More complex cursed behaviors can be layered later without breaking saves.
 */
@EventBusSubscriber
public class RelicEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelicEvents.class);

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Sharpened Bone: +1 damage, plus a 25% chance to deal extra damage (approximation of "ignore armor").
        if (playerHasItem(player, ModItems.SHARPENED_BONE.get())) {
            float dmg = event.getAmount() + 1.0f;
            if (player.getRandom().nextFloat() < 0.25f) {
                dmg += 2.0f;
            }
            event.setAmount(dmg);
        }
    }

    @SubscribeEvent
    public static void onPlayerBreakSpeed(PlayerEvent.BreakSpeed event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (!playerHasItem(player, ModItems.THE_ROOTS.get())) return;

        // The Roots: random mining speed boost. Keep it bounded.
        float original = event.getOriginalSpeed();
        float mult = 1.0f + (player.getRandom().nextFloat() * 1.0f); // 1.0 .. 2.0
        event.setNewSpeed(original * mult);
    }

    private static boolean playerHasItem(Player player, net.minecraft.world.item.Item item) {
        ItemStack needle = new ItemStack(item);
        if (player.getInventory().contains(needle)) return true;
        if (ModUtils.isCuriosLoaded()) return hasInCurios(player, item);
        return false;
    }

    private static boolean hasInCurios(LivingEntity player, net.minecraft.world.item.Item item) {
        try {
            Class<?> curioApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosHelperMethod = curioApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHelperMethod.invoke(null);
            Method getEquippedCurios = curiosHelper.getClass().getMethod("getEquippedCurios", LivingEntity.class);
            Object equippedCurios = getEquippedCurios.invoke(curiosHelper, player);

            if (equippedCurios instanceof Iterable<?> items) {
                for (Object itemPair : items) {
                    Method getRight = itemPair.getClass().getMethod("getRight");
                    ItemStack stack = (ItemStack) getRight.invoke(itemPair);
                    if (stack != null && stack.getItem() == item) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable t) {
            LOGGER.debug("Curios check for relic failed: {}", t.toString());
            return false;
        }
    }
}

