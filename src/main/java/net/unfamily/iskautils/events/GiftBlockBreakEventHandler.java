package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for Gift block break events.
 * When a Gift block is broken and the config flag is enabled,
 * places a Hard Ice block after 3 seconds (60 ticks).
 */
@EventBusSubscriber
public class GiftBlockBreakEventHandler {
    
    // Track pending hard ice placements: BlockPos -> tick when to place
    private static final Map<BlockPos, Long> PENDING_PLACEMENTS = new HashMap<>();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Check if the broken block is a Gift block
        BlockState brokenBlock = event.getState();
        if (!brokenBlock.is(ModBlocks.GIFT.get())) {
            return;
        }

        // Server side only
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        // Get the player who broke the block
        Player player = event.getPlayer();
        if (player != null) {
            // Show message on actionbar (like hard_ice)
            player.displayClientMessage(
                Component.translatable("message.iska_utils.gift.break_message"),
                true // actionbar
            );
        }

        // Check if the feature is enabled
        if (!Config.giftPlaceHardIce) {
            return;
        }
        
        BlockPos pos = event.getPos();
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
        
        if (PENDING_PLACEMENTS.isEmpty()) {
            return;
        }
        
        long currentTick = level.getGameTime();
        
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
