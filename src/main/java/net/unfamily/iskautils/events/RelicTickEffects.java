package net.unfamily.iskautils.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.custom.relic.ChosenCheeseItem;
import net.unfamily.iskautils.item.custom.relic.IceDiamondItem;
import net.unfamily.iskautils.stage.StageRegistry;
import net.unfamily.iskautils.util.CurioEquipUtil;
import net.unfamily.iskautils.util.RelicEquipStages;

/**
 * Tick-based relic effects driven by internal equip stages.
 */
@EventBusSubscriber
public final class RelicTickEffects {
    private static final ResourceLocation OLD_BRICK_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "old_brick_armor");
    private static final ResourceLocation CHOSEN_CHEESE_HP_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "chosen_cheese_hp");

    private RelicTickEffects() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }

        applyOldBrick(sp);
        applyChosenCheese(sp);
        applyIceDiamond(sp);
    }

    private static void applyOldBrick(ServerPlayer player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        AttributeModifier existing = armor.getModifier(OLD_BRICK_ARMOR_ID);
        if (StageRegistry.playerHasStage(player, RelicEquipStages.OLD_BRICK)) {
            if (existing == null) {
                armor.addTransientModifier(new AttributeModifier(
                        OLD_BRICK_ARMOR_ID, 2.0, AttributeModifier.Operation.ADD_VALUE));
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
        if (!StageRegistry.playerHasStage(player, RelicEquipStages.CHOSEN_CHEESE)) {
            if (maxHealth.getModifier(CHOSEN_CHEESE_HP_ID) != null) {
                maxHealth.removeModifier(CHOSEN_CHEESE_HP_ID);
                clampHealth(player);
            }
            return;
        }

        ItemStack cheese = findEquippedStack(player, ChosenCheeseItem.class);
        int effective = cheese != null
                ? Math.min(ChosenCheeseItem.getLevel(cheese), Config.chosenCheeseMax)
                : 0;
        double bonus = effective * 2.0;
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
        if (!StageRegistry.playerHasStage(player, RelicEquipStages.ICE_DIAMOND)) {
            return;
        }
        if ((player.tickCount % 20) != 0) {
            return;
        }
        ItemStack iceDiamond = findEquippedStack(player, IceDiamondItem.class);
        if (iceDiamond == null || iceDiamond.getDamageValue() >= iceDiamond.getMaxDamage()) {
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
        for (ItemStack armor : player.getInventory().armor) {
            if (armor.isDamaged()) return armor;
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
        if (t <= 0.15f) return 0;
        if (t >= 1.5f) return 5;
        return 1;
    }

    private static void clampHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
