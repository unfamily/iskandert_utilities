package net.unfamily.iskautils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.unfamily.iskautils.entity.DeceptionSeatEntity;

/**
 * Sitting on {@link TheDeceptionBlock} via an invisible zero-size seat entity.
 */
public final class TheDeceptionSeatUtil {
    private static final String SEAT_POS_KEY = "iska_utils_deception_seat_pos";
    private static final String SEAT_START_TICK_KEY = "iska_utils_deception_sit_tick";

    /** Top surface of the {@code sit} element in block space (matches model y 7–9). */
    private static final double SEAT_HEIGHT = 9.0D / 16.0D;

    private TheDeceptionSeatUtil() {}

    public static boolean trySit(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || player.isPassenger() || isSitting(player)) {
            return false;
        }

        if (!DeceptionSeatEntity.availableAt(level, pos)) {
            return false;
        }

        Direction facing = state.getValue(TheDeceptionBlock.FACING);
        if (!fitsRider(level, player, pos.getY() + SEAT_HEIGHT)) {
            return false;
        }

        // Face away from the backrest (model back is on +Z; block FACING is the seated view direction).
        if (!DeceptionSeatEntity.sit(player, pos, SEAT_HEIGHT, facing)) {
            return false;
        }

        player.getPersistentData().putLong(SEAT_POS_KEY, pos.asLong());
        player.getPersistentData().putLong(SEAT_START_TICK_KEY, level.getGameTime());
        return true;
    }

    public static void tickSit(Player player) {
        if (player.level().isClientSide() || !isSitting(player)) {
            return;
        }

        Level level = player.level();
        BlockPos pos = getSitBlockPos(player);
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TheDeceptionBlock)) {
            stopSit(player);
            return;
        }

        if (!(player.getVehicle() instanceof DeceptionSeatEntity)) {
            clearSitTags(player);
            return;
        }

        long startTick = player.getPersistentData().getLong(SEAT_START_TICK_KEY);
        if (player.isShiftKeyDown() && level.getGameTime() > startTick + 1L) {
            player.stopRiding();
            clearSitTags(player);
            return;
        }

        if (!fitsRider(level, player, pos.getY() + SEAT_HEIGHT)) {
            player.stopRiding();
            clearSitTags(player);
        }
    }

    public static void stopSitAt(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        for (Player player : level.players()) {
            if (isSittingOn(player, pos)) {
                stopSit(player);
            }
        }
        removeSeats(level, pos);
    }

    public static void onDismount(DeceptionSeatEntity seat) {
        if (!seat.isVehicle()) {
            seat.discard();
        }
    }

    public static boolean isSitting(Player player) {
        return player.getPersistentData().contains(SEAT_POS_KEY);
    }

    public static boolean isSittingOn(Player player, BlockPos pos) {
        return isSitting(player) && getSitBlockPos(player).equals(pos);
    }

    private static void stopSit(Player player) {
        if (player.getVehicle() instanceof DeceptionSeatEntity seat) {
            player.stopRiding();
            onDismount(seat);
        }
        clearSitTags(player);
    }

    private static void clearSitTags(Player player) {
        player.getPersistentData().remove(SEAT_POS_KEY);
        player.getPersistentData().remove(SEAT_START_TICK_KEY);
    }

    private static BlockPos getSitBlockPos(Player player) {
        return BlockPos.of(player.getPersistentData().getLong(SEAT_POS_KEY));
    }

    private static void removeSeats(Level level, BlockPos pos) {
        for (DeceptionSeatEntity seat : level.getEntitiesOfClass(DeceptionSeatEntity.class, new AABB(pos))) {
            seat.ejectPassengers();
            seat.discard();
        }
    }

    private static boolean fitsRider(Level level, Player player, double seatY) {
        int minY = level.dimensionType().minY();
        int maxY = minY + level.dimensionType().height();
        double height = player.getBbHeight();
        return seatY >= minY && seatY + height <= maxY + 1.0E-4;
    }
}
