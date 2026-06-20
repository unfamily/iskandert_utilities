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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.artifact.ChosenCheeseItem;
import net.unfamily.iskautils.item.custom.artifact.IceDiamondItem;
import net.unfamily.iskautils.item.custom.artifact.RunicDiceItem;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.util.ArtifactEquipStages;
import net.unfamily.iskautils.util.ArtifactTickIntervals;
import net.unfamily.iskautils.util.AttributeSyncGrace;
import net.unfamily.iskautils.util.RelicEffectGate;
import net.unfamily.iskautils.util.CurioEquipUtil;

/**
 * Tick-based artifact effects driven by internal equip stages.
 */
@EventBusSubscriber
public final class ArtifactTickEffects {
    private static final ResourceLocation OLD_BRICK_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "old_brick_armor");
    private static final ResourceLocation CHOSEN_CHEESE_HP_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "chosen_cheese_hp");
    private static final ResourceLocation ANCIENT_STAR_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "ancient_star_armor");
    private static final ResourceLocation ANCIENT_STAR_TOUGHNESS_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "ancient_star_toughness");
    private static final ResourceLocation RUNIC_DICE_ATTACK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "runic_dice_attack_speed");

    private ArtifactTickEffects() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!RelicEffectGate.shouldApply(sp)) {
            return;
        }

        long gameTime = sp.level().getGameTime();
        if (ArtifactTickIntervals.isDue(gameTime, ArtifactTickIntervals.FAST_TICKS)) {
            applyOldBrick(sp);
            applyChosenCheese(sp);
            applyAncientStarAttributes(sp);
            applyRunicDice(sp, gameTime);
        }
        applyIceDiamond(sp);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAncientStarDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (player instanceof ServerPlayer sp && !RelicEffectGate.shouldApply(sp)) {
            return;
        }
        if (Config.ancientStarDamageBonus <= 0.0D || Config.ancientStarHighHpRatio <= 0.0D) {
            return;
        }
        if (!StageRegistry.playerHasStage(player, ArtifactEquipStages.ANCIENT_STAR)) {
            return;
        }
        float maxHp = player.getMaxHealth();
        if (maxHp <= 0.0F) {
            return;
        }
        if (player.getHealth() / maxHp < Config.ancientStarHighHpRatio) {
            return;
        }
        event.setAmount(event.getAmount() + (float) Config.ancientStarDamageBonus);
    }

    private static void applyRunicDice(ServerPlayer player, long gameTime) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }

        if (!StageRegistry.playerHasStage(player, ArtifactEquipStages.RUNIC_DICE)) {
            removeTransientModifier(attackSpeed, RUNIC_DICE_ATTACK_SPEED_ID, player);
            return;
        }

        ItemStack dice = CurioEquipUtil.findActiveStack(player, ModItems.RUNIC_DICE.get());
        if (dice.isEmpty()) {
            removeTransientModifier(attackSpeed, RUNIC_DICE_ATTACK_SPEED_ID, player);
            return;
        }

        int rerollTicks = Math.max(1, Config.runicDiceRerollTicks);
        if (gameTime % rerollTicks == 0) {
            RunicDiceItem.reroll(dice, player.getRandom());
        } else {
            RunicDiceItem.ensureRoll(dice, player.getRandom());
        }

        double bonus = RunicDiceItem.getStoredBonus(dice);
        if (bonus <= 0.0D) {
            removeTransientModifier(attackSpeed, RUNIC_DICE_ATTACK_SPEED_ID, player);
            return;
        }

        AttributeModifier existing = attackSpeed.getModifier(RUNIC_DICE_ATTACK_SPEED_ID);
        if (existing != null
                && existing.amount() == bonus
                && existing.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
            AttributeSyncGrace.shouldRemoveEquippedBonus(player, RUNIC_DICE_ATTACK_SPEED_ID, true);
            return;
        }
        if (existing != null) {
            attackSpeed.removeModifier(RUNIC_DICE_ATTACK_SPEED_ID);
        }
        attackSpeed.addTransientModifier(new AttributeModifier(
                RUNIC_DICE_ATTACK_SPEED_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        AttributeSyncGrace.shouldRemoveEquippedBonus(player, RUNIC_DICE_ATTACK_SPEED_ID, true);
    }

    private static void removeTransientModifier(
            AttributeInstance attribute, ResourceLocation id, ServerPlayer player) {
        if (attribute.getModifier(id) != null
                && AttributeSyncGrace.shouldRemoveEquippedBonus(player, id, false)) {
            attribute.removeModifier(id);
        }
    }

    private static void applyAncientStarAttributes(ServerPlayer player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor == null || toughness == null) {
            return;
        }
        if (!StageRegistry.playerHasStage(player, ArtifactEquipStages.ANCIENT_STAR)) {
            if (!AttributeSyncGrace.shouldDeferRemoval(player)) {
                if (armor.getModifier(ANCIENT_STAR_ARMOR_ID) != null) {
                    armor.removeModifier(ANCIENT_STAR_ARMOR_ID);
                }
                if (toughness.getModifier(ANCIENT_STAR_TOUGHNESS_ID) != null) {
                    toughness.removeModifier(ANCIENT_STAR_TOUGHNESS_ID);
                }
            }
            return;
        }

        float ratio = player.getMaxHealth() > 0.0F ? player.getHealth() / player.getMaxHealth() : 0.0F;
        applyModifier(armor, ANCIENT_STAR_ARMOR_ID, Config.ancientStarArmorBonus);
        if (ratio < Config.ancientStarLowHpRatio && Config.ancientStarToughnessBonus > 0.0D) {
            applyModifier(toughness, ANCIENT_STAR_TOUGHNESS_ID, Config.ancientStarToughnessBonus);
        } else if (toughness.getModifier(ANCIENT_STAR_TOUGHNESS_ID) != null) {
            toughness.removeModifier(ANCIENT_STAR_TOUGHNESS_ID);
        }
    }

    private static void applyModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        AttributeModifier existing = attribute.getModifier(id);
        if (amount <= 0.0D) {
            if (existing != null) {
                attribute.removeModifier(id);
            }
            return;
        }
        if (existing != null && existing.amount() == amount) {
            return;
        }
        if (existing != null) {
            attribute.removeModifier(id);
        }
        attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
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
