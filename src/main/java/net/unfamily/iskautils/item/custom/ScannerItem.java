package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.unfamily.iskautils.IskaUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import net.minecraft.world.item.UseAnim;


import java.util.*;
import java.util.stream.Collectors;

/**
 * Item for scanning specific blocks in an area
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class ScannerItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCAN_RANGE = 32; // Scan range in blocks (2 chunks)
    private static final int MAX_TTL = 1800; // 1 minute and 30 seconds (20 ticks per second)
    private static final String TARGET_BLOCK_TAG = "TargetBlock";
    private static final String SCANNER_ID_TAG = "ScannerId";
    private static final int SCAN_DURATION = 60; // 3 seconds (20 ticks per second)
    
    // Map to track active markers by scanner ID
    private static final Map<UUID, List<BlockPos>> ACTIVE_MARKERS = new HashMap<>();
    
    // Map to track TTL for each marker
    private static final Map<BlockPos, Integer> MARKER_TTL = new HashMap<>();

    public ScannerItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .fireResistant());
    }

    /**
     * Event handler for chunk unload
     * This will remove all markers in the chunk that's being unloaded
     */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getChunk() instanceof LevelChunk levelChunk && !levelChunk.getLevel().isClientSide()) {
            ServerLevel level = (ServerLevel) levelChunk.getLevel();
            ChunkPos chunkPos = levelChunk.getPos();
            
            // Remove all markers in this chunk
            removeMarkersInChunk(level, chunkPos);
        }
    }
    
    /**
     * Removes all markers in a specific chunk
     */
    private static void removeMarkersInChunk(ServerLevel level, ChunkPos chunkPos) {
        try {
            // Find all marker positions in this chunk
            List<BlockPos> markersToRemove = MARKER_TTL.keySet().stream()
                .filter(pos -> new ChunkPos(pos).equals(chunkPos))
                .collect(Collectors.toList());
            
            if (!markersToRemove.isEmpty()) {
                // Remove the visual markers
                String command = String.format("kill @e[type=block_display,tag=temp_scan,x=%d,y=%d,z=%d,dx=16,dy=384,dz=16]", 
                        chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ());
                
                level.getServer().getCommands().performPrefixedCommand(
                    level.getServer().createCommandSourceStack(),
                    command
                );
                
                // Remove from our tracking maps
                for (BlockPos pos : markersToRemove) {
                    MARKER_TTL.remove(pos);
                    
                    // Remove from all active scanners
                    for (UUID scannerId : ACTIVE_MARKERS.keySet()) {
                        List<BlockPos> markers = ACTIVE_MARKERS.get(scannerId);
                        markers.remove(pos);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error removing markers in chunk {}: {}", chunkPos, e.getMessage());
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        BlockPos blockPos = context.getClickedPos();
        
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }
        
        // If player is crouching (Shift), register the target block
        if (player.isCrouching()) {
            BlockState state = level.getBlockState(blockPos);
            Block block = state.getBlock();
            
            if (block != Blocks.AIR) {
                // Register the target block in the scanner's NBT
                setTargetBlock(itemStack, block);
                
                player.displayClientMessage(Component.translatable("item.iska_utils.scanner.target_set", block.getName()), true);
                return InteractionResult.SUCCESS;
            }
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }
        
        Block targetBlock = getTargetBlock(itemStack);
        if (targetBlock == null) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_target"), true);
            return InteractionResultHolder.fail(itemStack);
        }
        
        // Remove existing markers
        clearMarkers(player, itemStack);
        
        // Set use duration
        player.startUsingItem(hand);
        
        return InteractionResultHolder.consume(itemStack);
    }

    public InteractionResultHolder<ItemStack> resetTarget(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }
        
        Block targetBlock = getTargetBlock(itemStack);
        if (targetBlock == null) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_target"), true);
            return InteractionResultHolder.fail(itemStack);
        }
        
        // Remove existing markers
        clearMarkers(player, itemStack);
        
        // Set use duration
        player.startUsingItem(hand);
        
        return InteractionResultHolder.consume(itemStack);
    }
    

    @Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.BOW;
	}

	@Override
	public int getUseDuration(ItemStack itemstack, LivingEntity livingEntity) {
		return 300;
	}
    
    @Override
    public void releaseUsing(ItemStack itemstack, Level world, LivingEntity entity, int time) {
        if (world.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        
        // Check if item was held for enough time
        Block targetBlock = getTargetBlock(itemstack);
        if (targetBlock == null) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_target"), true);
            return;
        }
        
        // The time parameter represents remaining time, so we need to check if it's LESS than a certain value
        // to determine if user has held long enough
        int totalDuration = getUseDuration(itemstack, entity);
        int timeUsed = totalDuration - time;
        
        if (timeUsed >= SCAN_DURATION) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.scan_started", targetBlock.getName()), true);
            scanArea(player, itemstack);
        } else {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.interrupted"), true);
            resetTarget(player.level(), player, InteractionHand.MAIN_HAND);
        }
    }
    
    /**
     * Sets the target block in the scanner
     */
    private void setTargetBlock(ItemStack itemStack, Block block) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TARGET_BLOCK_TAG, BuiltInRegistries.BLOCK.getKey(block).toString());
        
        // Make sure the scanner has a unique ID
        if (!tag.contains(SCANNER_ID_TAG)) {
            tag.putUUID(SCANNER_ID_TAG, UUID.randomUUID());
        }
        
        // Save data to ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Gets the target block from the scanner
     */
    private Block getTargetBlock(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TARGET_BLOCK_TAG)) {
            return null;
        }
        
        String blockId = tag.getString(TARGET_BLOCK_TAG);
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
    }
    
    /**
     * Removes all existing markers for this scanner
     */
    private void clearMarkers(Player player, ItemStack itemStack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(SCANNER_ID_TAG)) {
            UUID scannerId = tag.getUUID(SCANNER_ID_TAG);
            
            if (ACTIVE_MARKERS.containsKey(scannerId)) {
                List<BlockPos> markers = ACTIVE_MARKERS.get(scannerId);
                for (BlockPos pos : markers) {
                    // Remove the marker
                    removeMarker(serverPlayer, pos);
                }
                
                markers.clear();
            }
        }
    }
    
    /**
     * Removes a single marker
     */
    private void removeMarker(ServerPlayer player, BlockPos pos) {
        try {
            String command = String.format("kill @e[type=block_display,tag=temp_scan,x=%d,y=%d,z=%d,distance=..1]", 
                    pos.getX(), pos.getY(), pos.getZ());
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                command
            );
            
            MARKER_TTL.remove(pos);
        } catch (Exception e) {
            LOGGER.error("Error removing a marker: {}", e.getMessage());
        }
    }
    
    /**
     * Scans the area for target blocks
     */
    private void scanArea(ServerPlayer player, ItemStack itemStack) {
        Block targetBlock = getTargetBlock(itemStack);
        if (targetBlock == null || player.level() == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID scannerId = tag.getUUID(SCANNER_ID_TAG);
        
        // Remove any existing markers
        clearMarkers(player, itemStack);
        
        // Create a list for this scanner's markers
        List<BlockPos> scannerMarkers = new ArrayList<>();
        ACTIVE_MARKERS.put(scannerId, scannerMarkers);
        
        // Get player position
        BlockPos playerPos = player.blockPosition();
        ChunkPos playerChunkPos = new ChunkPos(playerPos);
        
        // Scan in a radius of 2 chunks
        int markersFound = 0;
        
        for (int chunkX = playerChunkPos.x - 1; chunkX <= playerChunkPos.x + 1; chunkX++) {
            for (int chunkZ = playerChunkPos.z - 1; chunkZ <= playerChunkPos.z + 1; chunkZ++) {
                ChunkPos currentChunkPos = new ChunkPos(chunkX, chunkZ);
                
                // Check if chunk is loaded
                if (!level.isLoaded(BlockPos.containing(currentChunkPos.getMiddleBlockX(), 0, currentChunkPos.getMiddleBlockZ()))) {
                    continue;
                }
                
                // Scan the chunk
                for (int x = currentChunkPos.getMinBlockX(); x <= currentChunkPos.getMaxBlockX(); x++) {
                    for (int z = currentChunkPos.getMinBlockZ(); z <= currentChunkPos.getMaxBlockZ(); z++) {
                        for (int y = level.getMinBuildHeight(); y <= level.getMaxBuildHeight(); y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            
                            // Check if it's too far from the player
                            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > SCAN_RANGE * SCAN_RANGE) {
                                continue;
                            }
                            
                            // Check the block
                            if (level.getBlockState(pos).getBlock() == targetBlock) {
                                // Create a marker
                                createMarker(player, pos);
                                scannerMarkers.add(pos);
                                markersFound++;
                                
                                // Add TTL for this marker
                                MARKER_TTL.put(pos, MAX_TTL);
                            }
                        }
                    }
                }
            }
        }
        
        player.displayClientMessage(Component.translatable("item.iska_utils.scanner.found_blocks", 
                markersFound, targetBlock.getName()), true);
    }
    
    /**
     * Creates a marker at the specified position
     */
    private void createMarker(ServerPlayer player, BlockPos pos) {
        try {
            // First create a marker for exact positioning
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                String.format("summon minecraft:marker %d %d %d {Tags:[\"temp_scan_marker\"]}", pos.getX(), pos.getY(), pos.getZ())
            );
            
            // Then create a block_display at the block center with glowing effect
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                "execute at @e[type=marker,tag=temp_scan_marker,limit=1] run summon block_display ~0.0 ~0.0 ~0.0 " +
                "{Tags:[\"temp_scan\"],Glowing:1b,block_state:{Name:\"iska_utils:scan_block\"}," +
                "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.55f,-0.05f,-0.55f],scale:[1.1f,1.1f,1.1f]}}"
            );
            
            // Remove the marker
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                "kill @e[type=marker,tag=temp_scan_marker]"
            );
        } catch (Exception e) {
            LOGGER.error("Error creating a marker: {}", e.getMessage());
        }
    }
    
    /**
     * Handles item tick (called every game tick)
     * Used to update markers and their TTL
     */
    public static void tick(ServerLevel level) {
        Iterator<Map.Entry<BlockPos, Integer>> markerIterator = MARKER_TTL.entrySet().iterator();
        
        while (markerIterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = markerIterator.next();
            BlockPos pos = entry.getKey();
            int ttl = entry.getValue() - 1;
            
            if (ttl <= 0) {
                // Remove expired marker
                try {
                    String command = String.format("kill @e[type=block_display,tag=temp_scan,x=%d,y=%d,z=%d,distance=..1]", 
                            pos.getX(), pos.getY(), pos.getZ());
                    level.getServer().getCommands().performPrefixedCommand(
                        level.getServer().createCommandSourceStack(),
                        command
                    );
                } catch (Exception e) {
                    LOGGER.error("Error removing an expired marker: {}", e.getMessage());
                }
                
                // Remove marker from TTL map
                markerIterator.remove();
                
                // Remove marker from all active scanners
                for (UUID scannerId : ACTIVE_MARKERS.keySet()) {
                    List<BlockPos> markers = ACTIVE_MARKERS.get(scannerId);
                    markers.remove(pos);
                }
            } else {
                // Update TTL
                entry.setValue(ttl);
            }
        }
    }
    
    /**
     * Clean up all markers when the server/world starts
     * This ensures no markers remain after reloading the datapack
     */
    public static void cleanupAllMarkers(ServerLevel level) {
        try {
            // Kill all block_display entities with the temp_scan tag
            level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack(),
                "kill @e[type=block_display,tag=temp_scan]"
            );
            
            // Clear the internal maps
            ACTIVE_MARKERS.clear();
            MARKER_TTL.clear();
        } catch (Exception e) {
            LOGGER.error("Error cleaning up scanner markers: {}", e.getMessage());
        }
    }
} 