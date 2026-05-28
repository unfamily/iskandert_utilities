package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.item.component.AncientTabletContents;
import net.unfamily.iskautils.util.RelicTooltipUtil;

import java.util.List;

public class AncientTabletItem extends Item {

    public AncientTabletItem(Properties properties) {
        super(properties.stacksTo(1));
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
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack tablet = context.getItemInHand();
        if (!level.isClientSide) {
            AncientTabletContents.dropAll(level, player, tablet);
            player.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8f, 1.0f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack tablet, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.PRIMARY || tablet.getCount() != 1) {
            return false;
        }
        ItemStack other = slot.getItem();
        if (other.isEmpty()) {
            return false;
        }
        if (!AncientTabletContents.tryInsert(tablet, player.registryAccess(), other)) {
            return false;
        }
        other.shrink(1);
        player.containerMenu.broadcastChanges();
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack tablet,
            ItemStack other,
            Slot slot,
            ClickAction action,
            Player player,
            SlotAccess carriedItem) {
        if (action != ClickAction.PRIMARY || tablet.getCount() != 1 || other.isEmpty()) {
            return false;
        }
        if (!AncientTabletContents.tryInsert(tablet, player.registryAccess(), other)) {
            return false;
        }
        other.shrink(1);
        player.containerMenu.broadcastChanges();
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        RelicTooltipUtil.appendDescLines(tooltip, "ancient_tablet");
        int n = AncientTabletContents.occupiedCount(stack);
        tooltip.add(Component.translatable("tooltip.iska_utils.ancient_tablet.contents", n, AncientTabletContents.MAX_SLOTS));
    }
}
