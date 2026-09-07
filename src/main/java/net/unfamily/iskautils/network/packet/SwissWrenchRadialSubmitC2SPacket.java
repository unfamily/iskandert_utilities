package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.VectorBlock;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock;
import net.unfamily.iskautils.item.custom.SwissWrenchItem;
import net.unfamily.iskautils.util.SwissWrenchRotationProperties;

import java.util.ArrayList;
import java.util.List;

/** C2S: apply one or more BlockState property values chosen in the Swiss Wrench radial UI. */
public record SwissWrenchRadialSubmitC2SPacket(BlockPos pos, List<String> propertyNames, List<String> valueNames)
        implements CustomPacketPayload {

    private static final TagKey<Block> WRENCH_NOT_ROTATE =
            BlockTags.create(Identifier.tryParse("c:wrench_not_rotate"));

    public static final Type<SwissWrenchRadialSubmitC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "swiss_wrench_radial_submit"));

    public static final StreamCodec<FriendlyByteBuf, SwissWrenchRadialSubmitC2SPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        BlockPos.STREAM_CODEC.encode(buf, packet.pos());
                        buf.writeVarInt(packet.propertyNames().size());
                        for (int i = 0; i < packet.propertyNames().size(); i++) {
                            buf.writeUtf(packet.propertyNames().get(i), 64);
                            buf.writeUtf(packet.valueNames().get(i), 64);
                        }
                    },
                    buf -> {
                        BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                        int size = buf.readVarInt();
                        List<String> propertyNames = new ArrayList<>(size);
                        List<String> valueNames = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            propertyNames.add(buf.readUtf(64));
                            valueNames.add(buf.readUtf(64));
                        }
                        return new SwissWrenchRadialSubmitC2SPacket(pos, propertyNames, valueNames);
                    }
            );

    /** Convenience for a single property change. */
    public static SwissWrenchRadialSubmitC2SPacket single(BlockPos pos, String propertyName, String valueName) {
        return new SwissWrenchRadialSubmitC2SPacket(pos, List.of(propertyName), List.of(valueName));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SwissWrenchRadialSubmitC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof SwissWrenchItem)) {
                return;
            }
            if (SetWrenchDirectionBlock.getSelectedRotationMode(stack) != SetWrenchDirectionBlock.RotationMode.RADIAL) {
                return;
            }
            if (packet.propertyNames().isEmpty()
                    || packet.propertyNames().size() != packet.valueNames().size()
                    || packet.propertyNames().size() > 16) {
                return;
            }
            for (String propertyName : packet.propertyNames()) {
                if (!SwissWrenchRotationProperties.isRegistered(propertyName)) {
                    return;
                }
            }

            Level level = player.level();
            BlockPos pos = packet.pos();
            if (!level.isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }

            BlockState current = level.getBlockState(pos);
            if (current.is(WRENCH_NOT_ROTATE) || current.getBlock() instanceof VectorBlock) {
                player.sendOverlayMessage(
                        Component.translatable("item.iska_utils.swiss_wrench.message.cannot_rotate"));
                return;
            }

            var applied = SwissWrenchRotationProperties.applyNamedValues(
                    current,
                    new ArrayList<>(packet.propertyNames()),
                    new ArrayList<>(packet.valueNames()));
            if (applied.isEmpty()) {
                player.sendOverlayMessage(
                        Component.translatable("item.iska_utils.swiss_wrench.message.cannot_rotate"));
                return;
            }

            BlockState newState = applied.get();
            if (!newState.canSurvive(level, pos)) {
                player.sendOverlayMessage(
                        Component.translatable("item.iska_utils.swiss_wrench.message.cannot_rotate"));
                return;
            }

            level.setBlock(pos, newState, 3);
            level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.sendOverlayMessage(
                    Component.translatable("item.iska_utils.swiss_wrench.message.block_rotated"));
        });
    }
}
