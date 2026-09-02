package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.client.gui.AutoShopMenu;
import net.unfamily.iskautils.client.gui.ShopBrowsePanel;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Converts the AutoShop selected item into a matching fluid/gas shop entry (explicit button; not auto on insert).
 */
public record AutoShopConvertSelectedC2SPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<AutoShopConvertSelectedC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "auto_shop_convert_selected"));

    public static final StreamCodec<FriendlyByteBuf, AutoShopConvertSelectedC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AutoShopConvertSelectedC2SPacket::pos,
                    AutoShopConvertSelectedC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutoShopConvertSelectedC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            var blockEntity = player.level().getBlockEntity(packet.pos());
            if (!(blockEntity instanceof AutoShopBlockEntity autoShop) || !autoShop.canPlayerUse(player)) {
                return;
            }

            ItemStack selected = autoShop.getSelectedItem();
            if (selected.isEmpty()) {
                return;
            }

            ShopEntry entry = resolveConvertedEntry(selected, autoShop.isAutoBuyMode());
            if (entry == null) {
                return;
            }

            boolean buyMode = ShopEntryHelper.resolveBuyModeForEntry(entry, autoShop.isAutoBuyMode());
            if (!ShopBrowsePanel.isSelectableAutoShopEntry(entry, buyMode)) {
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

            autoShop.applyPickerSelection(filterItem, currency, buyMode, entry.id);
            autoShop.setChanged();
            player.level().sendBlockUpdated(packet.pos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            if (player.containerMenu instanceof AutoShopMenu autoMenu && autoMenu.getBlockPos().equals(packet.pos())) {
                autoMenu.broadcastFullState();
            }
            player.level().playSound(
                    null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
        });
    }

    @Nullable
    private static ShopEntry resolveConvertedEntry(ItemStack selected, boolean preferBuy) {
        FluidStack contained = ShopEntryHelper.fluidContainedInItem(selected);
        contained = ShopEntryHelper.normalizeFluidIngredient(contained);
        if (!contained.isEmpty()) {
            ShopEntry fluidEntry = ShopEntryHelper.findMatchingFluidEntry(contained, preferBuy);
            if (fluidEntry != null) {
                return fluidEntry;
            }
        }
        if (MekChemicalHelper.isLoaded()) {
            Object gas = MekChemicalHelper.sampleFromItemStack(selected);
            if (!MekChemicalHelper.isEmpty(gas)) {
                return ShopEntryHelper.findMatchingGasEntry(gas, preferBuy);
            }
        }
        return null;
    }
}
