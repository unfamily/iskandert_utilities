package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.CollectingCrateBlockEntity;
import net.unfamily.iskautils.client.gui.CollectingCrateMenu;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.CollectingCratePreview;

/**
 * C2S: adjust collection area (0=up, 1=left, 2=right, 3=depth). increment=true add, false subtract.
 */
public record CollectingCrateSizeC2SPacket(BlockPos pos, int direction, boolean increment, int amount)
        implements CustomPacketPayload {

    public static final Type<CollectingCrateSizeC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "collecting_crate_size")
    );

    public static final StreamCodec<FriendlyByteBuf, CollectingCrateSizeC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CollectingCrateSizeC2SPacket::pos,
            ByteBufCodecs.INT,
            CollectingCrateSizeC2SPacket::direction,
            ByteBufCodecs.BOOL,
            CollectingCrateSizeC2SPacket::increment,
            ByteBufCodecs.INT,
            CollectingCrateSizeC2SPacket::amount,
            CollectingCrateSizeC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CollectingCrateSizeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            CollectingCrateBlockEntity crate = resolveCrate(player, level, packet.pos());
            if (crate == null) {
                return;
            }
            int amount = Math.max(1, Math.min(10, packet.amount()));
            crate.adjustSize(packet.direction(), packet.increment(), amount);
            level.playSound(null, crate.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS, 0.3f, 1.0f);
            if (player.containerMenu instanceof CollectingCrateMenu) {
                player.containerMenu.broadcastChanges();
            }
            if (crate.isPreviewEnabled()) {
                ModMessages.clearPreviewForBuilder(player, crate.getBlockPos());
                CollectingCratePreview.sendFootprint(player, level, crate.getBlockPos(), crate);
            }
        });
    }

    private static CollectingCrateBlockEntity resolveCrate(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player.containerMenu instanceof CollectingCrateMenu menu) {
            CollectingCrateBlockEntity menuCrate = menu.getBlockEntity();
            if (menuCrate != null && menuCrate.getBlockPos().equals(pos)) {
                return menuCrate;
            }
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CollectingCrateBlockEntity crate) {
            return crate;
        }
        return null;
    }
}
