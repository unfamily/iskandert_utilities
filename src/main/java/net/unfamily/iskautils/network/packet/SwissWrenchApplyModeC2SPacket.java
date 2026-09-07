package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock.RotationMode;
import net.unfamily.iskautils.item.custom.SwissWrenchItem;
import net.unfamily.iskautils.util.SwissWrenchRotationApplier;

/**
 * C2S: apply a Swiss Wrench rotation mode to a block.
 * Legacy keybind path reads mode from the item (requires legacy config).
 * Radial arrow path sends an explicit quick rotate (LEFT/RIGHT only; no legacy config gate).
 */
public record SwissWrenchApplyModeC2SPacket(BlockPos pos, int modeOrdinal, boolean fromRadial)
        implements CustomPacketPayload {

    public static final Type<SwissWrenchApplyModeC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "swiss_wrench_apply_mode"));

    public static final StreamCodec<FriendlyByteBuf, SwissWrenchApplyModeC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SwissWrenchApplyModeC2SPacket::pos,
                    ByteBufCodecs.VAR_INT, SwissWrenchApplyModeC2SPacket::modeOrdinal,
                    ByteBufCodecs.BOOL, SwissWrenchApplyModeC2SPacket::fromRadial,
                    SwissWrenchApplyModeC2SPacket::new);

    /** Legacy R keybind: mode comes from item NBT on the server. */
    public static SwissWrenchApplyModeC2SPacket legacyFromItem(BlockPos pos) {
        return new SwissWrenchApplyModeC2SPacket(pos, 0, false);
    }

    /** Radial quick-rotate buttons: explicit LEFT/RIGHT. */
    public static SwissWrenchApplyModeC2SPacket radialQuick(BlockPos pos, RotationMode mode) {
        return new SwissWrenchApplyModeC2SPacket(pos, mode.ordinal(), true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SwissWrenchApplyModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof SwissWrenchItem)) {
                return;
            }

            RotationMode mode;
            if (packet.fromRadial()) {
                mode = modeFromOrdinal(packet.modeOrdinal());
                if (mode == null || !isRadialQuickMode(mode)) {
                    return;
                }
            } else {
                if (!Config.swissWrenchLegacyModes) {
                    return;
                }
                mode = SetWrenchDirectionBlock.getSelectedRotationMode(stack);
                if (mode == RotationMode.RADIAL) {
                    return;
                }
            }

            BlockPos pos = packet.pos();
            if (!player.level().isLoaded(pos)
                    || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }
            SwissWrenchRotationApplier.apply(player.level(), pos, player, mode);
        });
    }

    private static boolean isRadialQuickMode(RotationMode mode) {
        return mode == RotationMode.ROTATE_LEFT
                || mode == RotationMode.ROTATE_RIGHT;
    }

    private static RotationMode modeFromOrdinal(int ordinal) {
        RotationMode[] values = RotationMode.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }
}
