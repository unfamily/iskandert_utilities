package net.unfamily.iskautils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible seat entity for {@link TheDeceptionBlock}.
 */
public final class TheDeceptionSeatUtil {
    private static final String SEAT_POS_KEY = "iska_utils_deception_seat_pos";

    /** Center of the {@code sit} element in block space (from {@code the_deception.json}). */
    private static final double SIT_CENTER_X = 8.0D / 16.0D;
    private static final double SIT_TOP_Y = 9.0D / 16.0D;
    private static final double SIT_CENTER_Z = 8.0D / 16.0D;

    private TheDeceptionSeatUtil() {}

    public static boolean trySit(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || player.isShiftKeyDown() || player.isPassenger()) {
            return false;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ArmorStand seat = findSeat(serverLevel, pos);
        if (seat != null && seat.isVehicle()) {
            return false;
        }
        if (seat == null) {
            seat = createSeat(serverLevel, pos, state);
        }

        float yaw = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
        Vec3 seatPos = seatPosition(pos);
        seat.setPos(seatPos);
        seat.setYRot(yaw);
        seat.yRotO = yaw;

        if (player.startRiding(seat)) {
            player.setYRot(yaw);
            player.setYBodyRot(yaw);
            player.setYHeadRot(yaw);
            return true;
        }
        return false;
    }

    public static void removeSeat(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        ArmorStand seat = findSeat(level, pos);
        if (seat != null) {
            seat.ejectPassengers();
            seat.discard();
        }
    }

    public static boolean isDeceptionSeat(ArmorStand stand) {
        return stand.getPersistentData().contains(SEAT_POS_KEY);
    }

    public static BlockPos getSeatBlockPos(ArmorStand stand) {
        return BlockPos.of(stand.getPersistentData().getLong(SEAT_POS_KEY).orElse(0L));
    }

    public static void discardIfEmpty(ArmorStand stand) {
        if (!stand.isVehicle()) {
            stand.discard();
        }
    }

    private static ArmorStand createSeat(ServerLevel level, BlockPos pos, BlockState state) {
        Vec3 seatPos = seatPosition(pos);
        float yaw = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
        ArmorStand seat = new ArmorStand(level, seatPos.x, seatPos.y, seatPos.z);
        seat.setPos(seatPos);
        seat.setYRot(yaw);
        seat.yRotO = yaw;
        configureSeat(seat);
        seat.getPersistentData().putLong(SEAT_POS_KEY, pos.asLong());
        level.addFreshEntity(seat);
        return seat;
    }

    private static void configureSeat(ArmorStand seat) {
        seat.setInvisible(true);
        seat.setNoGravity(true);
        seat.setSilent(true);
        seat.setInvulnerable(true);
        byte flags = (byte) (ArmorStand.CLIENT_FLAG_SMALL | ArmorStand.CLIENT_FLAG_NO_BASEPLATE);
        seat.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, flags);
    }

    private static Vec3 seatPosition(BlockPos pos) {
        return new Vec3(pos.getX() + SIT_CENTER_X, pos.getY() + SIT_TOP_Y, pos.getZ() + SIT_CENTER_Z);
    }

    private static ArmorStand findSeat(Level level, BlockPos pos) {
        AABB searchBox = new AABB(pos).inflate(0.25D);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, searchBox, TheDeceptionSeatUtil::isDeceptionSeat)) {
            if (getSeatBlockPos(stand).equals(pos)) {
                return stand;
            }
        }
        return null;
    }
}
