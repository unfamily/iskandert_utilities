package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.client.gui.AutoShopMenu;
import net.unfamily.iskautils.client.gui.ShopBrowsePanel;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import net.unfamily.iskautils.shop.ShopLoader;

public record AutoShopApplyPickerSelectionC2SPacket(BlockPos pos, String entryId, boolean buyMode)
        implements CustomPacketPayload {

    public static final Type<AutoShopApplyPickerSelectionC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "auto_shop_apply_picker_selection"));

    public static final StreamCodec<FriendlyByteBuf, AutoShopApplyPickerSelectionC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AutoShopApplyPickerSelectionC2SPacket::pos,
                    ByteBufCodecs.STRING_UTF8, AutoShopApplyPickerSelectionC2SPacket::entryId,
                    ByteBufCodecs.BOOL, AutoShopApplyPickerSelectionC2SPacket::buyMode,
                    AutoShopApplyPickerSelectionC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutoShopApplyPickerSelectionC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            var blockEntity = player.serverLevel().getBlockEntity(packet.pos());
            if (!(blockEntity instanceof AutoShopBlockEntity autoShop)) {
                return;
            }
            if (!autoShop.canPlayerUse(player)) {
                return;
            }

            ShopEntry entry = ShopLoader.getEntries().get(packet.entryId());
            if (!ShopBrowsePanel.isSelectableAutoShopEntry(entry, packet.buyMode())) {
                return;
            }
            if (packet.buyMode()) {
                if (!(entry.buy > 0 || entry.free)) {
                    return;
                }
            } else if (entry.sell <= 0) {
                return;
            }

            ItemStack filterItem = ShopEntryHelper.displayStackForEntry(entry);
            if (entry.type == ShopEntry.EntryType.ITEM && filterItem.isEmpty()) {
                return;
            }
            if (!filterItem.isEmpty()) {
                filterItem.setCount(1);
            }

            String currency = entry.currency != null && !entry.currency.isEmpty()
                    ? entry.currency
                    : (entry.valute != null && !entry.valute.isEmpty() ? entry.valute : "null_coin");

            autoShop.applyPickerSelection(filterItem, currency, packet.buyMode(), packet.entryId());
            autoShop.setChanged();
            player.serverLevel().sendBlockUpdated(packet.pos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            if (player.containerMenu instanceof AutoShopMenu autoMenu && autoMenu.getBlockPos().equals(packet.pos())) {
                autoMenu.broadcastFullState();
            }
            player.serverLevel().playSound(
                    null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
        });
    }
}
