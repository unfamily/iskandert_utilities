package net.unfamily.iskautils.events;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.custom.artifact.ChosenCheeseItem;
import net.unfamily.iskautils.item.custom.artifact.IceDiamondItem;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.util.ArtifactEquipStages;
import net.unfamily.iskautils.util.ArtifactTickIntervals;
import net.unfamily.iskautils.util.AttributeSyncGrace;
import net.unfamily.iskautils.util.CurioEquipUtil;

/**
 * Tick-based artifact effects driven by internal equip stages.
 */
@EventBusSubscriber
public final class ArtifactTickEffects {
    private static final Identifier OLD_BRICK_ARMOR_ID = Identifier.fromNamespaceAndPath("iska_utils", "old_brick_armor");
    private static final Identifier CHOSEN_CHEESE_HP_ID = Identifier.fromNamespaceAndPath("iska_utils", "chosen_cheese_hp");

    private ArtifactTickEffects() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }

        long gameTime = sp.level().getGameTime();
        if (ArtifactTickIntervals.isDue(gameTime, ArtifactTickIntervals.FAST_TICKS)) {
            applyOldBrick(sp);
            applyChosenCheese(sp);
        }
        applyIceDiamond(sp);
    }

    private static void applyOldBrick(ServerPlayer player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        AttributeModifier existing = armor.getModifier(OLD_BRICK_ARMOR_ID);
        if (StageRegistry.playerHasStage(player, ArtifactEquipStages.OLD_BRICK)) {
            if (existing == null) {
                armor.addTransientModifier(new AttributeModifier(
                        OLD_BRICK_ARMOR_ID, Config.oldBrickArmorBonus, AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (existing != null) {
            armor.removeModifier(OLD_BRICK_ARMOR_ID);
        }
    }

    private static void applyChosenCheese(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        if (!StageRegistry.playerHasStage(player, ArtifactEquipStages.CHOSEN_CHEESE)) {
            if (!AttributeSyncGrace.shouldDeferRemoval(player)
                    && maxHealth.getModifier(CHOSEN_CHEESE_HP_ID) != null) {
                maxHealth.removeModifier(CHOSEN_CHEESE_HP_ID);
                clampHealth(player);
            }
            return;
        }

        ItemStack cheese = findEquippedStack(player, ChosenCheeseItem.class);
        if (cheese != null) {
            ChosenCheeseItem.syncDisplayModel(cheese);
        }
        int effective = cheese != null
                ? Math.min(ChosenCheeseItem.getLevel(cheese), Config.chosenCheeseMax)
                : 0;
        double bonus = effective * Config.chosenCheeseHpPerLevel;
        AttributeModifier existing = maxHealth.getModifier(CHOSEN_CHEESE_HP_ID);
        if (existing != null && existing.amount() == bonus) {
            return;
        }
        if (existing != null) {
            maxHealth.removeModifier(CHOSEN_CHEESE_HP_ID);
        }
        if (bonus > 0) {
            maxHealth.addTransientModifier(new AttributeModifier(
                    CHOSEN_CHEESE_HP_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
            clampHealth(player);
        }
    }

    private static void applyIceDiamond(ServerPlayer player) {
        if ((player.tickCount % Config.iceDiamondRepairIntervalTicks) != 0) {
            return;
        }
        ItemStack iceDiamond = findIceDiamondStack(player);
        if (iceDiamond == null) {
            return;
        }

        ItemStack target = findRepairTarget(player);
        if (target == null || !target.isDamaged()) {
            return;
        }
        target.setDamageValue(Math.max(0, target.getDamageValue() - 1));

        int cost = computeIceDiamondCost(player.level(), player);
        if (cost > 0) {
            iceDiamond.setDamageValue(Math.min(iceDiamond.getMaxDamage(), iceDiamond.getDamageValue() + cost));
        }
    }

    /** Ice Diamond is active in inventory, hands, or Curios (not Curios-only). */
    private static ItemStack findIceDiamondStack(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (isUsableIceDiamond(main)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (isUsableIceDiamond(off)) {
            return off;
        }
        ItemStack[] found = new ItemStack[1];
        CurioEquipUtil.forEachEquippedCurioStack(player, stack -> {
            if (found[0] == null && isUsableIceDiamond(stack)) {
                found[0] = stack;
            }
        });
        if (found[0] != null) {
            return found[0];
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isUsableIceDiamond(stack)) {
                return stack;
            }
        }
        return null;
    }

    private static boolean isUsableIceDiamond(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof IceDiamondItem
                && stack.getDamageValue() < stack.getMaxDamage();
    }

    private static ItemStack findEquippedStack(ServerPlayer player, Class<?> itemClass) {
        ItemStack[] found = new ItemStack[1];
        CurioEquipUtil.forEachEquippedCurioStack(player, stack -> {
            if (found[0] == null && itemClass.isInstance(stack.getItem())) {
                found[0] = stack;
            }
        });
        return found[0];
    }

    private static ItemStack findRepairTarget(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.isDamaged()) return main;
        ItemStack off = player.getOffhandItem();
        if (off.isDamaged()) return off;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isDamaged()) {
                return armor;
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (inv.isDamaged()) return inv;
        }
        return null;
    }

    private static int computeIceDiamondCost(Level level, Player player) {
        Biome biome = level.getBiome(player.blockPosition()).value();
        float t = biome.getBaseTemperature();
        if (t <= Config.iceDiamondColdBiomeMaxTemp) return Config.iceDiamondColdRepairCost;
        if (t >= Config.iceDiamondHotBiomeMinTemp) return Config.iceDiamondHotRepairCost;
        return Config.iceDiamondTemperateRepairCost;
    }

    private static void clampHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
