package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.client.gui.ShopMenu;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the shop GUI without placing a shop block.
 */
public class PortableShopItem extends Item {

    public PortableShopItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level,
                                                           @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return Component.translatable("gui.iska_utils.shop.title");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player p) {
                    return ShopMenu.createPortable(containerId, inv);
                }
            });
        }
        return InteractionResultHolder.consume(stack);
    }
}
