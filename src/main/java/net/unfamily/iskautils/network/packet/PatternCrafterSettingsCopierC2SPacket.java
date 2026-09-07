package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;
import net.unfamily.iskautils.integration.anotherdynamics.DeepDrawerSettingsCopierLogic;

/**
 * Client-to-server: copy/paste Pattern Crafter Forbidden Outputs via Another Dynamics Settings Copier.
 */
public record PatternCrafterSettingsCopierC2SPacket(BlockPos pos, int action)
        implements CustomPacketPayload {

    public static final int ACTION_COPY = 0;
    public static final int ACTION_PASTE = 1;

    public static final Type<PatternCrafterSettingsCopierC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_settings_copier"));

    public static final StreamCodec<FriendlyByteBuf, PatternCrafterSettingsCopierC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos());
                buf.writeVarInt(p.action());
            },
            buf -> new PatternCrafterSettingsCopierC2SPacket(
                    BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternCrafterSettingsCopierC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof ImprovedPatternCrafterMenu menu)) {
                return;
            }
            if (menu.getBlockEntity() == null || !menu.getBlockEntity().getBlockPos().equals(packet.pos())) {
                return;
            }
            if (packet.action() == ACTION_COPY) {
                DeepDrawerSettingsCopierLogic.copyForbiddenToCopier(player, menu);
            } else if (packet.action() == ACTION_PASTE) {
                DeepDrawerSettingsCopierLogic.pasteForbiddenFromCopier(player, menu);
            }
        });
    }
}
