package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.client.gui.LabelingMachineMenu;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Portable item GUI: formatted rename of an item in a temporary slot (no energy / no BE).
 */
public class LabelingMachineItem extends Item {

    public LabelingMachineItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level,
                                          @NotNull Player player,
                                          @NotNull InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return Component.translatable("gui.iska_utils.labeling_machine.title");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player p) {
                    return new LabelingMachineMenu(containerId, inv);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull TooltipContext context,
                                @NotNull TooltipDisplay tooltipDisplay,
                                @NotNull Consumer<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.iska_utils.labeling_machine.desc0"));
    }
}
