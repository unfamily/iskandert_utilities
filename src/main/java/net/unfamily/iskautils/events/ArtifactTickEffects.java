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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
import net.unfamily.iskautils.util.ArtifactEffectGate;
import net.unfamily.iskautils.util.CurioEquipUtil;

/**
 * Tick-based artifact effects driven by internal equip stages.
 */
@EventBusSubscriber
public final class ArtifactTickEffects {
    private static final Identifier OLD_BRICK_ARMOR_ID = Identifier.fromNamespaceAndPath("iska_utils", "old_brick_armor");
    private static final Identifier CHOSEN_CHEESE_HP_ID = Identifier.fromNamespaceAndPath("iska_utils", "chosen_cheese_hp");
    private static final Identifier ANCIENT_STAR_ARMOR_ID = Identifier.fromNamespaceAndPath("iska_utils", "ancient_star_armor");
    private static final Identifier ANCIENT_STAR_TOUGHNESS_ID = Identifier.fromNamespaceAndPath("iska_utils", "ancient_star_toughness");
    private static final Identifier RUNIC_DICE_ATTACK_SPEED_ID = Identifier.fromNamespaceAndPath("iska_utils", "runic_dice_attack_speed");
    private static final Identifier CALLING_BELL_HP_ID = Identifier.fromNamespaceAndPath("iska_utils", "calling_bell_hp");
    private static final Identifier CALLING_BELL_ARMOR_ID = Identifier.fromNamespaceAndPath("iska_utils", "calling_bell_armor");
    private static final Identifier CALLING_BELL_TOUGHNESS_ID = Identifier.fromNamespaceAndPath("iska_utils", "calling_bell_toughness");

    private ArtifactTickEffects() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!ArtifactEffectGate.shouldApply(sp)) {
            return;
        }

        long gameTime = sp.level().getGameTime();
        applyChosenCheese(sp);
        if (AttributeSyncGrace.shouldRunAttributePass(sp, gameTime, ArtifactTickIntervals.FAST_TICKS)) {
            applyOldBrick(sp);
            applyAncientStarAttributes(sp);
            applyRunicDice(sp, gameTime);
            applyCallingBell(sp);
        }
        applyIceDiamond(sp);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAncientStarDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (player instanceof ServerPlayer sp && !ArtifactEffectGate.shouldApply(sp)) {
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
        float ratio = player.getHealth() / maxHp;
        if (ratio < Config.ancientStarHighHpRatio) {
            return;
        }
        event.setAmount(event.getAmount() + (float) Config.ancientStarDamageBonus);
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
            AttributeSyncGrace.shouldRemoveEquippedBonus(player, OLD_BRICK_ARMOR_ID, true);
        } else if (existing != null && AttributeSyncGrace.shouldRemoveEquippedBonus(player, OLD_BRICK_ARMOR_ID, false)) {
            armor.removeModifier(OLD_BRICK_ARMOR_ID);
        }
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
        if (existing != null && existing.amount() == bonus && existing.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
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

    private static void applyAncientStarAttributes(ServerPlayer player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor == null || toughness == null) {
            return;
        }

        boolean equipped = StageRegistry.playerHasStage(player, ArtifactEquipStages.ANCIENT_STAR);
        if (!equipped) {
            removeTransientModifier(armor, ANCIENT_STAR_ARMOR_ID, player);
            removeTransientModifier(toughness, ANCIENT_STAR_TOUGHNESS_ID, player);
            return;
        }

        float maxHp = player.getMaxHealth();
        float ratio = maxHp > 0.0F ? player.getHealth() / maxHp : 0.0F;
        boolean lowHp = ratio < Config.ancientStarLowHpRatio;

        applyTransientModifier(armor, ANCIENT_STAR_ARMOR_ID, Config.ancientStarArmorBonus, player, true);
        if (lowHp && Config.ancientStarToughnessBonus > 0.0D) {
            applyTransientModifier(toughness, ANCIENT_STAR_TOUGHNESS_ID, Config.ancientStarToughnessBonus, player, true);
        } else {
            removeTransientModifier(toughness, ANCIENT_STAR_TOUGHNESS_ID, player);
        }
    }

    private static void applyCallingBell(ServerPlayer player) {
        String stage = ArtifactEquipStages.stageForArcaneArtifact(ModItems.CALLING_BELL.get());
        boolean equipped = stage != null && StageRegistry.playerHasStage(player, stage);
        int arcane = CurioEquipUtil.countEquippedArcaneArtifacts(player, ModItems.CALLING_BELL.get());
        boolean active = equipped && arcane >= Config.callingBellArcaneArtifactThreshold;

        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (maxHealth == null || armor == null || toughness == null) {
            return;
        }

        if (!active) {
            removeCallingBellModifiersImmediately(maxHealth, armor, toughness, player);
            return;
        }

        applyTransientModifier(maxHealth, CALLING_BELL_HP_ID, Config.callingBellHpBonus, player, true);
        applyTransientModifier(armor, CALLING_BELL_ARMOR_ID, Config.callingBellArmorBonus, player, true);
        applyTransientModifier(toughness, CALLING_BELL_TOUGHNESS_ID, Config.callingBellToughnessBonus, player, true);
        clampHealth(player);
    }

    private static void removeCallingBellModifiersImmediately(
            AttributeInstance maxHealth,
            AttributeInstance armor,
            AttributeInstance toughness,
            ServerPlayer player) {
        boolean removedHp = false;
        if (maxHealth.getModifier(CALLING_BELL_HP_ID) != null) {
            maxHealth.removeModifier(CALLING_BELL_HP_ID);
            AttributeSyncGrace.shouldRemoveEquippedBonus(player, CALLING_BELL_HP_ID, true);
            removedHp = true;
        }
        if (armor.getModifier(CALLING_BELL_ARMOR_ID) != null) {
            armor.removeModifier(CALLING_BELL_ARMOR_ID);
            AttributeSyncGrace.shouldRemoveEquippedBonus(player, CALLING_BELL_ARMOR_ID, true);
        }
        if (toughness.getModifier(CALLING_BELL_TOUGHNESS_ID) != null) {
            toughness.removeModifier(CALLING_BELL_TOUGHNESS_ID);
            AttributeSyncGrace.shouldRemoveEquippedBonus(player, CALLING_BELL_TOUGHNESS_ID, true);
        }
        if (removedHp) {
            clampHealth(player);
        }
    }

    private static void applyTransientModifier(
            AttributeInstance attribute,
            Identifier id,
            double amount,
            ServerPlayer player,
            boolean equipped) {
        AttributeModifier existing = attribute.getModifier(id);
        if (amount <= 0.0D) {
            removeTransientModifier(attribute, id, player);
            return;
        }
        if (existing != null && existing.amount() == amount) {
            if (equipped) {
                AttributeSyncGrace.shouldRemoveEquippedBonus(player, id, true);
            }
            return;
        }
        if (existing != null) {
            attribute.removeModifier(id);
        }
        attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        if (equipped) {
            AttributeSyncGrace.shouldRemoveEquippedBonus(player, id, true);
        }
    }

    private static void removeTransientModifier(AttributeInstance attribute, Identifier id, ServerPlayer player) {
        if (attribute.getModifier(id) != null
                && AttributeSyncGrace.shouldRemoveEquippedBonus(player, id, false)) {
            attribute.removeModifier(id);
        }
    }

    private static void applyChosenCheese(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        ItemStack cheese = findEquippedStack(player, ChosenCheeseItem.class);
        boolean active = StageRegistry.playerHasStage(player, ArtifactEquipStages.CHOSEN_CHEESE)
                || !cheese.isEmpty();

        if (!active) {
            if (AttributeSyncGrace.shouldRemoveEquippedBonus(player, CHOSEN_CHEESE_HP_ID, false)
                    && maxHealth.getModifier(CHOSEN_CHEESE_HP_ID) != null) {
                maxHealth.removeModifier(CHOSEN_CHEESE_HP_ID);
                clampHealth(player);
            }
            return;
        }

        if (!cheese.isEmpty()) {
            ChosenCheeseItem.syncDisplayModel(cheese);
        }
        AttributeSyncGrace.shouldRemoveEquippedBonus(player, CHOSEN_CHEESE_HP_ID, true);
        int effective = !cheese.isEmpty()
                ? Math.min(ChosenCheeseItem.getLevel(cheese), Config.chosenCheeseMax)
                : 0;
        double bonus = effective * Config.chosenCheeseHpPerLevel;
        AttributeModifier existing = maxHealth.getModifier(CHOSEN_CHEESE_HP_ID);
        if (bonus <= 0.0D) {
            if (existing != null) {
                return;
            }
            return;
        }
        if (existing != null && existing.amount() == bonus) {
            return;
        }
        if (existing != null) {
            maxHealth.removeModifier(CHOSEN_CHEESE_HP_ID);
        }
        maxHealth.addTransientModifier(new AttributeModifier(
                CHOSEN_CHEESE_HP_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        clampHealth(player);
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
        return found[0] != null ? found[0] : ItemStack.EMPTY;
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
