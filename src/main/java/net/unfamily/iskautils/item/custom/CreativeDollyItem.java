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
 * Creative Dolly Item - Indestructible tool for picking up and moving ANY blocks
 * 
 * Features:
 * - Infinite durability (indestructible)
 * - Can pick up ANY block, including indestructible ones (bedrock, end portal, etc.)
 * - No whitelist/blacklist restrictions
 * - No mining level restrictions
 * - Stores block state and BlockEntity data
 * - Texture changes when filled (dolly_creative_filled)
 * - Creative mode only
 */
public class CreativeDollyItem extends Item {
    
    // NBT keys
    private static final String NBT_BLOCK_STATE = "BlockState";
    private static final String NBT_BLOCK_ENTITY = "BlockEntity";
    private static final String NBT_HAS_BLOCK = "HasBlock";
    
    public CreativeDollyItem(Properties properties) {
        super(properties);
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
     * Creative Dolly uses config for unbreakable blocks, but can move everything else
     */
    private InteractionResult pickupBlock(Level level, ServerPlayer player, ItemStack stack, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        // Check if block is air
        if (state.isAir()) {
            return InteractionResult.FAIL;
        }
        
        // Check if block is indestructible (bedrock, end portal, etc.)
        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed < 0) {
            // Check if we can move unbreakable blocks
            // If canMoveAllUnbreakable is false, we still check whitelist if it's not empty
            // This allows specific unbreakable blocks (like hard_ice) to be moved even when
            // canMoveAllUnbreakable is false
            boolean canCheckWhitelist = Config.creativeDollyCanMoveAllUnbreakable || !Config.creativeDollyUnbreakableWhitelist.isEmpty();
            
            if (!canCheckWhitelist) {
                player.displayClientMessage(Component.translatable("message.iska_utils.dolly_creative.indestructible"), true);
                return InteractionResult.FAIL;
            }
            
            // Check unbreakable whitelist/blacklist
            if (!isUnbreakableBlockAllowed(block)) {
                player.displayClientMessage(Component.translatable("message.iska_utils.dolly_creative.indestructible"), true);
                return InteractionResult.FAIL;
            }
        }
        
        // Creative Dolly: Can pick up any block (including indestructible ones if allowed by config)
        
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
        player.displayClientMessage(Component.translatable("message.iska_utils.dolly_creative.picked_up", 
                Component.translatable(block.getDescriptionId())), true);
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Places the stored block in the world
     */
    private InteractionResult placeBlock(Level level, ServerPlayer player, ItemStack stack, BlockPos pos) {
        // Check if target position is valid
        if (!level.getBlockState(pos).canBeReplaced()) {
            player.displayClientMessage(Component.translatable("message.iska_utils.dolly_creative.cannot_place"), true);
            return InteractionResult.FAIL;
        }
        
        // Get stored block data
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();
        
        if (!nbt.contains(NBT_BLOCK_STATE)) {
            player.displayClientMessage(Component.translatable("message.iska_utils.dolly_creative.no_block"), true);
            return InteractionResult.FAIL;
        }
        
        // Read saved BlockState
        CompoundTag blockStateTag = nbt.getCompound(NBT_BLOCK_STATE);
        BlockState savedState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), blockStateTag);
        
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
        
        // Creative Dolly: NO durability consumption
        
        // Play sound (scaffold place sound)
        level.playSound(null, pos, SoundEvents.SCAFFOLDING_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        
        // Send feedback
        player.displayClientMessage(Component.translatable("message.iska_utils.dolly_creative.placed", 
                Component.translatable(savedState.getBlock().getDescriptionId())), true);
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Checks if an unbreakable block is allowed by whitelist/blacklist config
     * Logic:
     * - Blacklist always wins
     * - If whitelist is empty, all unbreakable blocks allowed (except blacklisted ones are always rejected)
     * - If whitelist has entries, only those blocks allowed
     */
    private boolean isUnbreakableBlockAllowed(Block block) {
        // Check blacklist first (has priority)
        for (String blacklisted : Config.creativeDollyUnbreakableBlacklist) {
            if (matchesBlockId(block, blacklisted)) {
                return false; // Block is blacklisted
            }
        }
        
        // If whitelist is empty
        if (Config.creativeDollyUnbreakableWhitelist.isEmpty()) {
            // If canMoveAllUnbreakable is true, allow all (blacklist already checked)
            // This is the default behavior for Creative Dolly
            if (Config.creativeDollyCanMoveAllUnbreakable) {
                return true;
            }
            // Otherwise reject all
            return false;
        }
        
        // Check if block matches any whitelisted ID
        for (String allowed : Config.creativeDollyUnbreakableWhitelist) {
            if (matchesBlockId(block, allowed)) {
                return true;
            }
        }
        
        return false; // Block doesn't match any whitelisted ID
    }
    
    /**
     * Checks if a Block matches a block ID (no tags for unbreakable blocks)
     * @param block the Block to check
     * @param blockId the block ID
     * @return true if it matches
     */
    private boolean matchesBlockId(Block block, String blockId) {
        try {
            ResourceLocation blockIdLocation = ResourceLocation.parse(blockId);
            ResourceLocation actualId = BuiltInRegistries.BLOCK.getKey(block);
            return blockIdLocation.equals(actualId);
        } catch (Exception e) {
            // Invalid block ID format
            return false;
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
                tooltipComponents.add(Component.translatable("tooltip.iska_utils.dolly_creative.contains", 
                        Component.translatable(block.getDescriptionId())));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.iska_utils.dolly_creative.empty"));
        }
        
        // Add info lines based on config
        for (int i = 0; i < Config.creativeDollyInfoLines; i++) {
            String key = "tooltip.iska_utils.dolly_creative.info" + i;
            Component infoLine = Component.translatable(key);
            // Only add if translation exists (not equal to the key itself)
            if (!infoLine.getString().equals(key)) {
                tooltipComponents.add(infoLine);
            }
        }
    }
}
