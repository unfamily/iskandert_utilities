package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.LabelingMachineMenu;
import net.unfamily.iskautils.util.LabelingNameStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * C2S: apply multi-line formatted lore to the Labeling Machine target slot.
 */
public record LabelingMachineLoreC2SPacket(
        List<List<LabelingNameStyle.Segment>> lines
) implements CustomPacketPayload {

    public static final Type<LabelingMachineLoreC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "labeling_machine_lore"));

    public static final StreamCodec<FriendlyByteBuf, LabelingMachineLoreC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                List<List<LabelingNameStyle.Segment>> lines = p.lines() == null ? List.of() : p.lines();
                int lineCount = Math.min(lines.size(), LabelingNameStyle.HARD_MAX_LORE_LINES);
                buf.writeVarInt(lineCount);
                for (int li = 0; li < lineCount; li++) {
                    writeSegments(buf, lines.get(li));
                }
            },
            buf -> {
                int lineCount = buf.readVarInt();
                if (lineCount < 0 || lineCount > LabelingNameStyle.HARD_MAX_LORE_LINES) {
                    lineCount = 0;
                }
                List<List<LabelingNameStyle.Segment>> lines = new ArrayList<>(lineCount);
                for (int li = 0; li < lineCount; li++) {
                    lines.add(readSegments(buf));
                }
                return new LabelingMachineLoreC2SPacket(lines);
            }
    );

    private static void writeSegments(FriendlyByteBuf buf, List<LabelingNameStyle.Segment> segments) {
        List<LabelingNameStyle.Segment> list = segments == null ? List.of() : segments;
        int count = Math.min(list.size(), LabelingNameStyle.MAX_SEGMENTS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            LabelingNameStyle.Segment s = list.get(i);
            buf.writeUtf(LabelingNameStyle.clampSegmentText(s.text), LabelingNameStyle.MAX_SEGMENT_LENGTH);
            int flags = 0;
            if (s.bold) flags |= 1;
            if (s.italic) flags |= 2;
            if (s.underline) flags |= 4;
            if (s.strikethrough) flags |= 8;
            if (s.obfuscated) flags |= 16;
            buf.writeByte(flags);
            buf.writeInt(s.colorRgb);
        }
    }

    private static List<LabelingNameStyle.Segment> readSegments(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > LabelingNameStyle.MAX_SEGMENTS) {
            count = 0;
        }
        List<LabelingNameStyle.Segment> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String text = buf.readUtf(LabelingNameStyle.MAX_SEGMENT_LENGTH);
            int flags = buf.readUnsignedByte();
            int color = buf.readInt();
            list.add(new LabelingNameStyle.Segment(
                    text,
                    (flags & 1) != 0,
                    (flags & 2) != 0,
                    (flags & 4) != 0,
                    (flags & 8) != 0,
                    (flags & 16) != 0,
                    color));
        }
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LabelingMachineLoreC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof LabelingMachineMenu menu)) {
                return;
            }
            menu.applyFormattedLore(packet.lines());
        });
    }
}
