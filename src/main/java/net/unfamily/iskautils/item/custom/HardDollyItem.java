package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Hard Dolly Item - Tool for picking up and moving blocks with their contents
 * 
 * Features:
 * - 4096 durability
 * - Can pick up blocks up to Iron mining level
 * - Stores block state and BlockEntity data
 * - Texture changes when filled (dolly_hard_filled)
 * - Configurable whitelist/blacklist (separate from regular dolly)
 * - Blacklist always takes priority
 */
public class HardDollyItem extends Item {
    
    private static final int MAX_DURABILITY = 4096;
    
    // NBT keys
    private static final String NBT_BLOCK_STATE = "BlockState";
    private static final String NBT_BLOCK_ENTITY = "BlockEntity";
    private static final String NBT_HAS_BLOCK = "HasBlock";
    
    public HardDollyItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }
    
    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        
        // Only work on server side
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        
        // Check if dolly is empty or filled
        boolean hasBlock = hasStoredBlock(stack);
        
        if (player.isShiftKeyDown() && !hasBlock) {
            // Shift+Click on block = Pick up block
            return pickupBlock(level, serverPlayer, stack, pos);
        } else if (!player.isShiftKeyDown() && hasBlock) {
            // Normal click with filled dolly = Place block
            BlockPos placePos = pos.relative(context.getClickedFace());
            return placeBlock(level, serverPlayer, stack, placePos);
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Picks up a block from the world and stores it in the dolly
     */
    private InteractionResult pickupBlock(Level level, ServerPlayer player, ItemStack stack, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        // Check if block is air
        if (state.isAir()) {
            return InteractionResult.FAIL;
        }
        
        // Check if block is indestructible (bedrock, end portal, etc.)
        if (state.getDestroySpeed(level, pos) < 0) {
            player.displayClientMessage(Component.translatable("message.iska_utils.dolly_hard.indestructible"), true);
            return InteractionResult.FAIL;
        }
        
        // Check mining level (max iron)
        if (!canHarvest(state)) {
            player.displayClientMessage(Component.translatable("message.iska_utils.dolly_hard.too_hard"), true);
            return InteractionResult.FAIL;
        }
        
        // Check whitelist/blacklist
        if (!isBlockAllowed(state)) {
            player.displayClientMessage(Component.translatable("message.iska_utils.dolly_hard.not_allowed"), true);
            return InteractionResult.FAIL;
        }
        
        // Get BlockEntity data if present
        CompoundTag blockEntityTag = null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            // Use saveWithFullMetadata to ensure ALL data is saved (including position and ID)
            // This is important for blocks like drawers that might have custom storage
            blockEntityTag = blockEntity.saveWithFullMetadata(level.registryAccess());
        }
        
        // Store block data using CustomData
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();
        
        // Save complete BlockState with all properties (facing, powered, waterlogged, etc.)
        nbt.put(NBT_BLOCK_STATE, NbtUtils.writeBlockState(state));
        nbt.putBoolean(NBT_HAS_BLOCK, true);
        
        if (blockEntityTag != null) {
            nbt.put(NBT_BLOCK_ENTITY, blockEntityTag);
        }
        
        // Set CustomModelData to show filled texture
        nbt.putInt("CustomModelData", 1);
        
        // Update the item stack with new data
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        
        // Remove block from world WITHOUT dropping items
        // First remove the BlockEntity to prevent drops
        if (blockEntity != null) {
            level.removeBlockEntity(pos);
        }
        // Then replace the block with air (flag 2 = no drops, send update to clients)
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        
        // Play sound (scaffold break sound)
        level.playSound(null, pos, SoundEvents.SCAFFOLDING_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        
        // Send feedback
        player.displayClientMessage(Component.translatable("message.iska_utils.dolly_hard.picked_up", 
                Component.translatable(block.getDescriptionId())), true);
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Places the stored block in the world
     */
    private InteractionResult placeBlock(Level level, ServerPlayer player, ItemStack stack, BlockPos pos) {
        // Check if position is replaceable
        BlockState currentState = level.getBlockState(pos);
        if (!currentState.canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        
        // Get stored block data
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();
        if (!nbt.contains(NBT_BLOCK_STATE)) {
            return InteractionResult.FAIL;
        }
        
        // Read complete BlockState with all properties from NBT
        CompoundTag blockStateTag = nbt.getCompound(NBT_BLOCK_STATE);
        BlockState savedState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), blockStateTag);
        
        if (savedState.isAir()) {
            return InteractionResult.FAIL;
        }
        
        // Place block with all its properties (facing, powered, waterlogged, etc.)
        // Flag 2 = send update to clients but don't cause block update (no neighbor updates)
        level.setBlock(pos, savedState, 2);
        
        // Restore BlockEntity data if present
        if (nbt.contains(NBT_BLOCK_ENTITY)) {
            CompoundTag blockEntityTag = nbt.getCompound(NBT_BLOCK_ENTITY);
            
            // Create a copy and update the position to the new location
            CompoundTag loadTag = blockEntityTag.copy();
            loadTag.putInt("x", pos.getX());
            loadTag.putInt("y", pos.getY());
            loadTag.putInt("z", pos.getZ());
            
            // Remove the old BlockEntity if present and create a fresh one from the saved data
            // This ensures that custom storage systems (like drawers) are loaded correctly
            level.removeBlockEntity(pos);
            
            // Load the BlockEntity from the saved NBT data
            BlockEntity newBlockEntity = BlockEntity.loadStatic(pos, savedState, loadTag, level.registryAccess());
            if (newBlockEntity != null) {
                // Set the new BlockEntity in the world
                level.setBlockEntity(newBlockEntity);
                
                // Mark as changed
                newBlockEntity.setChanged();
                
                // Force update to clients (important for chest, furnace, drawer, etc.)
                level.sendBlockUpdated(pos, savedState, savedState, 3);
            }
        } else {
            // Even without BlockEntity, notify the world of the change
            level.sendBlockUpdated(pos, savedState, savedState, 3);
        }
        
        // Clear stored data
        nbt.remove(NBT_BLOCK_STATE);
        nbt.remove(NBT_BLOCK_ENTITY);
        nbt.putBoolean(NBT_HAS_BLOCK, false);
        
        // Remove CustomModelData to show empty texture
        nbt.remove("CustomModelData");
        
        // Update the item stack with cleared data
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        
        // Consume durability
        stack.setDamageValue(stack.getDamageValue() + 1);
        
        // Play sound (scaffold place sound)
        level.playSound(null, pos, SoundEvents.SCAFFOLDING_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        
        // Send feedback
        player.displayClientMessage(Component.translatable("message.iska_utils.dolly_hard.placed", 
                Component.translatable(savedState.getBlock().getDescriptionId())), true);
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Checks if a block can be harvested based on configured mining level tags
     */
    private boolean canHarvest(BlockState state) {
        Block block = state.getBlock();
        
        // If no mining level tags are configured, only reject indestructible blocks
        if (Config.hardDollyAllowedMiningLevelTags.isEmpty()) {
            float hardness = state.getDestroySpeed(null, BlockPos.ZERO);
            return hardness >= 0; // Only reject indestructible blocks (hardness < 0)
        }
        
        // Check if block matches any allowed mining level tag
        for (String tagStr : Config.hardDollyAllowedMiningLevelTags) {
            if (tagStr.startsWith("#")) {
                String tagName = tagStr.substring(1); // Remove #
                try {
                    ResourceLocation tagLocation = ResourceLocation.parse(tagName);
                    TagKey<Block> blockTag = TagKey.create(BuiltInRegistries.BLOCK.key(), tagLocation);
                    if (block.builtInRegistryHolder().is(blockTag)) {
                        return true; // Block matches an allowed mining level
                    }
                } catch (Exception e) {
                    // Invalid tag format, skip
                }
            }
        }
        
        // If block doesn't match any mining level tag, check if it requires no tool (like dirt, sand, etc.)
        // These blocks should be allowed even without matching a tag
        // We check if the block has hardness >= 0 and is mineable without tools
        float hardness = state.getDestroySpeed(null, BlockPos.ZERO);
        if (hardness >= 0 && hardness < 0.6f) {
            // Very soft blocks that don't require tools (dirt, sand, gravel, grass, etc.)
            return true;
        }
        
        return false; // Block requires tools beyond allowed mining levels
    }
    
    /**
     * Checks if a block is allowed by whitelist/blacklist config
     * Logic:
     * - Blacklist always wins
     * - If whitelist is empty, all blocks allowed (except blacklisted)
     * - If whitelist has entries, only those blocks allowed
     */
    private boolean isBlockAllowed(BlockState state) {
        Block block = state.getBlock();
        
        // Check blacklist first (has priority)
        for (String blacklisted : Config.hardDollyBlacklist) {
            if (matchesTagOrId(block, blacklisted)) {
                return false; // Block is blacklisted
            }
        }
        
        // If whitelist is empty, accept all (except blacklisted)
        if (Config.hardDollyWhitelist.isEmpty()) {
            return true;
        }
        
        // Check if block matches any whitelisted tag/ID
        for (String allowed : Config.hardDollyWhitelist) {
            if (matchesTagOrId(block, allowed)) {
                return true;
            }
        }
        
        return false; // Block doesn't match any whitelisted tag/ID
    }
    
    /**
     * Checks if a Block matches a tag or block ID
     * @param block the Block to check
     * @param tagOrId the tag (starting with #) or block ID
     * @return true if it matches
     */
    private boolean matchesTagOrId(Block block, String tagOrId) {
        if (tagOrId.startsWith("#")) {
            // It's a tag
            String tagName = tagOrId.substring(1); // Remove #
            try {
                ResourceLocation tagLocation = ResourceLocation.parse(tagName);
                TagKey<Block> blockTag = TagKey.create(BuiltInRegistries.BLOCK.key(), tagLocation);
                return block.builtInRegistryHolder().is(blockTag);
            } catch (Exception e) {
                // Invalid tag format
                return false;
            }
        } else {
            // It's a block ID
            try {
                ResourceLocation blockId = ResourceLocation.parse(tagOrId);
                ResourceLocation actualId = BuiltInRegistries.BLOCK.getKey(block);
                return blockId.equals(actualId);
            } catch (Exception e) {
                // Invalid block ID format
                return false;
            }
        }
    }
    
    /**
     * Checks if the dolly currently has a block stored
     */
    private boolean hasStoredBlock(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();
        return nbt.getBoolean(NBT_HAS_BLOCK);
    }
    
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Show if dolly has a block stored
        if (hasStoredBlock(stack)) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = customData.copyTag();
            if (nbt.contains(NBT_BLOCK_STATE)) {
                CompoundTag blockStateTag = nbt.getCompound(NBT_BLOCK_STATE);
                BlockState savedState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), blockStateTag);
                Block block = savedState.getBlock();
                tooltipComponents.add(Component.translatable("tooltip.iska_utils.dolly_hard.contains", 
                        Component.translatable(block.getDescriptionId())));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.iska_utils.dolly_hard.empty"));
        }
    }
}
