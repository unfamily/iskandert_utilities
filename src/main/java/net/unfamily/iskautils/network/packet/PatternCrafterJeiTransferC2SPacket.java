package net.unfamily.iskautils.network.packet;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;

/** Transfers a resolved JEI 3x3 crafting grid (and crafting mode) to the current Pattern Crafter pattern. */
public record PatternCrafterJeiTransferC2SPacket(BlockPos pos, List<ItemStack> ingredients, int craftingMode)
        implements CustomPacketPayload {
    public static final Type<PatternCrafterJeiTransferC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_jei_transfer"));
    public static final StreamCodec<FriendlyByteBuf, PatternCrafterJeiTransferC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PatternCrafterJeiTransferC2SPacket decode(FriendlyByteBuf buffer) {
                    BlockPos pos = buffer.readBlockPos();
                    List<ItemStack> stacks = new ArrayList<>(9);
                    for (int i = 0; i < 9; i++) {
                        stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer));
                    }
                    int craftingMode = buffer.readVarInt();
                    return new PatternCrafterJeiTransferC2SPacket(pos, stacks, craftingMode);
                }

                @Override
                public void encode(FriendlyByteBuf buffer, PatternCrafterJeiTransferC2SPacket packet) {
                    buffer.writeBlockPos(packet.pos());
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = i < packet.ingredients().size() ? packet.ingredients().get(i) : ItemStack.EMPTY;
                        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, stack);
                    }
                    buffer.writeVarInt(packet.craftingMode());
                }
            };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PatternCrafterJeiTransferC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof ImprovedPatternCrafterMenu menu)
                    || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(packet.pos())) return;
            menu.getBlockEntity().applyJeiPattern(packet.ingredients(), packet.craftingMode());
            menu.broadcastFullState();
        });
    }
}
