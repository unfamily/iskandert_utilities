package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.client.gui.StructurePlacerMachineMenu;
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.structure.StructureLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Block Entity for the Structure Placer Machine
 */
public class StructurePlacerMachineBlockEntity extends BlockEntity implements MenuProvider {
    
    private final ItemStackHandler itemHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
    // Energy storage for the machine (10,000 FE capacity, 100 FE/t input)
    private final EnergyStorage energyStorage = new EnergyStorage(10000, 100, 0);
    
    // Selected structure for this machine
    private String selectedStructure = "";
    
    // Preview mode toggle
    private boolean showPreview = false;
    
    // Structure rotation (0, 90, 180, 270 degrees)
    private int rotation = 0;
    
    // Redstone mode (0 = NONE, 1 = LOW, 2 = HIGH, 3 = PULSE)
    private int redstoneMode = 0;
    
    // Auto-placement counter for NONE mode (places every 60 ticks)
    private int autoPulseTimer = 0;
    private static final int AUTO_PULSE_INTERVAL = 60; // 3 seconds (60 ticks)
    
    /**
     * Enum for redstone modes
     */
    public enum RedstoneMode {
        NONE(0),    // Gunpowder icon
        LOW(1),     // Redstone dust icon  
        HIGH(2),    // Redstone gui icon
        PULSE(3);   // Repeater icon
        
        private final int value;
        
        RedstoneMode(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static RedstoneMode fromValue(int value) {
            for (RedstoneMode mode : values()) {
                if (mode.value == value) return mode;
            }
            return NONE;
        }
        
        public RedstoneMode next() {
            return switch (this) {
                case NONE -> LOW;
                case LOW -> HIGH;
                case HIGH -> PULSE;
                case PULSE -> NONE;
            };
        }
    }
    
    public StructurePlacerMachineBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.STRUCTURE_PLACER_MACHINE_BE.get(), pos, blockState);
        
        // Debug: Start with some energy for testing the energy bar
        this.energyStorage.receiveEnergy(5000, false); // Start with 5000/10000 FE (50%)
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, net.minecraft.core.HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (pkt.getTag() != null) {
            loadAdditional(pkt.getTag(), lookupProvider);
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putString("selectedStructure", selectedStructure);
        tag.putBoolean("showPreview", showPreview);
        tag.putInt("rotation", rotation);
        tag.putInt("redstoneMode", redstoneMode);
        tag.putInt("autoPulseTimer", autoPulseTimer);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("energy")) {
            energyStorage.deserializeNBT(registries, tag.get("energy"));
        }
        selectedStructure = tag.getString("selectedStructure");
        showPreview = tag.getBoolean("showPreview");
        rotation = tag.getInt("rotation");
        redstoneMode = tag.getInt("redstoneMode");
        autoPulseTimer = tag.getInt("autoPulseTimer");
    }
    
    /**
     * Drops all items when the block is broken
     */
    public void drops() {
        if (level != null) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 
                                       itemHandler.getStackInSlot(i));
            }
        }
    }
    
    // Getters and setters
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
    
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    public String getSelectedStructure() {
        return selectedStructure;
    }
    
    public void setSelectedStructure(String selectedStructure) {
        this.selectedStructure = selectedStructure;
        setChanged();
    }
    
    public boolean isShowPreview() {
        return showPreview;
    }
    
    public void setShowPreview(boolean showPreview) {
        this.showPreview = showPreview;
        setChanged();
    }
    
    public int getRotation() {
        return rotation;
    }
    
    public void setRotation(int rotation) {
        this.rotation = rotation % 360; // Ensure rotation is always 0-359
        setChanged();
    }
    
    public int getRedstoneMode() {
        return redstoneMode;
    }
    
    public void setRedstoneMode(int redstoneMode) {
        this.redstoneMode = redstoneMode % 4; // Ensure mode is always 0-3
        setChanged();
    }
    
    // Static tick method for server-side updates
    public static void tick(Level level, BlockPos pos, BlockState state, StructurePlacerMachineBlockEntity blockEntity) {
        if (level.isClientSide()) return;
        
        // Auto-placement logic for NONE mode
        if (blockEntity.redstoneMode == RedstoneMode.NONE.getValue()) {
            blockEntity.autoPulseTimer++;
            if (blockEntity.autoPulseTimer >= AUTO_PULSE_INTERVAL) {
                blockEntity.autoPulseTimer = 0; // Reset timer
                blockEntity.attemptAutoPlacement(level, pos);
            }
        } else {
            // Reset timer if not in NONE mode
            blockEntity.autoPulseTimer = 0;
        }
        
        // For testing: slowly drain energy (1 FE per second)
        if (level.getGameTime() % 20 == 0) { // Every second
            if (blockEntity.energyStorage.getEnergyStored() > 0) {
                blockEntity.energyStorage.extractEnergy(1, false);
                blockEntity.setChanged();
            }
        }
    }
    
    /**
     * Debug method to add energy for testing
     */
    public void addEnergyForTesting(int amount) {
        energyStorage.receiveEnergy(amount, false);
        setChanged();
    }
    
    // MenuProvider implementation
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.structure_placer_machine");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StructurePlacerMachineMenu(containerId, playerInventory, this);
    }
    
    /**
     * Attempts to place the selected structure using blocks from the internal inventory
     * Uses forced placement logic: places even if space is occupied but doesn't overwrite existing blocks
     */
    private void attemptAutoPlacement(Level level, BlockPos machinePos) {
        if (level.isClientSide()) return;
        
        // Check if we have a selected structure
        if (selectedStructure == null || selectedStructure.isEmpty()) {
            return;
        }
        
        // Get structure definition
        StructureDefinition structure = StructureLoader.getStructure(selectedStructure);
        if (structure == null) {
            return;
        }
        
        // Calculate structure positions (place above machine)
        BlockPos placementPos = machinePos.above();
        Map<BlockPos, String> blockPositions = calculateStructurePositions(placementPos, structure, rotation);
        Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
        
        if (blockPositions.isEmpty() || key == null) {
            return;
        }
        
        // Try to allocate blocks from inventory
        Map<BlockPos, AllocatedBlock> blockAllocation = new HashMap<>();
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            String character = entry.getValue();
            List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
            
            if (blockDefs != null && !blockDefs.isEmpty()) {
                AllocatedBlock allocated = allocateBlockFromInventory(blockDefs);
                if (allocated != null) {
                    blockAllocation.put(blockPos, allocated);
                }
            }
        }
        
        // Place allocated blocks with forced placement logic
        for (Map.Entry<BlockPos, AllocatedBlock> entry : blockAllocation.entrySet()) {
            BlockPos blockPos = entry.getKey();
            AllocatedBlock allocated = entry.getValue();
            
            // Check if position is occupied - if yes, skip but don't consume block
            BlockState currentState = level.getBlockState(blockPos);
            if (!currentState.isAir() && !currentState.canBeReplaced()) {
                // Return block to inventory since we couldn't place it
                restoreBlockToInventory(allocated);
                continue;
            }
            
            // Place the block
            placeSingleBlock((ServerLevel) level, blockPos, allocated, structure);
        }
    }
    
    /**
     * Helper class to hold allocated block information
     */
    private static class AllocatedBlock {
        private final StructureDefinition.BlockDefinition blockDefinition;
        private final int inventorySlot;
        private final ItemStack originalStack;
        
        public AllocatedBlock(StructureDefinition.BlockDefinition blockDefinition, int inventorySlot, ItemStack originalStack) {
            this.blockDefinition = blockDefinition;
            this.inventorySlot = inventorySlot;
            this.originalStack = originalStack;
        }
        
        public StructureDefinition.BlockDefinition getBlockDefinition() { return blockDefinition; }
        public int getInventorySlot() { return inventorySlot; }
        public ItemStack getOriginalStack() { return originalStack; }
    }
    
    /**
     * Calculates positions of all structure blocks with rotation
     */
    private Map<BlockPos, String> calculateStructurePositions(BlockPos centerPos, StructureDefinition structure, int rotation) {
        Map<BlockPos, String> positions = new HashMap<>();
        
        String[][][][] pattern = structure.getPattern();
        if (pattern == null) return positions;
        
        // Find structure center (@ symbol)
        BlockPos relativeCenter = structure.findCenter();
        if (relativeCenter == null) relativeCenter = BlockPos.ZERO;
        
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    
                    if (cellChars != null) {
                        for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                            String character = cellChars[charIndex];
                            
                            // Skip empty spaces
                            if (character == null || character.equals(" ")) continue;
                            
                            // If it's @, check if it's defined in the key
                            if (character.equals("@")) {
                                Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
                                if (key == null || !key.containsKey("@")) {
                                    // @ is not defined in key, treat as empty space
                                    continue;
                                }
                            }
                            
                            int originalX = x;
                            int originalY = y;
                            int originalZ = z * cellChars.length + charIndex;
                            
                            // Calculate relative position from center @
                            int relX = originalX - relativeCenter.getX();
                            int relY = originalY - relativeCenter.getY();
                            int relZ = originalZ - relativeCenter.getZ();
                            
                            // Apply rotation to relative coordinates
                            BlockPos rotatedRelativePos = applyRotation(relX, relY, relZ, rotation);
                            
                            // Calculate final position in world
                            BlockPos blockPos = centerPos.offset(rotatedRelativePos.getX(), rotatedRelativePos.getY(), rotatedRelativePos.getZ());
                            positions.put(blockPos, character);
                        }
                    }
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Applies rotation transformation to coordinates
     */
    private static BlockPos applyRotation(int x, int y, int z, int rotation) {
        return switch (rotation) {
            case 90 -> new BlockPos(-z, y, x);   // 90° clockwise
            case 180 -> new BlockPos(-x, y, -z); // 180°
            case 270 -> new BlockPos(z, y, -x);  // 270° clockwise (90° counter-clockwise)
            default -> new BlockPos(x, y, z);    // 0° (no rotation)
        };
    }
    
    /**
     * Tries to allocate a block from the inventory
     */
    private AllocatedBlock allocateBlockFromInventory(List<StructureDefinition.BlockDefinition> blockDefinitions) {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            
            if (!stack.isEmpty()) {
                // Get block from item if possible
                Block block = null;
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                
                if (stack.getItem() instanceof BlockItem blockItem) {
                    block = blockItem.getBlock();
                } else {
                    // Try to find a matching block for items that are not BlockItem
                    try {
                        ResourceLocation itemLocation = ResourceLocation.parse(itemId);
                        ResourceLocation blockLocation = 
                            ResourceLocation.fromNamespaceAndPath(itemLocation.getNamespace(), itemLocation.getPath());
                        
                        if (BuiltInRegistries.BLOCK.containsKey(blockLocation)) {
                            block = BuiltInRegistries.BLOCK.get(blockLocation);
                        }
                    } catch (Exception e) {
                        // Ignore if unable to find matching block
                    }
                }
                
                if (block != null) {
                    String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
                    
                    // Check if this block matches any of the required block definitions
                    for (StructureDefinition.BlockDefinition blockDef : blockDefinitions) {
                        if (blockDef.getBlock().equals(blockId)) {
                            // Extract one item from inventory
                            ItemStack extracted = itemHandler.extractItem(slot, 1, false);
                            if (!extracted.isEmpty()) {
                                return new AllocatedBlock(blockDef, slot, extracted);
                            }
                        }
                    }
                }
            }
        }
        
        return null; // No suitable block found
    }
    
    /**
     * Returns a block to inventory (when placement fails)
     */
    private void restoreBlockToInventory(AllocatedBlock allocated) {
        // Try to return to original slot first
        ItemStack remainder = itemHandler.insertItem(allocated.getInventorySlot(), allocated.getOriginalStack(), false);
        
        // If original slot is full, try other slots
        if (!remainder.isEmpty()) {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                remainder = itemHandler.insertItem(slot, remainder, false);
                if (remainder.isEmpty()) break;
            }
        }
        
        // If still can't fit, drop in world (shouldn't happen normally)
        if (!remainder.isEmpty() && level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
        }
    }
    
    /**
     * Places a single block with structure settings
     */
    private void placeSingleBlock(ServerLevel level, BlockPos blockPos, AllocatedBlock allocated, StructureDefinition structure) {
        StructureDefinition.BlockDefinition blockDef = allocated.getBlockDefinition();
        
        // Get block from registry
        ResourceLocation blockLocation = ResourceLocation.parse(blockDef.getBlock());
        Block block = BuiltInRegistries.BLOCK.get(blockLocation);
        
        if (block != null && block != Blocks.AIR) {
            BlockState blockState = block.defaultBlockState();
            
            // Apply properties if specified
            if (blockDef.getProperties() != null) {
                for (Map.Entry<String, String> propEntry : blockDef.getProperties().entrySet()) {
                    try {
                        blockState = applyBlockProperty(blockState, propEntry.getKey(), propEntry.getValue());
                    } catch (Exception e) {
                        // Ignore invalid properties
                    }
                }
            }
            
            // Check if we should place as player (fake player placement)
            if (structure.isPlaceAsPlayer()) {
                try {
                    Item blockItem = block.asItem();
                    if (blockItem != null && blockItem != Items.AIR) {
                        // Create a fake UseOnContext - we don't have a real player, so use null
                        var context = new UseOnContext(
                            null, // No player available 
                            InteractionHand.MAIN_HAND, 
                            new BlockHitResult(
                                Vec3.atCenterOf(blockPos), 
                                net.minecraft.core.Direction.UP, 
                                blockPos, 
                                false
                            )
                        );
                        
                        // Try to use BlockItem.place
                        if (blockItem instanceof BlockItem blockItemInstance) {
                            // Create placement context without player
                            BlockPlaceContext placeContext = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, 
                                allocated.getOriginalStack(), new BlockHitResult(Vec3.atCenterOf(blockPos), 
                                net.minecraft.core.Direction.UP, blockPos, false));
                            
                            blockItemInstance.place(placeContext);
                        } else {
                            // Fallback to normal placement
                            level.setBlock(blockPos, blockState, 3);
                        }
                    } else {
                        // Fallback to normal placement
                        level.setBlock(blockPos, blockState, 3);
                    }
                } catch (Exception e) {
                    // If player-like placement fails, fallback to normal placement
                    level.setBlock(blockPos, blockState, 3);
                }
            } else {
                // Normal placement
                level.setBlock(blockPos, blockState, 3);
            }
        }
    }
    
    /**
     * Applies a block property with the given name and value
     */
    private BlockState applyBlockProperty(BlockState state, String propertyName, String value) {
        for (var property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return applyPropertyValue(state, property, value);
            }
        }
        return state;
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> BlockState applyPropertyValue(BlockState state, 
                                                                   net.minecraft.world.level.block.state.properties.Property<T> property, 
                                                                   String value) {
        try {
            var optionalValue = property.getValue(value);
            if (optionalValue.isPresent()) {
                return state.setValue(property, optionalValue.get());
            }
        } catch (Exception e) {
            // Ignore conversion errors
        }
        return state;
    }
} 