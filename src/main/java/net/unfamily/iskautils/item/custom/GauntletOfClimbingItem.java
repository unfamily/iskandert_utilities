package net.unfamily.iskautils.item.custom;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.Config;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Gauntlet of Climbing - Allows player to climb walls when held in inventory.
 * Toggle state is in-memory per player UUID (same model as {@link BurningBrazierItem} auto-placement).
 */
public class GauntletOfClimbingItem extends Item {
    private static final double DEFAULT_CLIMB_SPEED = 0.15D;

    private static final ConcurrentHashMap<UUID, Boolean> CLIMBING_ENABLED_BY_PLAYER = new ConcurrentHashMap<>();

    public GauntletOfClimbingItem(Properties properties) {
        super(properties);
    }

    public static boolean isClimbingEnabled(UUID playerId) {
        return CLIMBING_ENABLED_BY_PLAYER.getOrDefault(playerId, Boolean.TRUE);
    }

    public static boolean isClimbingEnabled(@Nullable Player player) {
        if (player == null) {
            return true;
        }
        return isClimbingEnabled(player.getUUID());
    }

    /**
     * Toggles climbing for the player; returns the new enabled state.
     */
    public static boolean toggleClimbing(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean next = !isClimbingEnabled(id);
        CLIMBING_ENABLED_BY_PLAYER.put(id, next);
        return next;
    }

    private static double getClimbSpeed() {
        double speed = Config.gauntletClimbingSpeed;
        return speed > 0.0 ? speed : DEFAULT_CLIMB_SPEED;
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (!(entity instanceof Player player)) {
            return;
        }

        if (!isClimbingEnabled(player.getUUID())) {
            return;
        }

        if (player.horizontalCollision) {
            makePlayerClimb(player, getClimbSpeed());
        }
    }

    private void makePlayerClimb(Player player, double climbSpeed) {
        Vec3 motion = player.getDeltaMovement();

        if (player.isShiftKeyDown()) {
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0f;
        } else {
            double newY = Math.max(motion.y, climbSpeed);
            player.setDeltaMovement(motion.x, newY, motion.z);
            player.fallDistance = 0.0f;
        }

        player.hurtMarked = true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);

        Component keybindName = Component.translatable("key.iska_utils.gauntlet_climbing_toggle");
        tooltip.accept(Component.translatable("tooltip.iska_utils.gauntlet_of_climbing.desc"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.gauntlet_of_climbing.toggle", keybindName));

        boolean on = true;
        tooltip.accept(
                Component.translatable("tooltip.iska_utils.gauntlet_of_climbing.status." + (on ? "enabled" : "disabled"))
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}
