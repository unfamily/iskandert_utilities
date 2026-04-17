package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity;

import java.util.ArrayList;
import java.util.List;

public record SoundMufflerFilterUpdateC2SPacket(BlockPos pos, List<String> filterSoundIds) implements CustomPacketPayload {

    public static final Type<SoundMufflerFilterUpdateC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "sound_muffler_filter_update"));

    public static final StreamCodec<FriendlyByteBuf, SoundMufflerFilterUpdateC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos());
                List<String> ids = p.filterSoundIds();
                buf.writeVarInt(ids.size());
                for (String s : ids) buf.writeUtf(s);
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                int n = buf.readVarInt();
                List<String> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(buf.readUtf());
                return new SoundMufflerFilterUpdateC2SPacket(pos, list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoundMufflerFilterUpdateC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof SoundMufflerBlockEntity muffler) {
                muffler.setFilterSoundIds(packet.filterSoundIds());
                player.level().playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
            }
        });
    }
}
