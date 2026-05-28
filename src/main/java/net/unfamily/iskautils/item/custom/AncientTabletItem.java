package net.unfamily.iskautils.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.unfamily.iskautils.item.component.AncientTabletContents;
import java.util.List;

public class AncientTabletItem extends BundleItem {

    public AncientTabletItem(Properties properties) {
        super(properties
                .stacksTo(1)
                // Required for BundleItem interactions (insert/extract) to activate.
                .component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return AncientTabletContents.occupiedCount(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int n = AncientTabletContents.occupiedCount(stack);
        float t = Math.min(1f, Math.max(0f, n / (float) AncientTabletContents.MAX_SLOTS));
        return Math.min(1 + (int) Math.floor(t * 12f), 13);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // Remove vanilla bundle fullness line (weight-based) and replace with slot-based count.
        tooltip.removeIf(c -> c != null
                && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && "item.minecraft.bundle.fullness".equals(t.getKey()));
        int n = AncientTabletContents.occupiedCount(stack);
        tooltip.add(Component.translatable("tooltip.iska_utils.ancient_tablet.contents", n, AncientTabletContents.MAX_SLOTS));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tablet = player.getItemInHand(hand);
        if (!level.isClientSide) {
            AncientTabletContents.dropAll(level, player, tablet);
            player.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(tablet, level.isClientSide);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        if (self.getCount() != 1) {
            return false;
        }
        if (player.level().isClientSide) {
            return false;
        }
        if (!slot.allowModification(player)) {
            return false;
        }

        ItemStack other = slot.getItem();
        var provider = ((net.minecraft.server.level.ServerPlayer) player).registryAccess();

        // Insert: right-click (bundle-like) or left-click on a non-empty slot -> move whole stack into tablet.
        if ((action == ClickAction.SECONDARY || action == ClickAction.PRIMARY) && !other.isEmpty()) {
            ItemStack moving = other.copy();
            if (AncientTabletContents.tryInsert(self, provider, moving)) {
                slot.set(ItemStack.EMPTY);
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1.0f);
            }
            return true;
        }

        // Extract: secondary click on empty slot -> remove 1 item (LIFO).
        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            ItemStack removed = AncientTabletContents.popLast(self, provider);
            if (!removed.isEmpty()) {
                ItemStack remainder = slot.safeInsert(removed);
                if (!remainder.isEmpty()) {
                    // Put back if cannot insert.
                    AncientTabletContents.tryInsert(self, provider, remainder);
                } else {
                    player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1.0f);
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        if (self.getCount() != 1) {
            return false;
        }
        if (player.level().isClientSide) {
            return false;
        }
        if (!slot.allowModification(player)) {
            return false;
        }
        var provider = ((net.minecraft.server.level.ServerPlayer) player).registryAccess();

        // Insert from carried stack (right-click bundle-like, allow left-click too).
        if ((action == ClickAction.SECONDARY || action == ClickAction.PRIMARY) && !other.isEmpty()) {
            ItemStack moving = other.copy();
            if (AncientTabletContents.tryInsert(self, provider, moving)) {
                carried.set(ItemStack.EMPTY);
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1.0f);
            }
            return true;
        }

        // Extract to carried.
        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            ItemStack removed = AncientTabletContents.popLast(self, provider);
            if (!removed.isEmpty()) {
                carried.set(removed);
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1.0f);
            }
            return true;
        }

        return false;
    }
}
