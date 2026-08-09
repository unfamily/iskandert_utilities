package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.LabelingMachineMenu;

/**
 * C2S: apply a formatted custom name to the Labeling Machine target slot.
 */
public record LabelingMachineRenameC2SPacket(
        String name,
        boolean bold,
        boolean italic,
        boolean underline,
        boolean strikethrough,
        boolean obfuscated,
        int colorRgb
) implements CustomPacketPayload {

    public static final Type<LabelingMachineRenameC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "labeling_machine_rename"));

    public static final StreamCodec<FriendlyByteBuf, LabelingMachineRenameC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.name());
                int flags = 0;
                if (p.bold()) flags |= 1;
                if (p.italic()) flags |= 2;
                if (p.underline()) flags |= 4;
                if (p.strikethrough()) flags |= 8;
                if (p.obfuscated()) flags |= 16;
                buf.writeByte(flags);
                buf.writeInt(p.colorRgb());
            },
            buf -> {
                String name = buf.readUtf();
                int flags = buf.readUnsignedByte();
                int color = buf.readInt();
                return new LabelingMachineRenameC2SPacket(
                        name,
                        (flags & 1) != 0,
                        (flags & 2) != 0,
                        (flags & 4) != 0,
                        (flags & 8) != 0,
                        (flags & 16) != 0,
                        color);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LabelingMachineRenameC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof LabelingMachineMenu menu)) {
                return;
            }
            menu.applyFormattedName(
                    packet.name(),
                    packet.bold(),
                    packet.italic(),
                    packet.underline(),
                    packet.strikethrough(),
                    packet.obfuscated(),
                    packet.colorRgb());
        });
    }
}
