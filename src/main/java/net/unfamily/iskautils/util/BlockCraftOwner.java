package net.unfamily.iskautils.util;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Persists the player who first opened a machine GUI; used for stage/mod gates on automated craft.
 */
public final class BlockCraftOwner {
    public static final String NBT_OWNER = "ownerPlayer";

    private BlockCraftOwner() {}

    @Nullable
    public static UUID read(CompoundTag tag) {
        if (tag.hasUUID(NBT_OWNER)) {
            return tag.getUUID(NBT_OWNER);
        }
        return null;
    }

    public static void write(CompoundTag tag, @Nullable UUID owner) {
        if (owner != null) {
            tag.putUUID(NBT_OWNER, owner);
        } else {
            tag.remove(NBT_OWNER);
        }
    }

    public static void claimOnFirstOpen(BlockEntity be, @Nullable ServerPlayer player, @Nullable UUID currentOwner) {
        if (be == null || player == null || currentOwner != null) {
            return;
        }
        if (be instanceof BlockCraftOwnerHolder holder) {
            holder.setOwnerPlayerUuid(player.getUUID());
            be.setChanged();
        }
    }

    public static void clear(BlockEntity be) {
        if (be instanceof BlockCraftOwnerHolder holder) {
            holder.setOwnerPlayerUuid(null);
            be.setChanged();
        }
    }

    @Nullable
    public static ServerPlayer resolve(@Nullable Level level, @Nullable UUID ownerUuid) {
        if (level == null || ownerUuid == null || level.isClientSide()) {
            return null;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    public interface BlockCraftOwnerHolder {
        @Nullable
        UUID getOwnerPlayerUuid();

        void setOwnerPlayerUuid(@Nullable UUID uuid);
    }
}
