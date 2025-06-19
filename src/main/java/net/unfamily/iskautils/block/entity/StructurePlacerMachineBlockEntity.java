package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Block Entity for the Structure Placer Machine
 */
public class StructurePlacerMachineBlockEntity extends BlockEntity implements MenuProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StructurePlacerMachineBlockEntity.class);
    
    private final ItemStackHandler itemHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Get ghost filter from direct field access
            if (slot >= 0 && slot < ghostFilters.size()) {
                ItemStack ghostFilter = ghostFilters.get(slot);
                
                if (!ghostFilter.isEmpty()) {
                    // Only allow items that match the ghost filter
                    return ItemStack.isSameItemSameComponents(stack, ghostFilter);
                }
            }
            // No filter, allow any item
            return true;
        }
    };
    
    // Energy storage for the machine (configured capacity, max input, and max extract)
    private final EnergyStorage energyStorage = new EnergyStorage(
        net.unfamily.iskautils.Config.structurePlacerMachineEnergyBuffer, 
        net.unfamily.iskautils.Config.structurePlacerMachineEnergyBuffer, // Max input = max capacity
        net.unfamily.iskautils.Config.structurePlacerMachineEnergyBuffer  // Max extract = max capacity (allow full extraction)
    );
    
    // Auto-placement counter for NONE mode (places every 60 ticks)
    private int autoPulseTimer = 0;
    private static final int AUTO_PULSE_INTERVAL = 60; // 3 seconds (60 ticks)
    
    // Redstone state tracking for PULSE mode
    private boolean previousRedstoneState = false;
    private int pulseIgnoreTimer = 0; // Timer to ignore redstone after pulse placement
    
    // Player who placed this machine (for player-like placement)
    private UUID placedByPlayer = null;
    
    // Flag to force sync on next tick (after world reload)
    private boolean needsSync = false;
    
    // Structure data fields (direct storage)
    private List<ItemStack> ghostFilters = new ArrayList<>();
    private String selectedStructure = "";
    private boolean showPreview = false;
    private int rotation = 0;
    private int redstoneMode = 0;
    
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
        // Initialize ghost filters list with 27 empty slots
        for (int i = 0; i < 27; i++) {
            ghostFilters.add(ItemStack.EMPTY);
        }
    }
    
    // Data access methods
    
    
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
        int currentEnergy = energyStorage.getEnergyStored();
        tag.putInt("energy", currentEnergy);
        LOGGER.debug("Structure Placer Machine saving energy: {}", currentEnergy);
        tag.putInt("autoPulseTimer", autoPulseTimer);
        if (placedByPlayer != null) {
            tag.putUUID("placedByPlayer", placedByPlayer);
        }
        
        // Save redstone state tracking
        tag.putBoolean("previousRedstoneState", previousRedstoneState);
        tag.putInt("pulseIgnoreTimer", pulseIgnoreTimer);
        
        // Save structure data fields directly
        tag.putString("selectedStructure", selectedStructure);
        tag.putBoolean("showPreview", showPreview);
        tag.putInt("rotation", rotation);
        tag.putInt("redstoneMode", redstoneMode);
        
        // Save ghost filters manually to NBT using the same format as inventory
        CompoundTag ghostTag = new CompoundTag();
        int savedFilters = 0;
        for (int i = 0; i < ghostFilters.size(); i++) {
            ItemStack filter = ghostFilters.get(i);
            if (!filter.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                // Use ItemStack.saveOptional for proper serialization
                ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), filter)
                    .result().ifPresent(nbt -> itemTag.merge((CompoundTag) nbt));
                ghostTag.put("slot" + i, itemTag);
                savedFilters++;
            }
        }
        tag.put("ghostFilters", ghostTag);
    }
    

    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("energy")) {
            // Load energy directly from the saved int value
            int savedEnergy = tag.getInt("energy");
            // Set energy by extracting all and then receiving the saved amount
            energyStorage.extractEnergy(energyStorage.getEnergyStored(), false);
            energyStorage.receiveEnergy(savedEnergy, false);
            LOGGER.debug("Structure Placer Machine loaded energy: {}", savedEnergy);
        }
        autoPulseTimer = tag.getInt("autoPulseTimer");
        if (tag.hasUUID("placedByPlayer")) {
            placedByPlayer = tag.getUUID("placedByPlayer");
        }
        
        // Load redstone state tracking
        previousRedstoneState = tag.getBoolean("previousRedstoneState");
        pulseIgnoreTimer = tag.getInt("pulseIgnoreTimer");
        
        // Load structure data directly to fields
        this.selectedStructure = tag.getString("selectedStructure");
        this.showPreview = tag.getBoolean("showPreview");
        this.rotation = tag.getInt("rotation");
        this.redstoneMode = tag.getInt("redstoneMode");
        
        // Load ghost filters from NBT
        this.ghostFilters.clear();
        for (int i = 0; i < 27; i++) {
            this.ghostFilters.add(ItemStack.EMPTY);
        }
        
        if (tag.contains("ghostFilters")) {
            CompoundTag ghostTag = tag.getCompound("ghostFilters");
            int loadedFilters = 0;
            for (int i = 0; i < 27; i++) {
                String slotKey = "slot" + i;
                if (ghostTag.contains(slotKey)) {
                    CompoundTag itemTag = ghostTag.getCompound(slotKey);
                    // Use ItemStack.OPTIONAL_CODEC for proper deserialization
                    ItemStack filter = ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), itemTag)
                        .result().orElse(ItemStack.EMPTY);
                    this.ghostFilters.set(i, filter);
                    loadedFilters++;
                }
            }

        }
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
    
    public UUID getPlacedByPlayer() {
        return placedByPlayer;
    }
    
    public void setPlacedByPlayer(UUID placedByPlayer) {
        this.placedByPlayer = placedByPlayer;
        setChanged();
    }
    
    /**
     * Set Inventory functionality - saves ghost filters based on current inventory contents
     */
    public void setInventoryFilters() {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack currentStack = itemHandler.getStackInSlot(slot);
            if (!currentStack.isEmpty()) {
                // Save a copy of the item as ghost filter (with count 1)
                ItemStack filter = currentStack.copy();
                filter.setCount(1); // Set count to 1 for ghost filter
                ghostFilters.set(slot, filter);
            }
            // Empty slots remain empty (no filter)
        }
        setChanged();
    }

    /**
     * Clear all ghost filters from all slots (Shift+Click behavior)
     */
    public void clearAllFilters() {
        for (int i = 0; i < ghostFilters.size(); i++) {
            ghostFilters.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    /**
     * Clear ghost filters only from slots that don't currently have the matching item (Ctrl+Click behavior)
     */
    public void clearEmptyFilters() {
        for (int slot = 0; slot < ghostFilters.size(); slot++) {
            ItemStack filter = ghostFilters.get(slot);
            if (!filter.isEmpty()) {
                ItemStack currentStack = itemHandler.getStackInSlot(slot);
                
                // If slot is empty or contains different item, clear the filter
                if (currentStack.isEmpty() || !ItemStack.isSameItemSameComponents(currentStack, filter)) {
                    ghostFilters.set(slot, ItemStack.EMPTY);
                }
            }
        }
        setChanged();
    }

    /**
     * Get ghost filter for a specific slot (for GUI display)
     */
    public ItemStack getGhostFilter(int slot) {
        if (slot >= 0 && slot < ghostFilters.size()) {
            return ghostFilters.get(slot);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Check if a slot has a ghost filter
     */
    public boolean hasGhostFilter(int slot) {
        if (slot >= 0 && slot < ghostFilters.size()) {
            return !ghostFilters.get(slot).isEmpty();
        }
        return false;
    }
    
    // Static tick method for server-side updates
    public static void tick(Level level, BlockPos pos, BlockState state, StructurePlacerMachineBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return; // Only run on server side
        }
        
        // Force sync to client if needed (after world reload)
        if (blockEntity.needsSync) {
            blockEntity.needsSync = false;
            level.sendBlockUpdated(pos, state, state, 3);
        }
        
        // Get current redstone power level
        int redstonePower = level.getBestNeighborSignal(pos);
        boolean hasRedstoneSignal = redstonePower > 0;
        
        // Auto-placement logic based on redstone mode
        RedstoneMode mode = RedstoneMode.fromValue(blockEntity.getRedstoneMode());
        boolean shouldPlace = false;
        
        switch (mode) {
            case NONE -> {
                // Mode 0: Always active, ignore redstone
                blockEntity.autoPulseTimer++;
                if (blockEntity.autoPulseTimer >= AUTO_PULSE_INTERVAL) {
                    blockEntity.autoPulseTimer = 0; // Reset timer
                    shouldPlace = true;
                }
            }
            case LOW -> {
                // Mode 1: Only when redstone is OFF (low signal)
                if (!hasRedstoneSignal) {
                    blockEntity.autoPulseTimer++;
                    if (blockEntity.autoPulseTimer >= AUTO_PULSE_INTERVAL) {
                        blockEntity.autoPulseTimer = 0; // Reset timer
                        shouldPlace = true;
                    }
                } else {
                    // Reset timer when redstone is on
                    blockEntity.autoPulseTimer = 0;
                }
            }
            case HIGH -> {
                // Mode 2: Only when redstone is ON (high signal)
                if (hasRedstoneSignal) {
                    blockEntity.autoPulseTimer++;
                    if (blockEntity.autoPulseTimer >= AUTO_PULSE_INTERVAL) {
                        blockEntity.autoPulseTimer = 0; // Reset timer
                        shouldPlace = true;
                    }
                } else {
                    // Reset timer when redstone is off
                    blockEntity.autoPulseTimer = 0;
                }
            }
            case PULSE -> {
                // Mode 3: Only on redstone pulse (low to high transition)
                // Decrement ignore timer if active
                if (blockEntity.pulseIgnoreTimer > 0) {
                    blockEntity.pulseIgnoreTimer--;
                }
                
                // Check for low-to-high transition only if not ignoring pulses
                if (blockEntity.pulseIgnoreTimer == 0) {
                    if (hasRedstoneSignal && !blockEntity.previousRedstoneState) {
                        // Detected rising edge (pulse)
                        shouldPlace = true;
                        // Start ignore timer for 60 ticks
                        blockEntity.pulseIgnoreTimer = AUTO_PULSE_INTERVAL;
                    }
                }
                
                // Update previous state
                blockEntity.previousRedstoneState = hasRedstoneSignal;
                
                // Always reset auto timer in pulse mode (we don't use it)
                blockEntity.autoPulseTimer = 0;
            }
        }
        
        // Attempt placement if conditions are met
        if (shouldPlace) {
            blockEntity.attemptAutoPlacement(level, pos);
        }
        
        // Note: Energy is consumed only when blocks are actually placed,
        // not per tick. See placeSingleBlock() method for energy consumption logic.
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
        String selectedStructure = getSelectedStructure();
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
        Map<BlockPos, String> blockPositions = calculateStructurePositions(placementPos, structure, getRotation());
        Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
        
        if (blockPositions.isEmpty() || key == null) {
            return;
        }
        
        // Try to allocate blocks from inventory (simulate first)
        Map<BlockPos, AllocatedBlock> blockAllocation = new HashMap<>();
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            String character = entry.getValue();
            List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
            
            if (blockDefs != null && !blockDefs.isEmpty()) {
                // Check if position can be placed first
                BlockState currentState = level.getBlockState(blockPos);
                if (canReplaceBlock(currentState, structure)) {
                    // Only simulate allocation if position is actually placeable
                    AllocatedBlock simulated = simulateAllocateBlockFromInventory(blockDefs);
                    if (simulated != null) {
                        blockAllocation.put(blockPos, simulated);
                    }
                }
            }
        }
        
        // Place allocated blocks with forced placement logic
        if (structure.isSlower()) {
            // Use block-by-block placement with delays (like StructureMonouseItem)
            placeStructureWithBlockDelay((ServerLevel) level, blockAllocation, structure);
        } else {
            // Place all blocks immediately
            int placedBlocks = 0;
            for (Map.Entry<BlockPos, AllocatedBlock> entry : blockAllocation.entrySet()) {
                BlockPos blockPos = entry.getKey();
                AllocatedBlock simulated = entry.getValue();
                
                // Check if position is still available
                BlockState currentState = level.getBlockState(blockPos);
                if (canReplaceBlock(currentState, structure)) {
                    // Check if we have enough energy BEFORE consuming the item
                    int energyRequired = net.unfamily.iskautils.Config.structurePlacerMachineEnergyConsume;
                    if (energyRequired > 0 && energyStorage.getEnergyStored() < energyRequired) {
                        // Not enough energy, stop placing more blocks (don't consume items)
                        LOGGER.debug("Structure Placer Machine: Not enough energy to place block (need {}, have {})", 
                            energyRequired, energyStorage.getEnergyStored());
                        break;
                    }
                    
                    // Only consume the item if we have enough energy
                    if (consumeAllocatedBlock(simulated)) {
                        // Place the block with energy check (should always succeed now)
                        if (placeSingleBlock((ServerLevel) level, blockPos, simulated, structure)) {
                            placedBlocks++;
                        } else {
                            // This should not happen since we already checked energy
                            LOGGER.warn("Structure Placer Machine: Unexpected failure in placeSingleBlock after energy check");
                            break;
                        }
                    }
                }
                // Note: if we can't place or consume, we do nothing (no need to restore since we never consumed)
            }
            
            if (placedBlocks > 0) {
                LOGGER.info("Structure Placer Machine placed {} blocks", placedBlocks);
            }
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
     * Simulates allocating a block from inventory without actually consuming it
     */
    private AllocatedBlock simulateAllocateBlockFromInventory(List<StructureDefinition.BlockDefinition> blockDefinitions) {
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
                            // SIMULATE extraction (don't actually extract)
                            ItemStack simulated = itemHandler.extractItem(slot, 1, true); // true = simulate only
                            if (!simulated.isEmpty()) {
                                // Create simulated allocation with original stack
                                return new AllocatedBlock(blockDef, slot, simulated.copy());
                            }
                        }
                    }
                }
            }
        }
        
        return null; // No suitable block found
    }
    
    /**
     * Actually consumes an item from inventory for an already simulated allocation
     */
    private boolean consumeAllocatedBlock(AllocatedBlock simulated) {
        // Verify the item is still available in the expected slot
        ItemStack slotStack = itemHandler.getStackInSlot(simulated.getInventorySlot());
        if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, simulated.getOriginalStack())) {
            // Actually extract the item
            ItemStack extracted = itemHandler.extractItem(simulated.getInventorySlot(), 1, false);
            return !extracted.isEmpty();
        }
        return false; // Item no longer available
    }
    
    /**
     * Places a single block with structure settings and energy consumption
     */
    private boolean placeSingleBlock(ServerLevel level, BlockPos blockPos, AllocatedBlock allocated, StructureDefinition structure) {
        // Check if we have enough energy to place this block
        int energyRequired = net.unfamily.iskautils.Config.structurePlacerMachineEnergyConsume;
        LOGGER.debug("Structure Placer Machine energy requirement check: energyRequired={}, currentEnergy={}", 
            energyRequired, energyStorage.getEnergyStored());
        
        if (energyRequired > 0 && energyStorage.getEnergyStored() < energyRequired) {
            LOGGER.debug("Structure Placer Machine: Not enough energy to place block (need {}, have {})", 
                energyRequired, energyStorage.getEnergyStored());
            return false; // Not enough energy
        }
        
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
            
            // Check if we should place as player
            if (structure.isPlaceAsPlayer()) {
                // Place as if done by player - try to find the real player who placed this machine
                try {
                    Item blockItem = block.asItem();
                    if (blockItem != null && blockItem != Items.AIR) {
                        ItemStack blockStack = new ItemStack(blockItem);
                        
                        // Try to get the real player who placed this machine
                        Player realPlayer = null;
                        if (placedByPlayer != null) {
                            realPlayer = level.getPlayerByUUID(placedByPlayer);
                        }
                        
                        // Create UseOnContext with real player if available, null otherwise
                        var context = new UseOnContext(
                            realPlayer, // Use real player if available
                            InteractionHand.MAIN_HAND, 
                            new BlockHitResult(
                                Vec3.atCenterOf(blockPos), 
                                net.minecraft.core.Direction.UP, 
                                blockPos, 
                                false
                            )
                        );
                        
                        // Try to use BlockItem.place like the items do
                        if (blockItem instanceof BlockItem blockItemInstance) {
                            BlockPlaceContext placeContext = new BlockPlaceContext(context);
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
            
            // Consume energy after successful placement
            if (energyRequired > 0) {
                int energyBefore = energyStorage.getEnergyStored();
                energyStorage.extractEnergy(energyRequired, false);
                int energyAfter = energyStorage.getEnergyStored();
                LOGGER.debug("Structure Placer Machine consumed {} energy ({} -> {}) for placing block at {}", 
                    energyBefore - energyAfter, energyBefore, energyAfter, blockPos);
                setChanged(); // Mark block entity as changed for saving
            }
            
            return true; // Block placed successfully
        }
        
        return false; // Block was null or air, couldn't place
    }
    
    /**
     * Check if a block can be replaced by structure placement
     */
    private boolean canReplaceBlock(BlockState state, StructureDefinition structure) {
        // Air can always be replaced
        if (state.isAir()) return true;
        
        // Check structure can_replace list
        List<String> canReplace = structure.getCanReplace();
        if (canReplace != null) {
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            
            for (String replaceableId : canReplace) {
                if (replaceableId.equals(blockId)) {
                    return true;
                }
                
                // Handle special cases
                if (handleSpecialReplaceableCase(replaceableId, state)) {
                    return true;
                }
                
                // Handle tags with # prefix
                if (replaceableId.startsWith("#") && handleTagReplacement(replaceableId, state)) {
                    return true;
                }
            }
            
            return false; // If there's a can_replace list and block is not in list, it cannot be replaced
        }
        
        // If no can_replace list specified, use default Minecraft behavior
        return state.canBeReplaced();
    }
    
    /**
     * Handles special replacement cases like $replaceable, $fluids, etc.
     */
    private boolean handleSpecialReplaceableCase(String replaceableId, BlockState existingState) {
        return switch (replaceableId.toLowerCase()) {
            case "$replaceable" -> existingState.canBeReplaced();
            case "$fluids", "$fluid" -> existingState.getFluidState().isSource() || !existingState.getFluidState().isEmpty();
            case "$air" -> existingState.isAir();
            case "$water" -> existingState.is(net.minecraft.world.level.block.Blocks.WATER);
            case "$lava" -> existingState.is(net.minecraft.world.level.block.Blocks.LAVA);
            case "$plants", "$plant" -> existingState.is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES) || 
                                      existingState.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS) ||
                                      existingState.is(net.minecraft.tags.BlockTags.TALL_FLOWERS) ||
                                      existingState.is(net.minecraft.tags.BlockTags.SAPLINGS);
            case "$dirt" -> existingState.is(net.minecraft.tags.BlockTags.DIRT);
            case "$logs", "$log" -> existingState.is(net.minecraft.tags.BlockTags.LOGS);
            case "$leaves" -> existingState.is(net.minecraft.tags.BlockTags.LEAVES);
            case "$stone" -> existingState.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES) ||
                           existingState.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES);
            case "$ores", "$ore" -> existingState.is(net.minecraft.tags.BlockTags.COAL_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.IRON_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.GOLD_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.DIAMOND_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.EMERALD_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.LAPIS_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.REDSTONE_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.COPPER_ORES);
            default -> false;
        };
    }
    
    /**
     * Handles tag-based replacement with # prefix
     */
    private boolean handleTagReplacement(String tagId, BlockState existingState) {
        try {
            // Remove # prefix
            String cleanTagId = tagId.substring(1);
            
            // Parse as ResourceLocation
            net.minecraft.resources.ResourceLocation tagLocation = net.minecraft.resources.ResourceLocation.parse(cleanTagId);
            
            // Get tag from registry
            net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> blockTag = 
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tagLocation);
            
            // Check if block is in the tag
            return existingState.is(blockTag);
            
        } catch (Exception e) {
            LOGGER.warn("Invalid tag format: '{}' - {}", tagId, e.getMessage());
            return false;
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
    
    /**
     * Places the structure with 5 tick delay between each individual block (when slower is enabled)
     */
    private void placeStructureWithBlockDelay(ServerLevel level, Map<BlockPos, AllocatedBlock> blockAllocation, StructureDefinition structure) {
        java.util.List<Map.Entry<BlockPos, AllocatedBlock>> blockList = new java.util.ArrayList<>(blockAllocation.entrySet());
        
        // Place first block immediately
        if (!blockList.isEmpty()) {
            Map.Entry<BlockPos, AllocatedBlock> firstBlock = blockList.get(0);
            BlockPos blockPos = firstBlock.getKey();
            AllocatedBlock simulated = firstBlock.getValue();
            
            // Check if position is still available
            BlockState currentState = level.getBlockState(blockPos);
            if (canReplaceBlock(currentState, structure)) {
                // Check if we have enough energy BEFORE consuming the item
                int energyRequired = net.unfamily.iskautils.Config.structurePlacerMachineEnergyConsume;
                if (energyRequired > 0 && energyStorage.getEnergyStored() < energyRequired) {
                    // Not enough energy for first block, log message and stop (don't consume item)
                    LOGGER.warn("Structure Placer Machine: Not enough energy to place blocks (need {}, have {})", 
                        energyRequired, energyStorage.getEnergyStored());
                    return;
                }
                
                // Only consume the item if we have enough energy
                if (consumeAllocatedBlock(simulated)) {
                    // Place the block with energy check (should always succeed now)
                    if (!placeSingleBlock(level, blockPos, simulated, structure)) {
                        // This should not happen since we already checked energy
                        LOGGER.warn("Structure Placer Machine: Unexpected failure in placeSingleBlock after energy check");
                        return;
                    }
                }
            }
            
            // Schedule remaining blocks with 5 tick delay between each
            for (int i = 1; i < blockList.size(); i++) {
                final Map.Entry<BlockPos, AllocatedBlock> blockEntry = blockList.get(i);
                final int delayTicks = i * 5; // 5 ticks between each block
                
                // Schedule block placement after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(delayTicks * 50); // Convert ticks to milliseconds
                        level.getServer().execute(() -> {
                            BlockPos pos = blockEntry.getKey();
                            AllocatedBlock simulatedBlock = blockEntry.getValue();
                            
                            // Check if position is still available
                            BlockState currentBlockState = level.getBlockState(pos);
                            if (canReplaceBlock(currentBlockState, structure)) {
                                // Check if we have enough energy BEFORE consuming the item
                                int energyRequired = net.unfamily.iskautils.Config.structurePlacerMachineEnergyConsume;
                                if (energyRequired > 0 && energyStorage.getEnergyStored() < energyRequired) {
                                    // Not enough energy, log message but don't consume item
                                    LOGGER.warn("Structure Placer Machine: Not enough energy to place block at {} (need {}, have {})", 
                                        pos, energyRequired, energyStorage.getEnergyStored());
                                    return; // Stop placing more blocks
                                }
                                
                                // Only consume the item if we have enough energy
                                if (consumeAllocatedBlock(simulatedBlock)) {
                                    // Place the block with energy check (should always succeed now)
                                    if (!placeSingleBlock(level, pos, simulatedBlock, structure)) {
                                        // This should not happen since we already checked energy
                                        LOGGER.warn("Structure Placer Machine: Unexpected failure in placeSingleBlock after energy check at {}", pos);
                                    }
                                }
                            }
                            // Note: if we can't place or consume, we do nothing (no need to restore since we never consumed)
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
} 