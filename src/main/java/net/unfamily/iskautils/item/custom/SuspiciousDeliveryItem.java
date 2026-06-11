package net.unfamily.iskautils.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.fml.ModList;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoot;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryScriptRunner;

import java.util.List;

public class SuspiciousDeliveryItem extends Item {

    public SuspiciousDeliveryItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.suspicious_delivery.obtaining0"));
        tooltip.add(Component.translatable("tooltip.iska_utils.suspicious_delivery.obtaining1"));
        if (ModList.get().isLoaded("artifacts")) {
            tooltip.add(Component.translatable("tooltip.iska_utils.suspicious_delivery.obtaining2"));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        // FakePlayer extends ServerPlayer — automation can open packages too.
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.pass(stack);
        }
        if (sp instanceof FakePlayer && sp.level().isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        SuspiciousDeliveryDefinition.Entry entry = SuspiciousDeliveryLoot.pick(sp, sp.getRandom());
        stack.shrink(1);
        if (entry != null) {
            SuspiciousDeliveryScriptRunner.start(sp, entry);
        }
        return InteractionResultHolder.consume(stack);
    }
}
