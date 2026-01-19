package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.FanBlock;
import net.unfamily.iskautils.item.ModItems;

public class FanBlockEntity extends BlockEntity implements MenuProvider {
    // Enum for push type
    public enum PushType {
        MOBS_ONLY(0, "mobs_only"),
        MOBS_AND_PLAYERS(1, "mobs_and_players"),
        PLAYERS_ONLY(2, "players_only");
        
        private final int id;
        private final String name;
        
        PushType(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        
        public static PushType fromId(int id) {
            for (PushType type : values()) {
                if (type.id == id) return type;
            }
            return MOBS_ONLY; // Default
        }
        
        public static PushType fromName(String name) {
            for (PushType type : values()) {
                if (type.name.equals(name)) return type;
            }
            return MOBS_ONLY; // Default
        }
    }
    
    // Range parameters (stored in NBT)
    private int rangeUp = 0;
    private int rangeDown = 0;
    private int rangeRight = 0;
    private int rangeLeft = 0;
    private int rangeFront = 5;
    private double fanPower = 0.3;
    private PushType pushType = PushType.MOBS_ONLY; // Default: only mobs
    private int redstoneMode = 0; // Redstone mode (0=NONE, 1=LOW, 2=HIGH, 3=PULSE, 4=DISABLED)
    private boolean isPull = false; // false = push, true = pull
    // Redstone pulse mode tracking
    private boolean previousRedstoneState = false; // For PULSE mode
    private int pulseIgnoreTimer = 0; // Timer to ignore redstone after pulse
    private static final int PULSE_IGNORE_INTERVAL = 10; // Ignore pulses for 0.5 seconds after activation
    // GUI access tracking
    private boolean hasShownBackMessage = false; // Track if warning message has been shown
    private int backMessageTimer = 0; // Timer to reset hasShownBackMessage after 3 seconds (60 ticks)
    private static final int BACK_MESSAGE_RESET_INTERVAL = 60; // 3 seconds
    
    // Module slots (3 slots for upgrades)
    private final ItemStackHandler moduleHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Each slot accepts only its specific module type
            return switch (slot) {
                case 0 -> stack.is(ModItems.RANGE_MODULE.get()); // Slot 0: only range_module
                case 1 -> stack.is(ModItems.GHOST_MODULE.get()); // Slot 1: only ghost_module
                case 2 -> isSpeedModule(stack); // Slot 2: only speed modules
                default -> false;
            };
        }
    };
    
    // Check if item is a speed module
    private boolean isSpeedModule(ItemStack stack) {
        // Check if it's any of the speed module items
        return stack.is(ModItems.SLOW_MODULE.get()) ||
               stack.is(ModItems.MODERATE_MODULE.get()) ||
               stack.is(ModItems.FAST_MODULE.get()) ||
               stack.is(ModItems.EXTREME_MODULE.get()) ||
               stack.is(ModItems.ULTRA_MODULE.get());
    }

    public FanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FAN_BE.get(), pos, state);
    }
    
    public ItemStackHandler getModuleHandler() {
        return moduleHandler;
    }
    
    // MenuProvider implementation
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.fan");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new net.unfamily.iskautils.client.gui.FanMenu(containerId, playerInventory, this);
    }

    // Getters and setters for range parameters
    public int getRangeUp() { return rangeUp; }
    public void setRangeUp(int value) { 
        if (value < 0) {
            this.rangeUp = 0;
        } else if (value > Config.fanRangeVerticalMax) {
            this.rangeUp = Config.fanRangeVerticalMax;
        } else {
            this.rangeUp = value;
        }
        setChanged();
    }

    public int getRangeDown() { return rangeDown; }
    public void setRangeDown(int value) { 
        if (value < 0) {
            this.rangeDown = 0;
        } else if (value > Config.fanRangeVerticalMax) {
            this.rangeDown = Config.fanRangeVerticalMax;
        } else {
            this.rangeDown = value;
        }
        setChanged();
    }

    public int getRangeRight() { return rangeRight; }
    public void setRangeRight(int value) { 
        if (value < 0) {
            this.rangeRight = 0;
        } else if (value > Config.fanRangeHorizontalMax) {
            this.rangeRight = Config.fanRangeHorizontalMax;
        } else {
            this.rangeRight = value;
        }
        setChanged();
    }

    public int getRangeLeft() { return rangeLeft; }
    public void setRangeLeft(int value) { 
        if (value < 0) {
            this.rangeLeft = 0;
        } else if (value > Config.fanRangeHorizontalMax) {
            this.rangeLeft = Config.fanRangeHorizontalMax;
        } else {
            this.rangeLeft = value;
        }
        setChanged();
    }

    public int getRangeFront() { return rangeFront; }
    public void setRangeFront(int value) { 
        if (value < 0) {
            this.rangeFront = 0;
        } else if (value > Config.fanRangeFrontMax) {
            this.rangeFront = Config.fanRangeFrontMax;
        } else {
            this.rangeFront = value;
        }
        setChanged();
    }

    public double getFanPower() { return fanPower; }
    public void setFanPower(double value) { 
        this.fanPower = Math.max(0.0, Math.min(value, 100.0)); 
        setChanged();
    }

    public PushType getPushType() { return pushType; }
    public void setPushType(PushType value) { 
        this.pushType = value != null ? value : PushType.MOBS_ONLY; 
        setChanged();
    }
    
    public int getRedstoneMode() { 
        // Ensure value is always in valid range
        return Math.max(0, Math.min(redstoneMode, 4)); 
    }
    
    public void setRedstoneMode(int value) { 
        // Ensure value is in valid range and skip PULSE mode (3)
        if (value == 3) {
            value = 4; // Convert PULSE to DISABLED
        }
        int newMode = Math.max(0, Math.min(value, 4)); // 0-4 range (excluding 3)
        if (this.redstoneMode != newMode) {
            this.redstoneMode = newMode;
            setChanged();
            // Force save to ensure value is persisted
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }
    
    public void cycleRedstoneMode() {
        // Cycle through 0->1->2->4->0 (skip PULSE mode 3)
        int nextMode = (this.redstoneMode + 1) % 5;
        if (nextMode == 3) { // Skip PULSE mode
            nextMode = 4;
        }
        setRedstoneMode(nextMode);
    }
    
    public boolean isPull() { return isPull; }
    public void setPull(boolean value) { 
        this.isPull = value; 
        setChanged();
    }
    
    public boolean hasShownBackMessage() { return hasShownBackMessage; }
    public void setHasShownBackMessage(boolean value) { 
        this.hasShownBackMessage = value;
        if (value) {
            // Start timer when message is shown
            this.backMessageTimer = BACK_MESSAGE_RESET_INTERVAL;
        } else {
            // Reset timer when flag is cleared
            this.backMessageTimer = 0;
        }
        setChanged();
    }
    
    public void resetBackMessage() {
        this.hasShownBackMessage = false;
        this.backMessageTimer = 0;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rangeUp = tag.getInt("RangeUp");
        rangeDown = tag.getInt("RangeDown");
        rangeRight = tag.getInt("RangeRight");
        rangeLeft = tag.getInt("RangeLeft");
        rangeFront = tag.contains("RangeFront") ? tag.getInt("RangeFront") : 5; // Default 5 (inspired by vanilla water)
        fanPower = tag.contains("FanPower") ? tag.getDouble("FanPower") : 0.3; // Default 0.3
        pushType = tag.contains("PushType") ? PushType.fromId(tag.getInt("PushType")) : PushType.MOBS_ONLY; // Default: only mobs
        redstoneMode = tag.contains("RedstoneMode") ? tag.getInt("RedstoneMode") : 0; // Default: NONE
        // Ensure redstoneMode is valid (skip PULSE mode 3)
        if (redstoneMode == 3) {
            redstoneMode = 4; // Convert old PULSE mode to DISABLED
        }
        isPull = tag.contains("IsPull") ? tag.getBoolean("IsPull") : false; // Default: push
        previousRedstoneState = tag.contains("PreviousRedstoneState") ? tag.getBoolean("PreviousRedstoneState") : false;
        pulseIgnoreTimer = tag.contains("PulseIgnoreTimer") ? tag.getInt("PulseIgnoreTimer") : 0;
        hasShownBackMessage = tag.contains("HasShownBackMessage") ? tag.getBoolean("HasShownBackMessage") : false;
        backMessageTimer = tag.contains("BackMessageTimer") ? tag.getInt("BackMessageTimer") : 0;
        
        // Load module handler
        if (tag.contains("Modules")) {
            moduleHandler.deserializeNBT(registries, tag.getCompound("Modules"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RangeUp", rangeUp);
        tag.putInt("RangeDown", rangeDown);
        tag.putInt("RangeRight", rangeRight);
        tag.putInt("RangeLeft", rangeLeft);
        tag.putInt("RangeFront", rangeFront);
        tag.putDouble("FanPower", fanPower);
        tag.putInt("PushType", pushType.getId());
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putBoolean("IsPull", isPull);
        tag.putBoolean("PreviousRedstoneState", previousRedstoneState);
        tag.putInt("PulseIgnoreTimer", pulseIgnoreTimer);
        tag.putBoolean("HasShownBackMessage", hasShownBackMessage);
        tag.putInt("BackMessageTimer", backMessageTimer);
        
        // Save module handler
        tag.put("Modules", moduleHandler.serializeNBT(registries));
    }

    // Tick method to push entities
    public static void tick(Level level, BlockPos pos, BlockState state, FanBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        // Get current redstone mode (ensure it's in valid range)
        int currentRedstoneMode = Math.max(0, Math.min(blockEntity.redstoneMode, 4));
        
        // Check redstone conditions based on redstone mode
        int redstonePower = level.getBestNeighborSignal(pos);
        boolean hasRedstoneSignal = redstonePower > 0;
        boolean shouldPush = false;
        
        switch (currentRedstoneMode) {
            case 0 -> { // NONE: Always active, ignore redstone
                shouldPush = true;
            }
            case 1 -> { // LOW: Only when redstone is OFF (low signal)
                shouldPush = !hasRedstoneSignal;
            }
            case 2 -> { // HIGH: Only when redstone is ON (high signal)
                shouldPush = hasRedstoneSignal;
            }
            case 4 -> { // DISABLED: Always disabled
                shouldPush = false;
            }
            default -> {
                // Fallback: if mode is invalid, default to NONE (always active)
                shouldPush = true;
            }
        }
        
        // Update POWERED state based on advanced redstone logic (for rendering)
        // This ensures visual state matches the actual logic, not primitive redstone
        boolean shouldBePowered = shouldPush;
        if (state.getValue(FanBlock.POWERED) != shouldBePowered) {
            level.setBlock(pos, state.setValue(FanBlock.POWERED, shouldBePowered), 3);
        }
        
        // If we shouldn't push, update state and return
        if (!shouldPush) {
            // Update previous state for PULSE mode even when not pushing
            if (currentRedstoneMode == 3) {
                blockEntity.previousRedstoneState = hasRedstoneSignal;
            }
            return;
        }

        // Get the facing direction
        Direction facing = state.getValue(FanBlock.FACING);
        
        // Calculate the AABB for the push area based on ranges
        AABB pushArea = calculatePushArea(pos, facing, blockEntity);
        
        // Get all entities in the area
        var entities = level.getEntitiesOfClass(Entity.class, pushArea, 
            entity -> entity instanceof LivingEntity && !entity.isSpectator());
        
        // Push each entity based on push type
        for (Entity entity : entities) {
            if (shouldPushEntity(entity, blockEntity.pushType)) {
                pushEntity(entity, facing, blockEntity.fanPower, blockEntity.isPull);
            }
        }
    }

    // Calculate the AABB for the push area
    public static AABB calculatePushArea(BlockPos pos, Direction facing, FanBlockEntity blockEntity) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        
        // Calculate offsets based on facing direction
        int frontOffset = blockEntity.rangeFront;
        int backOffset = 0; // No back range for now
        int leftOffset = blockEntity.rangeLeft;
        int rightOffset = blockEntity.rangeRight;
        int upOffset = blockEntity.rangeUp;
        int downOffset = blockEntity.rangeDown;
        
        // Convert relative directions to absolute coordinates based on facing
        int minX, maxX, minY, maxY, minZ, maxZ;
        
        switch (facing) {
            case NORTH -> {
                // Front is -Z, Left is -X, Right is +X
                // Area starts at +1 in front direction (z-1), not including fan position (z)
                minX = x - leftOffset;
                maxX = x + rightOffset;
                minY = y - downOffset;
                maxY = y + upOffset;
                minZ = z - frontOffset;
                maxZ = z - 1; // Exclude fan position, start from z-1
            }
            case SOUTH -> {
                // Front is +Z, Left is +X, Right is -X
                // Area starts at +1 in front direction (z+1), not including fan position (z)
                minX = x - rightOffset;
                maxX = x + leftOffset;
                minY = y - downOffset;
                maxY = y + upOffset;
                minZ = z + 1; // Exclude fan position, start from z+1
                maxZ = z + frontOffset;
            }
            case WEST -> {
                // Front is -X, Left is +Z, Right is -Z
                // Area starts at +1 in front direction (x-1), not including fan position (x)
                minX = x - frontOffset;
                maxX = x - 1; // Exclude fan position, start from x-1
                minY = y - downOffset;
                maxY = y + upOffset;
                minZ = z - rightOffset;
                maxZ = z + leftOffset;
            }
            case EAST -> {
                // Front is +X, Left is -Z, Right is +Z
                // Area starts at +1 in front direction (x+1), not including fan position (x)
                minX = x + 1; // Exclude fan position, start from x+1
                maxX = x + frontOffset;
                minY = y - downOffset;
                maxY = y + upOffset;
                minZ = z - leftOffset;
                maxZ = z + rightOffset;
            }
            case UP -> {
                // Front is +Y, Left and Right depend on horizontal facing (use NORTH as default)
                // Area starts at +1 in front direction (y+1), not including fan position (y)
                minX = x - leftOffset;
                maxX = x + rightOffset;
                minY = y + 1; // Exclude fan position, start from y+1
                maxY = y + frontOffset;
                minZ = z - 0;
                maxZ = z + 0;
            }
            case DOWN -> {
                // Front is -Y, Left and Right depend on horizontal facing (use NORTH as default)
                // Area starts at +1 in front direction (y-1), not including fan position (y)
                minX = x - leftOffset;
                maxX = x + rightOffset;
                minY = y - frontOffset;
                maxY = y - 1; // Exclude fan position, start from y-1
                minZ = z - 0;
                maxZ = z + 0;
            }
            default -> {
                minX = x;
                maxX = x;
                minY = y;
                maxY = y;
                minZ = z;
                maxZ = z;
            }
        }
        
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    // Check if entity should be pushed based on push type
    private static boolean shouldPushEntity(Entity entity, PushType pushType) {
        boolean isPlayer = entity instanceof Player;
        
        return switch (pushType) {
            case MOBS_ONLY -> !isPlayer; // Only non-player entities
            case MOBS_AND_PLAYERS -> true; // All entities
            case PLAYERS_ONLY -> isPlayer; // Only players
        };
    }

    // Push an entity in the facing direction (or pull if isPull is true)
    private static void pushEntity(Entity entity, Direction facing, double power, boolean isPull) {
        Vec3 currentMotion = entity.getDeltaMovement();
        Vec3 pushVector = Vec3.ZERO;
        
        // Determine direction: if pull, reverse the facing direction
        Direction effectiveDirection = isPull ? facing.getOpposite() : facing;
        
        switch (effectiveDirection) {
            case NORTH -> pushVector = new Vec3(0, 0, -power);
            case SOUTH -> pushVector = new Vec3(0, 0, power);
            case WEST -> pushVector = new Vec3(-power, 0, 0);
            case EAST -> pushVector = new Vec3(power, 0, 0);
            case UP -> pushVector = new Vec3(0, power, 0);
            case DOWN -> pushVector = new Vec3(0, -power, 0);
        }
        
        // Apply push with some acceleration factor (similar to vector blocks)
        double accelerationFactor = 0.3;
        Vec3 newMotion = currentMotion.scale(1.0 - accelerationFactor)
            .add(pushVector.scale(accelerationFactor));
        
        entity.setDeltaMovement(newMotion);
        entity.hurtMarked = true; // Mark for physics update
    }
}
