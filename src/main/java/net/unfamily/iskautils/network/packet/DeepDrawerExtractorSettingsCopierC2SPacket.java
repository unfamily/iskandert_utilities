package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.DeepDrawerExtractorMenu;
import net.unfamily.iskautils.integration.anotherdynamics.DeepDrawerSettingsCopierLogic;

/**
 * Client-to-server: copy current filter list to settings copier or paste from copier into allow/deny list.
 */
public record DeepDrawerExtractorSettingsCopierC2SPacket(BlockPos pos, int action, int allowDeny)
        implements CustomPacketPayload {

    public static final int ACTION_COPY = 0;
    public static final int ACTION_PASTE = 1;
    public static final int LIST_ALLOW = 0;
    public static final int LIST_DENY = 1;

    public static final Type<DeepDrawerExtractorSettingsCopierC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "deep_drawer_extractor_settings_copier"));

    public static final StreamCodec<FriendlyByteBuf, DeepDrawerExtractorSettingsCopierC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos());
                buf.writeVarInt(p.action());
                buf.writeVarInt(p.allowDeny());
            },
            buf -> new DeepDrawerExtractorSettingsCopierC2SPacket(
                    BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeepDrawerExtractorSettingsCopierC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof DeepDrawerExtractorMenu menu)) {
                return;
            }
            if (!menu.getBlockPos().equals(packet.pos())) {
                return;
            }
            boolean allowList = packet.allowDeny() == LIST_ALLOW;
            if (packet.action() == ACTION_COPY) {
                DeepDrawerSettingsCopierLogic.copyToCopier(player, menu, allowList);
            } else if (packet.action() == ACTION_PASTE) {
                DeepDrawerSettingsCopierLogic.pasteFromCopier(player, menu, allowList);
            }
        });
    }
}
