package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoot;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryScriptRunner;

import java.util.function.Consumer;

public class SuspiciousDeliveryItem extends Item {

    public SuspiciousDeliveryItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.suspicious_delivery.obtaining0"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.suspicious_delivery.obtaining1"));
        if (ModList.get().isLoaded("artifacts")) {
            tooltip.accept(Component.translatable("tooltip.iska_utils.suspicious_delivery.obtaining2"));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        var entry = SuspiciousDeliveryLoot.pick(sp, sp.getRandom());
        stack.shrink(1);
        if (entry != null) {
            SuspiciousDeliveryScriptRunner.start(sp, entry);
        }
        return InteractionResult.CONSUME;
    }
}
