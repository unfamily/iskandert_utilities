package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock;

/** C2S: cycle Swiss Wrench rotation mode (left-click in air). */
public record SwissWrenchCycleModeC2SPacket() implements CustomPacketPayload {

    public static final Type<SwissWrenchCycleModeC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "swiss_wrench_cycle_mode"));

    public static final StreamCodec<FriendlyByteBuf, SwissWrenchCycleModeC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new SwissWrenchCycleModeC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SwissWrenchCycleModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                SetWrenchDirectionBlock.tryCycleRotationMode(sp);
            }
        });
    }
}
