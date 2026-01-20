package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.unfamily.iskautils.Config;

import java.util.List;

/**
 * Item can store a block or mob target to be transferred to a scanner
 */
public class ScannerChipItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Constant tags used to store data in the item
    private static final String TARGET_BLOCK_TAG = "TargetBlock";
    private static final String TARGET_MOB_TAG = "TargetMob";
    private static final String TARGET_GEN_TAG = "TargetGeneric";
    private static final String INITIALIZED_TAG = "Initialized";
    private static final String MINING_LEVEL_TAG = "MiningLevel";
    
    public ScannerChipItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }
    
    @Override
    public void onCraftedBy(ItemStack itemStack, Level level, Player player) {
        super.onCraftedBy(itemStack, level, player);
        
        // Inizializza l'NBT per i chip specifici
        initializeSpecializedChip(itemStack);
    }
    
    @Override
    public void inventoryTick(ItemStack itemStack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(itemStack, level, entity, slotId, isSelected);
        
        // Verifica se il chip è già inizializzato
        if (level.isClientSide) return;
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(INITIALIZED_TAG)) return;
        
        // Inizializza l'NBT per i chip specifici se non è già impostato
        initializeSpecializedChip(itemStack);
        
        // Imposta il flag di inizializzazione
        tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(INITIALIZED_TAG, true);
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Inizializza i chip specializzati con il loro target predefinito
     */
    private void initializeSpecializedChip(ItemStack itemStack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        String itemPath = itemId.getPath();
        
        if (itemPath.contains("scanner_chip_ores") && getGenericTarget(itemStack) == null) {
            // Inizializza il chip per minerali
            setGenericTarget(itemStack, "ores");
        } else if (itemPath.contains("scanner_chip_mobs") && getGenericTarget(itemStack) == null) {
            // Inizializza il chip per mostri
            setGenericTarget(itemStack, "mobs");
        }
    }
    
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack itemStack = super.getDefaultInstance();
        
        // Inizializza l'NBT per i chip specifici
        initializeSpecializedChip(itemStack);
        
        // Imposta il flag di inizializzazione
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(INITIALIZED_TAG, true);
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        return itemStack;
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
        
        // Check if this is a specialized chip (ores or mobs) - these cannot be overwritten
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        String itemPath = itemId.getPath();
        boolean isSpecializedChip = itemPath.contains("scanner_chip_ores") || itemPath.contains("scanner_chip_mobs");
        
        // If the player is crouching (Shift), register the target block, but only for regular chips
        if (player.isCrouching() && !isSpecializedChip) {
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
            String genericTarget = getGenericTarget(itemStack);
            
            if (targetBlock != null) {
                // Transfer the target block to the scanner
                transferBlockTargetToScanner(itemStack, mainHandItem, scanner, player);
                return InteractionResult.SUCCESS;
            } else if (targetMob != null) {
                // Transfer the target mob to the scanner
                transferMobTargetToScanner(itemStack, mainHandItem, scanner, player);
                return InteractionResult.SUCCESS;
            } else if (genericTarget != null) {
                // Transfer the generic target to the scanner
                transferGenericTargetToScanner(itemStack, mainHandItem, scanner, player);
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
        
        // Check if this is a specialized chip (ores or mobs)
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        String itemPath = itemId.getPath();
        boolean isSpecializedChip = itemPath.contains("scanner_chip_ores") || itemPath.contains("scanner_chip_mobs");
        
        // If the player is crouching (Shift), set the generic target based on the chip type
        if (player.isCrouching()) {
            // For specialized chips, automatically set the target
            if (isSpecializedChip) {
                if (itemPath.contains("scanner_chip_ores")) {
                    // If chip is in main hand, cycle mining level
                    if (hand == InteractionHand.MAIN_HAND) {
                        int currentLevel = getMiningLevel(itemStack);
                        // Cycle: 0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 100 -> 0
                        int newLevel;
                        if (currentLevel == 0) {
                            newLevel = 1;
                        } else if (currentLevel == 1) {
                            newLevel = 2;
                        } else if (currentLevel == 2) {
                            newLevel = 3;
                        } else if (currentLevel == 3) {
                            newLevel = 4;
                        } else if (currentLevel == 4) {
                            newLevel = 5;
                        } else if (currentLevel == 5) {
                            newLevel = 100;
                        } else { // currentLevel == 100
                            newLevel = 0;
                        }
                        setMiningLevel(itemStack, newLevel);
                        
                        // Display message to player
                        String levelText = switch (newLevel) {
                            case 0 -> "item.iska_utils.scanner_chip.mining_level.none";
                            case 1 -> "item.iska_utils.scanner_chip.mining_level.wood";
                            case 2 -> "item.iska_utils.scanner_chip.mining_level.stone";
                            case 3 -> "item.iska_utils.scanner_chip.mining_level.iron";
                            case 4 -> "item.iska_utils.scanner_chip.mining_level.diamond";
                            case 5 -> "item.iska_utils.scanner_chip.mining_level.netherite";
                            case 100 -> "item.iska_utils.scanner_chip.mining_level.modded";
                            default -> "item.iska_utils.scanner_chip.mining_level.none";
                        };
                        player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.mining_level.set", 
                                Component.translatable(levelText)), true);
                        return InteractionResultHolder.success(itemStack);
                    }
                    // Set the chip to scan for all ores if not already set
                    if (getGenericTarget(itemStack) == null) {
                        setGenericTarget(itemStack, "ores");
                    }
                    return InteractionResultHolder.success(itemStack);
                } else if (itemPath.contains("scanner_chip_mobs")) {
                    // Set the chip to scan for all mobs if not already set
                    if (getGenericTarget(itemStack) == null) {
                        setGenericTarget(itemStack, "mobs");
                    }
                    return InteractionResultHolder.success(itemStack);
                }
            } else {
                // For regular chips, check the item's registry name to determine its type
                // This is left for backward compatibility
                // ... existing code for regular chips ...
            }
        }
        
        // If the main hand has a scanner, transfer the target
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHandItem.getItem() instanceof ScannerItem scanner) {
            // Check if we have a target in the chip
            Block targetBlock = getTargetBlock(itemStack);
            String targetMob = getTargetMob(itemStack);
            String genericTarget = getGenericTarget(itemStack);
            
            if (targetBlock != null) {
                // Transfer the target block to the scanner
                transferBlockTargetToScanner(itemStack, mainHandItem, scanner, player);
                return InteractionResultHolder.success(itemStack);
            } else if (targetMob != null) {
                // Transfer the target mob to the scanner
                transferMobTargetToScanner(itemStack, mainHandItem, scanner, player);
                return InteractionResultHolder.success(itemStack);
            } else if (genericTarget != null) {
                // Transfer the generic target to the scanner
                transferGenericTargetToScanner(itemStack, mainHandItem, scanner, player);
                return InteractionResultHolder.success(itemStack);
            } else {
                player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.no_target"), true);
                return InteractionResultHolder.fail(itemStack);
            }
        }
        
        return InteractionResultHolder.pass(itemStack);
    }
    
    /**
     * Set the target block in the chip
     */
    public void setTargetBlock(ItemStack itemStack, Block block) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove any target mob or generic target
        tag.remove(TARGET_MOB_TAG);
        tag.remove(TARGET_GEN_TAG);
        
        // Set the target block
        tag.putString(TARGET_BLOCK_TAG, BuiltInRegistries.BLOCK.getKey(block).toString());
        
        // Save the data in the ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Get the target block from the chip
     */
    public Block getTargetBlock(ItemStack itemStack) {
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
    public void setTargetMob(ItemStack itemStack, String mobId) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove target block and generic target if present
        tag.remove(TARGET_BLOCK_TAG);
        tag.remove(TARGET_GEN_TAG);
        
        // Set target mob
        tag.putString(TARGET_MOB_TAG, mobId);
        
        // Save the data in the ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Get the target mob from the chip
     */
    public String getTargetMob(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TARGET_MOB_TAG)) {
            return null;
        }
        
        return tag.getString(TARGET_MOB_TAG);
    }

    /**
     * Set the generic target in the chip and remove any specific targets
     * @param itemStack The chip item stack
     * @param targetType Either "ores" or "mobs"
     */
    public void setGenericTarget(ItemStack itemStack, String targetType) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove specific targets if present
        tag.remove(TARGET_BLOCK_TAG);
        tag.remove(TARGET_MOB_TAG);
        
        // Set generic target
        tag.putString(TARGET_GEN_TAG, targetType);
        
        // Save the data in the ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Get the generic target from the chip
     * @return "ores", "mobs" or null if not set
     */
    public String getGenericTarget(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TARGET_GEN_TAG)) {
            return null;
        }
        
        return tag.getString(TARGET_GEN_TAG);
    }
    
    /**
     * Set the mining level in the chip (0, 1-5, 100)
     * 0 = no filter (default)
     * 1-5 = vanilla mining levels (wood, stone, iron, diamond, netherite)
     * 100 = modded mining level (greater than netherite)
     */
    public void setMiningLevel(ItemStack itemStack, int miningLevel) {
        if ((miningLevel < 0 || miningLevel > 5) && miningLevel != 100) {
            return; // Invalid level
        }
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MINING_LEVEL_TAG, miningLevel);
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Get the mining level from the chip
     * @return mining level (0-6), defaults to 0 if not set
     */
    public int getMiningLevel(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(MINING_LEVEL_TAG)) {
            return 0; // Default: no filter
        }
        
        return tag.getInt(MINING_LEVEL_TAG);
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
        scannerTag.remove(TARGET_GEN_TAG);
        
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
        scannerTag.remove(TARGET_GEN_TAG);
        
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
     * Transfer the generic target from the chip to the scanner
     */
    private void transferGenericTargetToScanner(ItemStack chipStack, ItemStack scannerStack, ScannerItem scanner, Player player) {
        String genericTarget = getGenericTarget(chipStack);
        if (genericTarget == null) {
            return;
        }
        
        // Get the chip tag
        CompoundTag chipTag = chipStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Get the scanner tag
        CompoundTag scannerTag = scannerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove previous targets from the scanner
        scannerTag.remove(TARGET_BLOCK_TAG);
        scannerTag.remove(TARGET_MOB_TAG);
        
        // If it's ores, append mining level (ores -> ores0, ores1, etc.)
        String targetToSet = genericTarget;
        if ("ores".equals(genericTarget)) {
            int miningLevel = getMiningLevel(chipStack);
            targetToSet = "ores" + miningLevel;
        }
        
        // Set the new generic target
        scannerTag.putString(TARGET_GEN_TAG, targetToSet);
        
        // Ensure the scanner has a unique ID
        if (!scannerTag.contains("ScannerId")) {
            scannerTag.putUUID("ScannerId", java.util.UUID.randomUUID());
        }
        
        // Save the data in the scanner
        scannerStack.set(DataComponents.CUSTOM_DATA, CustomData.of(scannerTag));
        
        // Display appropriate message based on target type
        if ("ores".equals(genericTarget)) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success_ores"), true);
        } else if ("mobs".equals(genericTarget)) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success_all_mobs"), true);
        }
    }
    
    /**
     * Creates a localized name for a mob from its ID
     */
    private Component getLocalizedMobName(String mobId) {
        if (mobId == null) return Component.literal("Unknown");
        
        // Extract namespace and path from the ID
        String namespace = "minecraft";
        String path = mobId;
        
        if (mobId.contains(":")) {
            String[] parts = mobId.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        }
        
        // Try to use the specific translation key for the namespace
        String translationKey = "entity." + namespace + "." + path;
        Component translated = Component.translatable(translationKey);
        
        // If the namespace is not minecraft, add the namespace to the name if the translation fails
        if (!namespace.equals("minecraft")) {
            // Check if the translation was successful
            String translatedText = translated.getString();
            if (translatedText.equals(translationKey)) {
                // Translation failed, use an alternative format
                return Component.literal(namespace + ":" + path);
            }
        }
        
        return translated;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Check if this is a specialized chip (ores or mobs)
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemPath = itemId.getPath();
        boolean isOresChip = itemPath.contains("scanner_chip_ores");
        boolean isMobsChip = itemPath.contains("scanner_chip_mobs");
        
        // Target information
        Block targetBlock = getTargetBlock(stack);
        String targetMob = getTargetMob(stack);
        String genericTarget = getGenericTarget(stack);
        
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
        } else if (genericTarget != null) {
            if ("ores".equals(genericTarget)) {
                Component targetText = Component.translatable("item.iska_utils.scanner_chip.tooltip.target_ores_prefix")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA));
                
                // Get mining level
                int miningLevel = getMiningLevel(stack);
                
                if (miningLevel == 0) {
                    MutableComponent mutableTargetText = targetText.copy();
                    targetText = mutableTargetText.append(Component.translatable("item.iska_utils.scanner_chip.tooltip.target_ores_value")
                        .withStyle(ChatFormatting.WHITE));
                } else {
                    String levelText = switch (miningLevel) {
                        case 1 -> "item.iska_utils.scanner_chip.tooltip.mining_level.wood";
                        case 2 -> "item.iska_utils.scanner_chip.tooltip.mining_level.stone";
                        case 3 -> "item.iska_utils.scanner_chip.tooltip.mining_level.iron";
                        case 4 -> "item.iska_utils.scanner_chip.tooltip.mining_level.diamond";
                        case 5 -> "item.iska_utils.scanner_chip.tooltip.mining_level.netherite";
                        case 100 -> "item.iska_utils.scanner_chip.tooltip.mining_level.modded";
                        default -> "item.iska_utils.scanner_chip.tooltip.target_ores_value";
                    };
                    MutableComponent mutableTargetText = targetText.copy();
                    mutableTargetText = mutableTargetText.append(Component.translatable("item.iska_utils.scanner_chip.tooltip.target_ores_value")
                        .withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(levelText).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
                    targetText = mutableTargetText;
                }
                
                tooltipComponents.add(targetText);
            } else if ("mobs".equals(genericTarget)) {
                Component targetText = Component.translatable("item.iska_utils.scanner_chip.tooltip.target_all_mobs_prefix")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                        .append(Component.translatable("item.iska_utils.scanner_chip.tooltip.target_all_mobs_value")
                        .withStyle(ChatFormatting.WHITE));
                tooltipComponents.add(targetText);
            }
        } else {
            // For specialized chips, show default target even if not set yet
            if (isOresChip) {
                Component targetText = Component.translatable("item.iska_utils.scanner_chip.tooltip.target_ores_prefix")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA));
                
                // Get mining level
                int miningLevel = getMiningLevel(stack);
                
                if (miningLevel == 0) {
                    MutableComponent mutableTargetText = targetText.copy();
                    targetText = mutableTargetText.append(Component.translatable("item.iska_utils.scanner_chip.tooltip.target_ores_value")
                        .withStyle(ChatFormatting.WHITE));
                } else {
                    String levelText = switch (miningLevel) {
                        case 1 -> "item.iska_utils.scanner_chip.tooltip.mining_level.wood";
                        case 2 -> "item.iska_utils.scanner_chip.tooltip.mining_level.stone";
                        case 3 -> "item.iska_utils.scanner_chip.tooltip.mining_level.iron";
                        case 4 -> "item.iska_utils.scanner_chip.tooltip.mining_level.diamond";
                        case 5 -> "item.iska_utils.scanner_chip.tooltip.mining_level.netherite";
                        case 100 -> "item.iska_utils.scanner_chip.tooltip.mining_level.modded";
                        default -> "item.iska_utils.scanner_chip.tooltip.target_ores_value";
                    };
                    MutableComponent mutableTargetText = targetText.copy();
                    mutableTargetText = mutableTargetText.append(Component.translatable("item.iska_utils.scanner_chip.tooltip.target_ores_value")
                        .withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(levelText).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
                    targetText = mutableTargetText;
                }
                
                tooltipComponents.add(targetText);
            } else if (isMobsChip) {
                Component targetText = Component.translatable("item.iska_utils.scanner_chip.tooltip.target_all_mobs_prefix")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                        .append(Component.translatable("item.iska_utils.scanner_chip.tooltip.target_all_mobs_value")
                        .withStyle(ChatFormatting.WHITE));
                tooltipComponents.add(targetText);
            } else {
                Component noTargetText = Component.translatable("item.iska_utils.scanner_chip.tooltip.no_target")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY));
                
                tooltipComponents.add(noTargetText);
            }
        }
        
        // Instructions - only show for regular chips
        if (!isOresChip && !isMobsChip) {
            Component instruction0Text = Component.translatable("item.iska_utils.scanner_chip.tooltip.instruction0")
                    .withStyle(style -> style.withColor(ChatFormatting.YELLOW));
            tooltipComponents.add(instruction0Text);
            
            Component instruction1Text = Component.translatable("item.iska_utils.scanner_chip.tooltip.instruction1")
                    .withStyle(style -> style.withColor(ChatFormatting.YELLOW));
            tooltipComponents.add(instruction1Text);
        }
        
        // Add transfer instruction for all chips
        Component transferText = Component.translatable("item.iska_utils.scanner_chip.tooltip.transfer_instruction")
                .withStyle(style -> style.withColor(ChatFormatting.YELLOW));
        tooltipComponents.add(transferText);
        
        // Special chip information
        if (isOresChip) {
            Component chipTypeText = Component.translatable("item.iska_utils.scanner_chip.tooltip.ore_chip_desc")
                    .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE));
            tooltipComponents.add(chipTypeText);
            
            // Add mining level filter instruction
            Component miningLevelText = Component.translatable("item.iska_utils.scanner_chip.tooltip.ore_chip_mining_level")
                    .withStyle(style -> style.withColor(ChatFormatting.GRAY));
            tooltipComponents.add(miningLevelText);
        } else if (isMobsChip) {
            Component chipTypeText = Component.translatable("item.iska_utils.scanner_chip.tooltip.mob_chip_desc")
                    .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE));
            tooltipComponents.add(chipTypeText);
        }
    }
}