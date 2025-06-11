package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.List;

/**
 * Item can store a block or mob target to be transferred to a scanner
 */
public class ScannerChipItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Constant tags used to store data in the item
    private static final String TARGET_BLOCK_TAG = "TargetBlock";
    private static final String TARGET_MOB_TAG = "TargetMob";
    
    public ScannerChipItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
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
        
        // If the player is crouching (Shift), register the target block
        if (player.isCrouching()) {
            BlockState state = level.getBlockState(blockPos);
            Block block = state.getBlock();
            
            if (block != Blocks.AIR) {
                // Register the target block in the chip
                setTargetBlock(itemStack, block);
                
                player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.target_set", block.getName()), true);
                return InteractionResult.SUCCESS;
            }
        }
        
        // If there is a scanner in the main hand, transfer the target
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHandItem.getItem() instanceof ScannerItem scanner) {
            // Check if we have a target in the chip
            Block targetBlock = getTargetBlock(itemStack);
            String targetMob = getTargetMob(itemStack);
            
            if (targetBlock != null) {
                // Transfer the target block to the scanner
                transferBlockTargetToScanner(itemStack, mainHandItem, scanner, player);
                return InteractionResult.SUCCESS;
            } else if (targetMob != null) {
                // Transfer the target mob to the scanner
                transferMobTargetToScanner(itemStack, mainHandItem, scanner, player);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.no_target"), true);
                return InteractionResult.FAIL;
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
        
        // If the main hand has a scanner, transfer the target
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHandItem.getItem() instanceof ScannerItem scanner) {
            // Check if we have a target in the chip
            Block targetBlock = getTargetBlock(itemStack);
            String targetMob = getTargetMob(itemStack);
            
            if (targetBlock != null) {
                // Transfer the target block to the scanner
                transferBlockTargetToScanner(itemStack, mainHandItem, scanner, player);
                return InteractionResultHolder.success(itemStack);
            } else if (targetMob != null) {
                // Transfer the target mob to the scanner
                transferMobTargetToScanner(itemStack, mainHandItem, scanner, player);
                return InteractionResultHolder.success(itemStack);
            } else {
                player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.no_target"), true);
                return InteractionResultHolder.fail(itemStack);
            }
        }
        
        return InteractionResultHolder.pass(itemStack);
    }
    
    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
        if (sourceentity instanceof Player player && !(entity instanceof Player)) {
            // Select the mob as the target
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            
            // Set the target mob
            setTargetMob(itemstack, entityId);
            
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.mob_target_set", entity.getName()), true);
            
            return true; // Don't damage the mob
        }
        return false;
    }
    
    /**
     * Set the target block in the chip
     */
    private void setTargetBlock(ItemStack itemStack, Block block) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove any target mob
        tag.remove(TARGET_MOB_TAG);
        
        // Set the target block
        tag.putString(TARGET_BLOCK_TAG, BuiltInRegistries.BLOCK.getKey(block).toString());
        
        // Save the data in the ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Get the target block from the chip
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
     * Set the target mob in the chip and remove any target block
     */
    private void setTargetMob(ItemStack itemStack, String mobId) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove target block if present
        tag.remove(TARGET_BLOCK_TAG);
        
        // Set target mob
        tag.putString(TARGET_MOB_TAG, mobId);
        
        // Save the data in the ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Get the target mob from the chip
     */
    private String getTargetMob(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TARGET_MOB_TAG)) {
            return null;
        }
        
        return tag.getString(TARGET_MOB_TAG);
    }
    
    /**
     * Transfer the target block from the chip to the scanner
     */
    private void transferBlockTargetToScanner(ItemStack chipStack, ItemStack scannerStack, ScannerItem scanner, Player player) {
        Block targetBlock = getTargetBlock(chipStack);
        if (targetBlock == null) {
            return;
        }
        
        // Get the chip tag
        CompoundTag chipTag = chipStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Get the scanner tag
        CompoundTag scannerTag = scannerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove previous targets from the scanner
        scannerTag.remove(TARGET_MOB_TAG);
        
        // Set the new target block
        scannerTag.putString(TARGET_BLOCK_TAG, chipTag.getString(TARGET_BLOCK_TAG));
        
        // Ensure the scanner has a unique ID
        if (!scannerTag.contains("ScannerId")) {
            scannerTag.putUUID("ScannerId", java.util.UUID.randomUUID());
        }
        
        // Save the data in the scanner
        scannerStack.set(DataComponents.CUSTOM_DATA, CustomData.of(scannerTag));
        
        player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success", targetBlock.getName()), true);
    }
    
    /**
     * Transfer the target mob from the chip to the scanner
     */
    private void transferMobTargetToScanner(ItemStack chipStack, ItemStack scannerStack, ScannerItem scanner, Player player) {
        String targetMob = getTargetMob(chipStack);
        if (targetMob == null) {
            return;
        }
        
        // Get the chip tag
        CompoundTag chipTag = chipStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Get the scanner tag
        CompoundTag scannerTag = scannerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove previous targets from the scanner
        scannerTag.remove(TARGET_BLOCK_TAG);
        
        // Set the new target mob
        scannerTag.putString(TARGET_MOB_TAG, chipTag.getString(TARGET_MOB_TAG));
        
        // Ensure the scanner has a unique ID
        if (!scannerTag.contains("ScannerId")) {
            scannerTag.putUUID("ScannerId", java.util.UUID.randomUUID());
        }
        
        // Save the data in the scanner
        scannerStack.set(DataComponents.CUSTOM_DATA, CustomData.of(scannerTag));
        
        player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success_mob", 
                getLocalizedMobName(targetMob)), true);
    }
    
    /**
     * Crea un nome localizzato per un mob a partire dal suo ID
     */
    private Component getLocalizedMobName(String mobId) {
        if (mobId == null) return Component.literal("Unknown");
        
        // Estrai namespace e path dall'ID
        String namespace = "minecraft";
        String path = mobId;
        
        if (mobId.contains(":")) {
            String[] parts = mobId.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        }
        
        // Prova a usare la chiave di traduzione specifica per il namespace
        String translationKey = "entity." + namespace + "." + path;
        Component translated = Component.translatable(translationKey);
        
        // Se il namespace non è minecraft, aggiungi il namespace al nome se la traduzione fallisce
        if (!namespace.equals("minecraft")) {
            // Controlla se la traduzione ha avuto successo
            String translatedText = translated.getString();
            if (translatedText.equals(translationKey)) {
                // La traduzione ha fallito, usa un formato alternativo
                return Component.literal(namespace + ":" + path);
            }
        }
        
        return translated;
    }
    
    /**
     * Add tooltip information to the item
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Target information
        Block targetBlock = getTargetBlock(stack);
        String targetMob = getTargetMob(stack);
        
        if (targetBlock != null) {
            Component targetText = Component.translatable("item.iska_utils.scanner_chip.tooltip.target_block")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(targetBlock.getName().copy().withStyle(ChatFormatting.WHITE));
            
            tooltipComponents.add(targetText);
        } else if (targetMob != null) {
            Component targetText = Component.translatable("item.iska_utils.scanner_chip.tooltip.target_mob")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(getLocalizedMobName(targetMob).copy().withStyle(ChatFormatting.WHITE));
            
            tooltipComponents.add(targetText);
        } else {
            Component noTargetText = Component.translatable("item.iska_utils.scanner_chip.tooltip.no_target")
                .withStyle(style -> style.withColor(ChatFormatting.GRAY));
            
            tooltipComponents.add(noTargetText);
        }
        
        // Instructions
        Component instructionsText = Component.translatable("item.iska_utils.scanner_chip.tooltip.instructions")
            .withStyle(style -> style.withColor(ChatFormatting.YELLOW));
        
        tooltipComponents.add(instructionsText);
    }
}