package net.unfamily.iskautils.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.util.CurioEquipUtil;
import net.unfamily.iskautils.util.ModUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Fanpack - An extension of Vector Charm that also provides creative flight
 * 
 * Features:
 * - All Vector Charm functionality (movement boost, energy system, etc.)
 * - Creative flight for non-creative players
 * - Works in inventory, hands, or Curios slots
 * - Has its own energy configuration separate from Vector Charm
 */
public class FanpackItem extends VectorCharmItem {
    
    // Track last warning time for each player to avoid spam
    private static final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private static final long WARNING_COOLDOWN = 40; // 2 seconds (40 ticks)
    
    // Energy storage tag (same as VectorCharmItem)
    private static final String ENERGY_TAG = "Energy";
    
    public FanpackItem(Properties properties) {
        super(properties);
    }
    
    /**
     * Override to use Fanpack-specific energy capacity
     * Uses VectorCharm consumption for movement, but checks flight energy consumption too
     */
    @Override
    protected int determineEffectiveCapacity() {
        // Check if config is loaded
        if (Config.vectorCharmEnergyConsume == null) {
            return 0; // Config not loaded yet, assume no energy system
        }
        
        // Check if flight energy consumption is required
        boolean flightEnergyRequired = Config.fanpackFlightEnergyConsume > 0;
        
        // Check if VectorCharm has any movement consumption
        boolean hasMovementConsumption = Config.vectorCharmEnergyConsume.stream().anyMatch(consume -> consume > 0);
        
        // If all movement consumptions are 0 AND flight energy is not required, we don't need capacity
        if (!hasMovementConsumption && !flightEnergyRequired) {
            return 0;
        }
        
        // If configured capacity is 0, we don't store energy
        if (Config.fanpackEnergyCapacity <= 0) {
            return 0;
        }
        
        // Otherwise, use the configured value
        return Config.fanpackEnergyCapacity;
    }
    
    /**
     * Override to use VectorCharm energy consumption for movement
     * Fanpack uses the same consumption array as VectorCharm (no override needed, just use parent)
     */
    @Override
    protected List<Integer> determineEffectiveConsumption() {
        // Use VectorCharm's consumption array for movement
        // Check if VectorCharm config is loaded
        if (Config.vectorCharmEnergyConsume == null) {
            return java.util.Arrays.asList(0, 0, 0, 0, 0, 0, 0);
        }
        
        int capacity = determineEffectiveCapacity();
        java.util.List<Integer> effectiveConsume = new java.util.ArrayList<>();
        
        for (Integer consume : Config.vectorCharmEnergyConsume) {
            // If capacity is 0, we can't consume energy
            if (capacity <= 0) {
                effectiveConsume.add(0);
                continue;
            }
            
            // If consumption is 0, we don't consume energy
            if (consume <= 0) {
                effectiveConsume.add(0);
                continue;
            }
            
            // If configured consumption is greater than capacity, limit it to capacity
            if (consume > capacity) {
                effectiveConsume.add(capacity);
            } else {
                effectiveConsume.add(consume);
            }
        }
        
        return effectiveConsume;
    }
    
    /**
     * Override to use Fanpack-specific energy capacity check
     */
    @Override
    public boolean canStoreEnergy() {
        int capacity = determineEffectiveCapacity();
        return capacity > 0;
    }
    
    /**
     * Override to use Fanpack-specific max energy
     */
    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return determineEffectiveCapacity();
    }
    
    /**
     * Override to use Fanpack-specific energy storage
     */
    @Override
    public void setEnergyStored(ItemStack stack, int energy) {
        if (!canStoreEnergy()) {
            return;
        }
        
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int maxCapacity = determineEffectiveCapacity();
        tag.putInt(ENERGY_TAG, Math.max(0, Math.min(energy, maxCapacity)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Override to use Fanpack-specific energy retrieval
     */
    @Override
    public int getEnergyStored(ItemStack stack) {
        if (!canStoreEnergy()) {
            return 0;
        }
        
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getInt(ENERGY_TAG).orElse(0);
    }
    
    
    /**
     * Server-side heartbeat for flight stage and energy (Curios, hands, or inventory).
     */
    public void tickEquipped(ServerPlayer serverPlayer, ItemStack stack, ServerLevel level) {
        boolean flightEnergyRequired = Config.fanpackFlightEnergyConsume > 0;
        boolean hasEnoughEnergyForFlight = true;

        if (flightEnergyRequired && Config.fanpackEnergyCapacity > 0) {
            int currentEnergy = this.getEnergyStored(stack);
            int maxEnergy = this.getMaxEnergyStored(stack);
            int requiredEnergy = Config.fanpackFlightEnergyConsume;

            hasEnoughEnergyForFlight = currentEnergy >= requiredEnergy;

            if (maxEnergy > 0 && currentEnergy <= maxEnergy * 0.1) {
                long currentTick = level.getGameTime();
                UUID playerId = serverPlayer.getUUID();
                Long lastWarning = lastWarningTime.get(playerId);

                if (lastWarning == null || currentTick - lastWarning >= WARNING_COOLDOWN) {
                    int energyPercent = (int) Math.round((currentEnergy * 100.0) / maxEnergy);
                    serverPlayer.connection.send(
                            new ClientboundSystemChatPacket(
                                    Component.translatable("message.iska_utils.fanpack.low_energy", energyPercent)
                                            .withStyle(net.minecraft.ChatFormatting.RED),
                                    true));
                    level.playSound(
                            null,
                            serverPlayer.getX(),
                            serverPlayer.getY(),
                            serverPlayer.getZ(),
                            SoundEvents.BREEZE_IDLE_AIR,
                            SoundSource.PLAYERS,
                            0.5f,
                            1.0f);
                    lastWarningTime.put(playerId, currentTick);
                }
            }

            if (serverPlayer.getAbilities().flying && currentEnergy >= requiredEnergy
                    && !serverPlayer.getAbilities().instabuild && !serverPlayer.isSpectator()) {
                this.setEnergyStored(stack, currentEnergy - requiredEnergy);
            }
        }

        if (hasEnoughEnergyForFlight) {
            StageRegistry.addPlayerStage(serverPlayer, "iska_utils_internal-funpack_flight0");
        } else if (StageRegistry.playerHasStage(serverPlayer, "iska_utils_internal-funpack_flight0")) {
            StageRegistry.removePlayerStage(serverPlayer, "iska_utils_internal-funpack_flight0");
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, net.minecraft.world.entity.Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
    }
    
    
    /**
     * Add tooltip information
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        
        // Add info about creative flight
        if (Config.fanpackFlightEnergyConsume > 0 && canStoreEnergy()) {
            // Show flight info with energy consumption
            tooltipComponents.accept(Component.translatable("tooltip.iska_utils.fanpack.flight", Config.fanpackFlightEnergyConsume));
        } else {
            // Show flight info without consumption (energy disabled or not required)
            tooltipComponents.accept(Component.translatable("tooltip.iska_utils.fanpack.flight_no_energy"));
        }
    }
    
    /**
     * Static method to check if player has Fanpack (similar to VectorCharmItem.getActiveVectorCharm)
     */
    public static ItemStack getActiveFanpack(Player player, int speedLevel) {
        // Check hands (highest priority)
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof FanpackItem pack) {
            if (pack.hasEnoughEnergy(mainHand, speedLevel)) {
                return mainHand;
            }
        }
        
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof FanpackItem pack) {
            if (pack.hasEnoughEnergy(offHand, speedLevel)) {
                return offHand;
            }
        }
        
        // If Curios is loaded, check Curios slots (second priority)
        if (net.unfamily.iskautils.util.ModUtils.isCuriosLoaded()) {
            ItemStack curioPack = checkCuriosSlotsStatic(player, speedLevel);
            if (curioPack != null) {
                return curioPack;
            }
        }
        
        // Check player inventory (lowest priority)
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof FanpackItem pack) {
                if (pack.hasEnoughEnergy(stack, speedLevel)) {
                    return stack;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Static method to check Curios slots
     * Uses the same approach as VectorCharmItem for consistency
     */
    @Nullable
    private static ItemStack checkCuriosSlotsStatic(Player player, int speedLevel) {
        if (!ModUtils.isCuriosLoaded()) {
            return null;
        }
        ItemStack[] found = new ItemStack[1];
        CurioEquipUtil.forEachEquippedCurioStack(player, stack -> {
            if (found[0] == null && stack.getItem() instanceof FanpackItem pack && pack.hasEnoughEnergy(stack, speedLevel)) {
                found[0] = stack;
            }
        });
        return found[0];
    }
    
    /**
     * Static method to check if player has Fanpack
     */
    public static boolean hasFanpack(Player player) {
        return getActiveFanpack(player, 0) != null; // Check with no energy consumption
    }
    
    /**
     * Static method to get active Fanpack for flight (doesn't check movement energy, only checks if item exists)
     */
    public static ItemStack getActiveFanpackForFlight(Player player) {
        // Check hands (highest priority)
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof FanpackItem) {
            return mainHand;
        }
        
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof FanpackItem) {
            return offHand;
        }
        
        // If Curios is loaded, check Curios slots (second priority)
        if (net.unfamily.iskautils.util.ModUtils.isCuriosLoaded()) {
            ItemStack curioPack = checkCuriosSlotsStaticForFlight(player);
            if (curioPack != null) {
                return curioPack;
            }
        }
        
        // Check player inventory (lowest priority)
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof FanpackItem) {
                return stack;
            }
        }
        
        return null;
    }
    
    /**
     * Static method to check Curios slots for flight (doesn't check energy)
     * Uses the same approach as VectorCharmItem for consistency
     */
    @Nullable
    private static ItemStack checkCuriosSlotsStaticForFlight(Player player) {
        if (!ModUtils.isCuriosLoaded()) {
            return null;
        }
        ItemStack[] found = new ItemStack[1];
        CurioEquipUtil.forEachEquippedCurioStack(player, stack -> {
            if (found[0] == null && stack.getItem() instanceof FanpackItem) {
                found[0] = stack;
            }
        });
        return found[0];
    }
}
