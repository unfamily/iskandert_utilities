package net.unfamily.iskautils.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.item.component.AncientTabletContents;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.function.Consumer;

public class AncientTabletItem extends BundleItem {

    public AncientTabletItem(Properties properties) {
        super(properties
                .stacksTo(1)
                // Required for BundleItem interactions (insert/extract) to activate.
                .component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            AncientTabletContents.dropAll(level, player, player.getItemInHand(hand));
            player.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8f, 1.0f);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        if (self.getCount() != 1) {
            return false;
        }
        if (player.level().isClientSide()) {
            return false;
        }
        if (!slot.allowModification(player)) {
            return false;
        }

        ItemStack other = slot.getItem();
        var provider = ((net.minecraft.server.level.ServerPlayer) player).registryAccess();

        if ((action == ClickAction.SECONDARY || action == ClickAction.PRIMARY) && !other.isEmpty()) {
            ItemStack moving = other.copy();
            if (AncientTabletContents.tryInsert(self, provider, moving)) {
                slot.set(ItemStack.EMPTY);
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1.0f);
            }
            return true;
        }

        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            ItemStack removed = AncientTabletContents.popLast(self, provider);
            if (!removed.isEmpty()) {
                ItemStack remainder = slot.safeInsert(removed);
                if (!remainder.isEmpty()) {
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
        if (player.level().isClientSide()) {
            return false;
        }
        if (!slot.allowModification(player)) {
            return false;
        }
        var provider = ((net.minecraft.server.level.ServerPlayer) player).registryAccess();

        if ((action == ClickAction.SECONDARY || action == ClickAction.PRIMARY) && !other.isEmpty()) {
            ItemStack moving = other.copy();
            if (AncientTabletContents.tryInsert(self, provider, moving)) {
                carried.set(ItemStack.EMPTY);
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1.0f);
            }
            return true;
        }

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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Consumer<Component> filtered = c -> {
            if (c != null && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
                // Vanilla bundle shows weight-based fullness / "full" messages.
                // Tablet uses slot-based capacity, so we hide those vanilla lines.
                if ("item.minecraft.bundle.fullness".equals(t.getKey()) || "item.minecraft.bundle.full".equals(t.getKey())) {
                    return;
                }
            }
            tooltip.accept(c);
        };
        super.appendHoverText(stack, context, display, filtered, flag);
        int n = AncientTabletContents.occupiedCount(stack);
        ArtifactTooltipUtil.addLoreLine(tooltip, "tooltip.iska_utils.ancient_tablet.desc0");
        ArtifactTooltipUtil.addLoreLine(tooltip, "tooltip.iska_utils.ancient_tablet.desc1");
        ArtifactTooltipUtil.addTechLine(tooltip,
                "tooltip.iska_utils.ancient_tablet.contents", n, AncientTabletContents.MAX_SLOTS);
        ArtifactTooltipUtil.appendDescLinesFrom(tooltip, "ancient_tablet", 2, 0, -1);
    }
}
