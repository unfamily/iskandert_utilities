package net.unfamily.iskautils.network.packet;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;

/** Applies JEI variable filters/letters immediately. Pattern grid stays pending until Save. */
public record PatternCrafterJeiTransferC2SPacket(
        BlockPos pos, List<ItemStack> ingredients, List<String> filterSpecs, int craftingMode)
        implements CustomPacketPayload {
    public static final Type<PatternCrafterJeiTransferC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_jei_transfer"));
    public static final StreamCodec<FriendlyByteBuf, PatternCrafterJeiTransferC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PatternCrafterJeiTransferC2SPacket decode(FriendlyByteBuf buffer) {
                    BlockPos pos = buffer.readBlockPos();
                    List<ItemStack> stacks = new ArrayList<>(9);
                    for (int i = 0; i < 9; i++) {
                        stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer));
                    }
                    List<String> specs = new ArrayList<>(9);
                    for (int i = 0; i < 9; i++) {
                        specs.add(buffer.readUtf(256));
                    }
                    int craftingMode = buffer.readVarInt();
                    return new PatternCrafterJeiTransferC2SPacket(pos, stacks, specs, craftingMode);
                }

                @Override
                public void encode(FriendlyByteBuf buffer, PatternCrafterJeiTransferC2SPacket packet) {
                    buffer.writeBlockPos(packet.pos());
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = i < packet.ingredients().size() ? packet.ingredients().get(i) : ItemStack.EMPTY;
                        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, stack);
                    }
                    for (int i = 0; i < 9; i++) {
                        String spec = i < packet.filterSpecs().size() && packet.filterSpecs().get(i) != null
                                ? packet.filterSpecs().get(i) : "";
                        buffer.writeUtf(spec, 256);
                    }
                    buffer.writeVarInt(packet.craftingMode());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternCrafterJeiTransferC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof ImprovedPatternCrafterMenu menu)
                    || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(packet.pos())) return;
            // Variables only — pattern grid / crafting mode remain client-pending until Save.
            menu.getBlockEntity().applyJeiVariablesOnly(packet.ingredients(), packet.filterSpecs());
            menu.broadcastFullState();
        });
    }
}
