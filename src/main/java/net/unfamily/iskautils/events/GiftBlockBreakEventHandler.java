package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Gift block right-click events.
 * When a Gift block is right-clicked and the config flag is enabled,
 * drops the greedy shield, removes the block, and places a Hard Ice block after 3 seconds (60 ticks).
 * Requires confirmation click: first click shows warning message, second click opens the gift.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class GiftBlockBreakEventHandler {
    
    // Track pending hard ice placements: BlockPos -> tick when to place
    private static final Map<BlockPos, Long> PENDING_PLACEMENTS = new HashMap<>();
    
    // Track pending confirmations: Player UUID + BlockPos -> tick when first clicked
    private static final Map<String, Long> PENDING_CONFIRMATIONS = new HashMap<>();
    
    private static final int CONFIRMATION_TIMEOUT_TICKS = 100; // 5 seconds

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Server side only
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        // Check if the clicked block is a Gift block
        BlockState clickedBlock = level.getBlockState(event.getPos());
        if (!clickedBlock.is(ModBlocks.GIFT.get())) {
            return;
        }

        // Cancel the default interaction
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        
        // Get the player who clicked the block
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        
        BlockPos pos = event.getPos();
        UUID playerUuid = player.getUUID();
        String confirmationKey = playerUuid.toString() + "_" + pos.asLong();
        long currentTick = level.getGameTime();
        
        // Clean up any old confirmations for this player on different positions
        String playerPrefix = playerUuid.toString() + "_";
        PENDING_CONFIRMATIONS.entrySet().removeIf(entry -> 
            entry.getKey().startsWith(playerPrefix) && !entry.getKey().equals(confirmationKey) &&
            (currentTick - entry.getValue()) > CONFIRMATION_TIMEOUT_TICKS
        );
        
        // Check if this is a confirmation click (second click within timeout)
        Long firstClickTick = PENDING_CONFIRMATIONS.get(confirmationKey);
        if (firstClickTick != null && firstClickTick < currentTick && (currentTick - firstClickTick) <= CONFIRMATION_TIMEOUT_TICKS) {
            // This is a confirmation click - verify block is still Gift and open it
            BlockState currentBlock = level.getBlockState(pos);
            if (currentBlock.is(ModBlocks.GIFT.get())) {
                PENDING_CONFIRMATIONS.remove(confirmationKey);
                openGift(level, player, pos);
            } else {
                // Block changed, clear confirmation
                PENDING_CONFIRMATIONS.remove(confirmationKey);
            }
        } else {
            // This is the first click - show confirmation message in actionbar
            // Remove any existing confirmation for this position first
            if (firstClickTick != null) {
                PENDING_CONFIRMATIONS.remove(confirmationKey);
            }
            PENDING_CONFIRMATIONS.put(confirmationKey, currentTick);
            player.displayClientMessage(
                Component.translatable("message.iska_utils.gift.confirmation"),
                true // actionbar
            );
        }
    }
    
    /**
     * Opens the gift: drops greedy shield, removes block, schedules hard ice placement
     */
    private static void openGift(ServerLevel level, Player player, BlockPos pos) {
        // Show message on actionbar
        player.displayClientMessage(
            Component.translatable("message.iska_utils.gift.break_message"),
            true // actionbar
        );
        
        // Drop greedy shield
        ItemStack greedyShieldStack = new ItemStack(ModItems.GREEDY_SHIELD.get(), 1);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        ItemEntity greedyShieldEntity = new ItemEntity(level, x, y, z, greedyShieldStack);
        greedyShieldEntity.setDefaultPickUpDelay();
        level.addFreshEntity(greedyShieldEntity);
        
        // Remove the Gift block by replacing with air (prevents loot table drop)
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        
        // Check if the feature is enabled for hard ice placement
        if (!Config.giftPlaceHardIce) {
            return;
        }
        
        long currentTick = level.getGameTime();
        long executeAtTick = currentTick + 60; // 3 seconds = 60 ticks

        // Schedule hard ice placement after 3 seconds
        PENDING_PLACEMENTS.put(pos, executeAtTick);
    }
    
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        long currentTick = level.getGameTime();
        
        // Clean up expired confirmations
        PENDING_CONFIRMATIONS.entrySet().removeIf(entry -> 
            (currentTick - entry.getValue()) > CONFIRMATION_TIMEOUT_TICKS
        );
        
        if (PENDING_PLACEMENTS.isEmpty()) {
            return;
        }
        
        // Check all pending placements
        PENDING_PLACEMENTS.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            long executeAtTick = entry.getValue();
            
            if (currentTick >= executeAtTick) {
                // Time to place the block
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, ModBlocks.HARD_ICE.get().defaultBlockState(), 3);
                }
                return true; // Remove from map
            }
            return false; // Keep in map
        });
    }
}
