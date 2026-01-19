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
        this.rangeUp = Math.max(0, value); // Allow values beyond max (shown in blue in GUI)
        setChanged();
    }

    public int getRangeDown() { return rangeDown; }
    public void setRangeDown(int value) { 
        this.rangeDown = Math.max(0, value); // Allow values beyond max (shown in blue in GUI)
        setChanged();
    }

    public int getRangeRight() { return rangeRight; }
    public void setRangeRight(int value) { 
        this.rangeRight = Math.max(0, value); // Allow values beyond max (shown in blue in GUI)
        setChanged();
    }

    public int getRangeLeft() { return rangeLeft; }
    public void setRangeLeft(int value) { 
        this.rangeLeft = Math.max(0, value); // Allow values beyond max (shown in blue in GUI)
        setChanged();
    }

    public int getRangeFront() { return rangeFront; }
    public void setRangeFront(int value) { 
        this.rangeFront = Math.max(0, value); // Allow values beyond max (shown in blue in GUI)
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
    
    public int getRedstoneMode() { return redstoneMode; }
    public void setRedstoneMode(int value) { 
        this.redstoneMode = Math.max(0, Math.min(value, 4)); // 0-4 range
        setChanged();
    }
    
    public boolean isPull() { return isPull; }
    public void setPull(boolean value) { 
        this.isPull = value; 
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
        isPull = tag.contains("IsPull") ? tag.getBoolean("IsPull") : false; // Default: push
        
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
        
        // Save module handler
        tag.put("Modules", moduleHandler.serializeNBT(registries));
    }

    // Tick method to push entities
    public static void tick(Level level, BlockPos pos, BlockState state, FanBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        // Only push when powered
        if (!state.getValue(FanBlock.POWERED)) {
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
                pushEntity(entity, facing, blockEntity.fanPower);
            }
        }
    }

    // Calculate the AABB for the push area
    private static AABB calculatePushArea(BlockPos pos, Direction facing, FanBlockEntity blockEntity) {
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
                minX = x - leftOffset;
                maxX = x + rightOffset;
                minY = y - downOffset;
                maxY = y + upOffset;
                minZ = z - frontOffset;
                maxZ = z + backOffset;
            }
            case SOUTH -> {
                // Front is +Z, Left is +X, Right is -X
                minX = x - rightOffset;
                maxX = x + leftOffset;
                minY = y - downOffset;
                maxY = y + upOffset;
                minZ = z - backOffset;
                maxZ = z + frontOffset;
            }
            case WEST -> {
                // Front is -X, Left is +Z, Right is -Z
                minX = x - frontOffset;
                maxX = x + backOffset;
                minY = y - downOffset;
                maxY = y + upOffset;
                minZ = z - rightOffset;
                maxZ = z + leftOffset;
            }
            case EAST -> {
                // Front is +X, Left is -Z, Right is +Z
                minX = x - backOffset;
                maxX = x + frontOffset;
                minY = y - downOffset;
                maxY = y + upOffset;
                minZ = z - leftOffset;
                maxZ = z + rightOffset;
            }
            case UP -> {
                // Front is +Y, Left and Right depend on horizontal facing (use NORTH as default)
                minX = x - leftOffset;
                maxX = x + rightOffset;
                minY = y - backOffset;
                maxY = y + frontOffset;
                minZ = z - 0;
                maxZ = z + 0;
            }
            case DOWN -> {
                // Front is -Y, Left and Right depend on horizontal facing (use NORTH as default)
                minX = x - leftOffset;
                maxX = x + rightOffset;
                minY = y - frontOffset;
                maxY = y + backOffset;
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

    // Push an entity in the facing direction
    private static void pushEntity(Entity entity, Direction facing, double power) {
        Vec3 currentMotion = entity.getDeltaMovement();
        Vec3 pushVector = Vec3.ZERO;
        
        switch (facing) {
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
