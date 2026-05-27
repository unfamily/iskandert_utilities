package net.unfamily.iskautils.item.custom;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoader;
import net.neoforged.fml.ModList;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class SuspiciousDeliveryItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuspiciousDeliveryItem.class);

    public SuspiciousDeliveryItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.suspicious_delivery.obtaining0"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.suspicious_delivery.obtaining1"));
        if (ModList.get().isLoaded("artifacts")) {
            tooltip.accept(Component.translatable("tooltip.iska_utils.suspicious_delivery.artifacts_loaded"));
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

        SuspiciousDeliveryDefinition def = SuspiciousDeliveryLoader.get();
        SuspiciousDeliveryDefinition.Entry entry = pickEntry(def.entries(), sp.getRandom());
        if (entry != null) {
            Vec3 origin = sp.position();
            CommandSourceStack source = sp.createCommandSourceStack().withSuppressedOutput();
            String ctx = "suspicious_delivery";
            for (SuspiciousDeliveryDefinition.Action a : entry.actions()) {
                try {
                    a.run(source, sp, origin, LOGGER, ctx);
                } catch (Exception e) {
                    LOGGER.error("Error running Suspicious Delivery action: {}", e.getMessage());
                }
            }
        }

        stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    private static SuspiciousDeliveryDefinition.Entry pickEntry(List<SuspiciousDeliveryDefinition.Entry> entries, RandomSource random) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        int total = 0;
        for (var e : entries) {
            total += Math.max(0, e.weight());
        }
        if (total <= 0) {
            return entries.get(0);
        }
        int r = random.nextInt(total);
        int acc = 0;
        for (var e : entries) {
            acc += Math.max(0, e.weight());
            if (r < acc) {
                return e;
            }
        }
        return entries.get(entries.size() - 1);
    }
}

