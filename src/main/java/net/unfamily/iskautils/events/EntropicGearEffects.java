package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.stage.StageRegistry;
import net.unfamily.iskautils.util.DamageArmorBypassUtil;
import net.unfamily.iskautils.util.EntropicGearUtil;
import net.unfamily.iskautils.util.RelicEquipStages;

@EventBusSubscriber
public final class EntropicGearEffects {
    private static final ResourceLocation HELMET_HP_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "entropic_helmet_hp");
    private static final ResourceLocation CHEST_TOUGHNESS_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "entropic_chest_toughness");
    private static final ResourceLocation LEGGINGS_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "entropic_leggings_armor");

    private EntropicGearEffects() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) {
            return;
        }
        applyHelmet(sp);
        applyChestplate(sp);
        applyLeggings(sp);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!Config.entropicBootsNegateFallDamage) {
            return;
        }
        if (EntropicGearUtil.isWearing(player, ModItems.ENTROPIC_BOOTS.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        if (!EntropicGearUtil.isEntropicPickaxeTool(event.getTool())) {
            return;
        }
        if (Config.entropicPickaxeBonusFortuneChance <= 0.0D) {
            return;
        }
        if (player.getRandom().nextDouble() >= Config.entropicPickaxeBonusFortuneChance) {
            return;
        }

        ServerLevel level = event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = event.getBlockEntity();
        Entity breaker = event.getBreaker();
        ItemStack probe = copyWithBonusFortune(event.getTool(), level);

        for (ItemStack drop : Block.getDrops(state, level, pos, blockEntity, breaker, probe)) {
            if (drop.isEmpty()) {
                continue;
            }
            event.getDrops().add(new ItemEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    drop));
        }
    }

    private static ItemStack copyWithBonusFortune(ItemStack tool, ServerLevel level) {
        ItemStack probe = tool.copy();
        int extra = Config.entropicPickaxeBonusFortuneLevels;
        if (extra <= 0) {
            return probe;
        }
        var fortune = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE);
        int current = EnchantmentHelper.getItemEnchantmentLevel(fortune, probe);
        probe.enchant(fortune, current + extra);
        return probe;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSharpenedBoneDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (Config.sharpenedBoneArmorIgnoreChance <= 0.0D) {
            return;
        }
        if (!StageRegistry.playerHasStage(player, RelicEquipStages.SHARPENED_BONE)) {
            return;
        }
        if (player.getRandom().nextDouble() >= Config.sharpenedBoneArmorIgnoreChance) {
            return;
        }
        float extra = DamageArmorBypassUtil.computeExtraDamage(
                target, event.getAmount(), event.getSource(), 1.0F, 0.0F);
        if (extra > 0.0F) {
            event.setAmount(event.getAmount() + extra);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onEntropicArmorPen(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!EntropicGearUtil.isEntropicArmorPenWeapon(weapon)) {
            return;
        }
        if (Config.entropicArmorPenChance <= 0.0D || Config.entropicArmorPenIgnoreFraction <= 0.0D) {
            return;
        }
        if (player.getRandom().nextDouble() >= Config.entropicArmorPenChance) {
            return;
        }
        float extra = DamageArmorBypassUtil.computeExtraDamage(
                target,
                event.getAmount(),
                event.getSource(),
                (float) Config.entropicArmorPenIgnoreFraction,
                (float) Config.entropicArmorPenIgnoreFraction);
        if (extra > 0.0F) {
            event.setAmount(event.getAmount() + extra);
        }
    }

    private static void applyHelmet(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        if (!EntropicGearUtil.isWearing(player, ModItems.ENTROPIC_HELMET.get())) {
            removeModifier(maxHealth, HELMET_HP_ID);
            clampHealth(player);
            return;
        }

        double bonus = Config.entropicHelmetBaseHp
                + Config.entropicHelmetHpPerEntropicPiece
                        * EntropicGearUtil.countEntropicArmorPieces(player, EquipmentSlot.HEAD);
        applyModifier(maxHealth, HELMET_HP_ID, bonus);
        clampHealth(player);
    }

    private static void applyChestplate(ServerPlayer player) {
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness == null) {
            return;
        }
        if (!EntropicGearUtil.isWearing(player, ModItems.ENTROPIC_CHESTPLATE.get())) {
            removeModifier(toughness, CHEST_TOUGHNESS_ID);
            return;
        }
        if (Config.entropicChestplateMissingHpPerStep <= 0.0D) {
            removeModifier(toughness, CHEST_TOUGHNESS_ID);
            return;
        }

        int missingHp = EntropicGearUtil.missingHealthPoints(player);
        int steps = (int) (missingHp / Config.entropicChestplateMissingHpPerStep);
        double bonus = steps * Config.entropicChestplateToughnessBonusPerStep;
        applyModifier(toughness, CHEST_TOUGHNESS_ID, bonus);
    }

    private static void applyLeggings(ServerPlayer player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        if (!EntropicGearUtil.isWearing(player, ModItems.ENTROPIC_LEGGINGS.get())) {
            removeModifier(armor, LEGGINGS_ARMOR_ID);
            return;
        }
        if (Config.entropicLeggingsMissingHpPerStep <= 0.0D) {
            removeModifier(armor, LEGGINGS_ARMOR_ID);
            return;
        }

        int missingHp = EntropicGearUtil.missingHealthPoints(player);
        int steps = (int) (missingHp / Config.entropicLeggingsMissingHpPerStep);
        double bonus = steps * Config.entropicLeggingsArmorBonusPerStep;
        applyModifier(armor, LEGGINGS_ARMOR_ID, bonus);
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

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
    }

    private static void clampHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
