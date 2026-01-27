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
        
        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case 0 -> Config.fanRangeUpgradeMax; // Slot 0: range module limit from config
                case 1 -> 1; // Slot 1: ghost module limited to 1
                case 2 -> Config.fanAccelerationUpgradeMax; // Slot 2: acceleration module limit from config
                default -> super.getSlotLimit(slot);
            };
        }
        
    };
    
    // Count how many range modules are currently installed
    public int countRangeModules() {
        int count = 0;
        for (int i = 0; i < moduleHandler.getSlots(); i++) {
            ItemStack stack = moduleHandler.getStackInSlot(i);
            if (stack.is(ModItems.RANGE_MODULE.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    // Count how many acceleration modules are currently installed
    public int countAccelerationModules() {
        int count = 0;
        for (int i = 0; i < moduleHandler.getSlots(); i++) {
            ItemStack stack = moduleHandler.getStackInSlot(i);
            if (isSpeedModule(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    // Get the effective maximum range (base + range modules)
    private int getEffectiveMaxRange(int baseMax) {
        int rangeModules = countRangeModules();
        return baseMax + rangeModules;
    }
    
    // Get the absolute maximum range (base + max upgrade limit from config)
    private int getAbsoluteMaxRange(int baseMax) {
        return baseMax + Config.fanRangeUpgradeMax;
    }
    
    // Check if item is a speed module
    private boolean isSpeedModule(ItemStack stack) {
        // Check if it's any of the speed module items
        return stack.is(ModItems.SLOW_MODULE.get()) ||
               stack.is(ModItems.MODERATE_MODULE.get()) ||
               stack.is(ModItems.FAST_MODULE.get()) ||
               stack.is(ModItems.EXTREME_MODULE.get()) ||
               stack.is(ModItems.ULTRA_MODULE.get());
    }
    
    // Get the power value for a speed module item
    private double getModulePower(ItemStack stack) {
        if (stack.is(ModItems.SLOW_MODULE.get())) {
            return Config.fanAccelerationModulePowers.size() > 0 ? Config.fanAccelerationModulePowers.get(0) : 0.1D;
        } else if (stack.is(ModItems.MODERATE_MODULE.get())) {
            return Config.fanAccelerationModulePowers.size() > 1 ? Config.fanAccelerationModulePowers.get(1) : 0.5D;
        } else if (stack.is(ModItems.FAST_MODULE.get())) {
            return Config.fanAccelerationModulePowers.size() > 2 ? Config.fanAccelerationModulePowers.get(2) : 1.0D;
        } else if (stack.is(ModItems.EXTREME_MODULE.get())) {
            return Config.fanAccelerationModulePowers.size() > 3 ? Config.fanAccelerationModulePowers.get(3) : 5.0D;
        } else if (stack.is(ModItems.ULTRA_MODULE.get())) {
            return Config.fanAccelerationModulePowers.size() > 4 ? Config.fanAccelerationModulePowers.get(4) : 15.0D;
        }
        return 0.0D;
    }
    
    // Calculate effective power (base + sum of acceleration module powers)
    public double getEffectivePower() {
        double totalPower = Config.fanDefaultPower;
        ItemStack slot2Stack = moduleHandler.getStackInSlot(2);
        if (!slot2Stack.isEmpty() && isSpeedModule(slot2Stack)) {
            double modulePower = getModulePower(slot2Stack);
            totalPower += modulePower * slot2Stack.getCount();
        }
        return totalPower;
    }
    
    // Get maximum possible power (base + max acceleration modules * max power)
    private double getMaxPossiblePower() {
        double maxModulePower = Config.fanAccelerationModulePowers.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0D);
        return Config.fanDefaultPower + (Config.fanAccelerationUpgradeMax * maxModulePower);
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
        } else {
            int effectiveMax = getEffectiveMaxRange(Config.fanRangeVerticalMax);
            if (value > effectiveMax) {
                this.rangeUp = effectiveMax;
            } else {
                this.rangeUp = value;
            }
        }
        setChanged();
    }

    public int getRangeDown() { return rangeDown; }
    public void setRangeDown(int value) { 
        if (value < 0) {
            this.rangeDown = 0;
        } else {
            int effectiveMax = getEffectiveMaxRange(Config.fanRangeVerticalMax);
            if (value > effectiveMax) {
                this.rangeDown = effectiveMax;
            } else {
                this.rangeDown = value;
            }
        }
        setChanged();
    }

    public int getRangeRight() { return rangeRight; }
    public void setRangeRight(int value) { 
        if (value < 0) {
            this.rangeRight = 0;
        } else {
            int effectiveMax = getEffectiveMaxRange(Config.fanRangeHorizontalMax);
            if (value > effectiveMax) {
                this.rangeRight = effectiveMax;
            } else {
                this.rangeRight = value;
            }
        }
        // Update front range to maintain cube (front = horizontal * 2 + 1)
        int effectiveHorizontalMax = getEffectiveMaxRange(Config.fanRangeHorizontalMax);
        int newFrontMax = effectiveHorizontalMax * 2 + 1;
        if (this.rangeFront > newFrontMax) {
            this.rangeFront = newFrontMax;
        }
        setChanged();
    }

    public int getRangeLeft() { return rangeLeft; }
    public void setRangeLeft(int value) { 
        if (value < 0) {
            this.rangeLeft = 0;
        } else {
            int effectiveMax = getEffectiveMaxRange(Config.fanRangeHorizontalMax);
            if (value > effectiveMax) {
                this.rangeLeft = effectiveMax;
            } else {
                this.rangeLeft = value;
            }
        }
        // Update front range to maintain cube (front = horizontal * 2 + 1)
        int effectiveHorizontalMax = getEffectiveMaxRange(Config.fanRangeHorizontalMax);
        int newFrontMax = effectiveHorizontalMax * 2 + 1;
        if (this.rangeFront > newFrontMax) {
            this.rangeFront = newFrontMax;
        }
        setChanged();
    }

    public int getRangeFront() { return rangeFront; }
    public void setRangeFront(int value) { 
        if (value < 0) {
            this.rangeFront = 0;
        } else {
            // Front range is always horizontal range * 2 + 1 to maintain cube shape
            int effectiveHorizontalMax = getEffectiveMaxRange(Config.fanRangeHorizontalMax);
            int effectiveFrontMax = effectiveHorizontalMax * 2 + 1;
            if (value > effectiveFrontMax) {
                this.rangeFront = effectiveFrontMax;
            } else {
                this.rangeFront = value;
            }
        }
        setChanged();
    }

    public double getFanPower() { return fanPower; }
    public void setFanPower(double value) { 
        // Validate against effective max power (base + installed acceleration modules)
        double effectiveMaxPower = getEffectivePower();
        // Also check against absolute max (base + max modules * max power)
        double absoluteMaxPower = getMaxPossiblePower();
        
        if (value < 0.0) {
            this.fanPower = 0.0;
        } else if (value > absoluteMaxPower) {
            this.fanPower = absoluteMaxPower;
        } else if (value > effectiveMaxPower) {
            this.fanPower = effectiveMaxPower;
        } else {
            this.fanPower = value;
        }
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
    
    // Validate and correct power based on installed acceleration modules
    private void validateAndCorrectPower() {
        double effectiveMaxPower = getEffectivePower();
        double absoluteMaxPower = getMaxPossiblePower();
        
        // Check and correct power value
        if (fanPower > absoluteMaxPower) {
            fanPower = absoluteMaxPower;
            setChanged();
        } else if (fanPower > effectiveMaxPower) {
            fanPower = effectiveMaxPower;
            setChanged();
        }
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

        // Validate and correct power based on installed acceleration modules
        blockEntity.validateAndCorrectPower();

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
        
        // Check if ghost module is installed (bypasses block obstruction)
        boolean hasGhostModule = !blockEntity.moduleHandler.getStackInSlot(1).isEmpty() && 
                                 blockEntity.moduleHandler.getStackInSlot(1).is(ModItems.GHOST_MODULE.get());
        
        // Push each entity based on push type (use effective power)
        double effectivePower = blockEntity.getEffectivePower();
        for (Entity entity : entities) {
            if (shouldPushEntity(entity, blockEntity.pushType)) {
                // Check if there's a blocking block between fan and entity
                if (!hasGhostModule && isBlockedByObstacle(level, pos, facing, entity, false)) {
                    continue; // Skip this entity if blocked and no ghost module
                } else if (hasGhostModule && isBlockedByObstacle(level, pos, facing, entity, true)) {
                    continue; // Skip this entity if blocked (even with ghost module, if config doesn't allow unbreakable bypass)
                }
                pushEntity(entity, facing, effectivePower, blockEntity.isPull);
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
                // Front is +Y, Left is -X, Right is +X (horizontal plane)
                // Area starts at +1 in front direction (y+1), not including fan position (y)
                minX = x - leftOffset;
                maxX = x + rightOffset;
                minY = y + 1; // Exclude fan position, start from y+1
                maxY = y + frontOffset;
                // Use left/right for Z axis when facing UP/DOWN (create horizontal area)
                minZ = z - leftOffset;
                maxZ = z + rightOffset;
            }
            case DOWN -> {
                // Front is -Y, Left is -X, Right is +X (horizontal plane)
                // Area starts at +1 in front direction (y-1), not including fan position (y)
                minX = x - leftOffset;
                maxX = x + rightOffset;
                minY = y - frontOffset;
                maxY = y - 1; // Exclude fan position, start from y-1
                // Use left/right for Z axis when facing UP/DOWN (create horizontal area)
                minZ = z - leftOffset;
                maxZ = z + rightOffset;
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

    // Check if there's a blocking obstacle between fan and entity
    private static boolean isBlockedByObstacle(Level level, BlockPos fanPos, Direction facing, Entity entity, boolean hasGhostModule) {
        // Get entity position (center)
        Vec3 entityPos = entity.position();
        Vec3 fanCenter = Vec3.atCenterOf(fanPos);
        
        // Calculate direction from fan to entity
        Vec3 direction = entityPos.subtract(fanCenter);
        
        // Project direction onto the facing axis to get the distance along that axis
        double distanceAlongFacing = switch (facing) {
            case NORTH -> fanCenter.z() - entityPos.z();
            case SOUTH -> entityPos.z() - fanCenter.z();
            case WEST -> fanCenter.x() - entityPos.x();
            case EAST -> entityPos.x() - fanCenter.x();
            case UP -> entityPos.y() - fanCenter.y();
            case DOWN -> fanCenter.y() - entityPos.y();
        };
        
        // Only check if entity is in front of the fan (positive distance)
        if (distanceAlongFacing <= 0) {
            return false; // Entity is behind or at same level, not blocked
        }
        
        // Check all blocks along the line from fan to entity in the facing direction
        // We check at integer block positions along the path
        int steps = (int) Math.ceil(distanceAlongFacing) + 1;
        Vec3 stepDirection = switch (facing) {
            case NORTH -> new Vec3(0, 0, -1);
            case SOUTH -> new Vec3(0, 0, 1);
            case WEST -> new Vec3(-1, 0, 0);
            case EAST -> new Vec3(1, 0, 0);
            case UP -> new Vec3(0, 1, 0);
            case DOWN -> new Vec3(0, -1, 0);
        };
        
        // Start from +1 block in front of fan (fan's own position doesn't block)
        for (int i = 1; i <= steps; i++) {
            Vec3 checkPos = fanCenter.add(stepDirection.scale(i));
            BlockPos blockPos = BlockPos.containing(checkPos);
            
            // Stop checking if we've passed the entity
            double checkDistance = switch (facing) {
                case NORTH -> fanCenter.z() - checkPos.z();
                case SOUTH -> checkPos.z() - fanCenter.z();
                case WEST -> fanCenter.x() - checkPos.x();
                case EAST -> checkPos.x() - fanCenter.x();
                case UP -> checkPos.y() - fanCenter.y();
                case DOWN -> fanCenter.y() - checkPos.y();
            };
            
            if (checkDistance > distanceAlongFacing) {
                break; // Past the entity, no need to check further
            }
            
            // Check if this block is solid and blocks the airflow
            BlockState blockState = level.getBlockState(blockPos);
            if (!blockState.isAir() && blockState.isSolid()) {
                // If ghost module is installed, we can bypass most blocks
                if (hasGhostModule) {
                    // Check if block is unbreakable (hardness < 0)
                    float destroySpeed = blockState.getDestroySpeed(level, blockPos);
                    if (destroySpeed < 0) {
                        // Unbreakable block - check if config allows bypass
                        if (Config.fanGhostModuleBypassUnbreakable) {
                            // Config allows bypass - continue checking
                            continue;
                        } else {
                            // Config doesn't allow bypass - block is unbreakable, so it blocks
                            return true;
                        }
                    }
                    // Block is solid but not unbreakable - ghost module allows bypass
                    continue;
                }
                // No ghost module - block is solid and blocks airflow
                return true;
            }
        }
        
        return false; // No blocking obstacles found
    }
    
    /**
     * Checks if ghost module is installed in this fan
     * @return true if ghost module is installed, false otherwise
     */
    public boolean hasGhostModule() {
        return !moduleHandler.getStackInSlot(1).isEmpty() && 
               moduleHandler.getStackInSlot(1).is(ModItems.GHOST_MODULE.get());
    }
    
    /**
     * Checks if a block is an obstacle (blocks airflow) considering ghost module
     * @param level The level
     * @param blockPos The block position to check
     * @param hasGhostModule Whether ghost module is installed
     * @return true if the block is an obstacle, false if it can be bypassed
     */
    public static boolean isBlockObstacle(Level level, BlockPos blockPos, boolean hasGhostModule) {
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.isAir() || !blockState.isSolid()) {
            return false; // Not an obstacle
        }
        
        // If ghost module is installed, we can bypass most blocks
        if (hasGhostModule) {
            // Check if block is unbreakable (hardness < 0)
            float destroySpeed = blockState.getDestroySpeed(level, blockPos);
            if (destroySpeed < 0) {
                // Unbreakable block - check if config allows bypass
                if (Config.fanGhostModuleBypassUnbreakable) {
                    // Config allows bypass - not an obstacle
                    return false;
                } else {
                    // Config doesn't allow bypass - block is unbreakable, so it's an obstacle
                    return true;
                }
            }
            // Block is solid but not unbreakable - ghost module allows bypass, not an obstacle
            return false;
        }
        
        // No ghost module - solid blocks are obstacles
        return true;
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
