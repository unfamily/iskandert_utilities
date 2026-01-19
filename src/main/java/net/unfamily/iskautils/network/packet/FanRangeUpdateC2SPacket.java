package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.FanBlockEntity;

/**
 * Packet to update fan range parameters
 */
public class FanRangeUpdateC2SPacket {
    
    public enum RangeType {
        FORWARD(0),
        UP(1),
        DOWN(2),
        LEFT(3),
        RIGHT(4);
        
        private final int id;
        
        RangeType(int id) {
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
        
        public static RangeType fromId(int id) {
            return switch (id) {
                case 0 -> FORWARD;
                case 1 -> UP;
                case 2 -> DOWN;
                case 3 -> LEFT;
                case 4 -> RIGHT;
                default -> FORWARD;
            };
        }
    }
    
    private final BlockPos pos;
    private final RangeType rangeType;
    private final int delta;
    
    public FanRangeUpdateC2SPacket(BlockPos pos, RangeType rangeType, int delta) {
        this.pos = pos;
        this.rangeType = rangeType;
        this.delta = delta;
    }
    
    /**
     * Handles the packet on the server
     */
    public void handle(ServerPlayer player) {
        if (player == null || player.level() == null) return;
        
        // Get the BlockEntity
        var blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof FanBlockEntity fan)) return;
        
        // Get current value and calculate new value
        int currentValue = switch (rangeType) {
            case FORWARD -> fan.getRangeFront();
            case UP -> fan.getRangeUp();
            case DOWN -> fan.getRangeDown();
            case LEFT -> fan.getRangeLeft();
            case RIGHT -> fan.getRangeRight();
        };
        
        // Calculate new value
        int newValue = currentValue + delta;
        
        // Calculate effective max range (base + installed range modules)
        int rangeModules = fan.countRangeModules();
        int maxValue = switch (rangeType) {
            case FORWARD -> {
                // Front range is always horizontal range * 2 to maintain cube shape
                int effectiveHorizontalMax = Config.fanRangeHorizontalMax + rangeModules;
                yield effectiveHorizontalMax * 2;
            }
            case UP, DOWN -> Config.fanRangeVerticalMax + rangeModules;
            case LEFT, RIGHT -> Config.fanRangeHorizontalMax + rangeModules;
        };
        
        // Apply limits: minimum is 0, if exceeds effective max, set to effective max
        if (newValue < 0) {
            newValue = 0;
        } else if (newValue > maxValue) {
            newValue = maxValue;
        }
        
        // Set the new value
        switch (rangeType) {
            case FORWARD -> fan.setRangeFront(newValue);
            case UP -> fan.setRangeUp(newValue);
            case DOWN -> fan.setRangeDown(newValue);
            case LEFT -> fan.setRangeLeft(newValue);
            case RIGHT -> fan.setRangeRight(newValue);
        }
        
        // Mark the BlockEntity as changed
        fan.setChanged();
        
        // Update the client
        player.level().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
    }
}
