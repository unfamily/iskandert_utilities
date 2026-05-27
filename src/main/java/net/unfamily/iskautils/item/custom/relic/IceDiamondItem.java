package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.unfamily.iskautils.util.CurioEquipUtil;

/**
 * Ice Diamond relic.
 * Repairs items over time while consuming its own durability.
 */
public class IceDiamondItem extends Item {
    public IceDiamondItem(Properties properties) {
        super(properties.stacksTo(1).durability(1024));
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) return;
        if (!CurioEquipUtil.hasEquipped(player, this)) return;

        if ((player.tickCount % 20) != 0) return;
        if (stack.getDamageValue() >= stack.getMaxDamage()) return;

        ItemStack target = findRepairTarget(player);
        if (target == null || !target.isDamaged()) return;

        target.setDamageValue(Math.max(0, target.getDamageValue() - 1));

        int cost = computeSelfCost(level, player);
        if (cost > 0 && player instanceof ServerPlayer sp) {
            stack.hurtAndBreak(cost, sp, EquipmentSlot.MAINHAND);
        } else if (cost > 0) {
            stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + cost));
        }
    }

    private static ItemStack findRepairTarget(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.isDamaged()) return main;
        ItemStack off = player.getOffhandItem();
        if (off.isDamaged()) return off;

        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor != null && armor.isDamaged()) return armor;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (inv.isDamaged()) return inv;
        }
        return null;
    }

    private static int computeSelfCost(Level level, Player player) {
        Biome biome = level.getBiome(player.blockPosition()).value();
        float t = biome.getBaseTemperature();
        if (t <= 0.15f) return 0;
        if (t >= 1.5f) return 5;
        return 1;
    }
}

