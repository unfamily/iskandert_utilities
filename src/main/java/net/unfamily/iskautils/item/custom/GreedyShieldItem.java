package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.Config;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Greedy Shield Item - When taking damage, has a chance to completely block it,
 * or if that fails, has a chance to reduce it significantly.
 * Internal equip stage is granted only while the stack is outside vanilla inventory (e.g. Curios).
 * Holding in main/off hand does not set that stage; damage logic checks hands separately.
 */
public class GreedyShieldItem extends Item {

    public static final String EQUIP_STAGE = "iska_utils_internal-greedy_shield_equip";

    public GreedyShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        
        // Get values from config and convert to percentages
        int blockChancePercent = (int) Math.round(Config.greedyShieldBlockChance * 100);
        int reduceChancePercent = (int) Math.round(Config.greedyShieldReduceChance * 100);
        int reduceAmountPercent = (int) Math.round((1.0 - Config.greedyShieldReduceAmount) * 100); // Percentage blocked
        int remainingPercent = (int) Math.round(Config.greedyShieldReduceAmount * 100); // Percentage remaining
        
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.desc0"));
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.desc1", blockChancePercent)
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.desc2", reduceChancePercent)
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.desc3", reduceAmountPercent, remainingPercent)
                .withStyle(ChatFormatting.GRAY));
        
        if (Config.greedyShieldInfo) {
            tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.info"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        // Match legacy iskandert_utilities: tick on both sides; StageRegistry mutates only on logical server
        if (!(entity instanceof Player player)) {
            return;
        }
        if (ModUtils.isStackInVanillaPlayerInventory(player, stack)) {
            StageRegistry.removePlayerStage(player, EQUIP_STAGE, true);
        } else {
            StageRegistry.addPlayerStage(player, EQUIP_STAGE, true);
        }
    }

    public static boolean isGreedyShieldInHands(Player player) {
        return player.getMainHandItem().getItem() instanceof GreedyShieldItem
            || player.getOffhandItem().getItem() instanceof GreedyShieldItem;
    }

    /**
     * True when the player holds a greedy shield in main/off hand or has one in a Curios slot.
     */
    public static boolean isGreedyShieldActive(Player player) {
        if (isGreedyShieldInHands(player)) {
            return true;
        }
        if (ModUtils.isCuriosLoaded()) {
            return hasGreedyInCurios(player);
        }
        return false;
    }

    private static boolean hasGreedyInCurios(Player player) {
        try {
            Class<?> curioApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosHelperMethod = curioApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHelperMethod.invoke(null);
            Method getEquippedCurios = curiosHelper.getClass().getMethod("getEquippedCurios", LivingEntity.class);
            Object equippedCurios = getEquippedCurios.invoke(curiosHelper, player);
            if (equippedCurios instanceof Iterable<?> items) {
                for (Object itemPair : items) {
                    Method getStackMethod = itemPair.getClass().getMethod("getRight");
                    ItemStack s = (ItemStack) getStackMethod.invoke(itemPair);
                    if (s.getItem() instanceof GreedyShieldItem) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack itemstack, Player entity) {
        StageRegistry.removePlayerStage(entity, EQUIP_STAGE, true);
        return true;
    }
}
