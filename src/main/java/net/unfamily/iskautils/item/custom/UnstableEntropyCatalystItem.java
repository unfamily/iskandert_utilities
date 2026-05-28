package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.component.UnstableEntropyCatalystDecay;

import java.util.function.Consumer;

public class UnstableEntropyCatalystItem extends Item {

    private static final int BAR_LENGTH = 15;

    public UnstableEntropyCatalystItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        Consumer<Component> filtered = c -> {
            if (c != null && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
                String key = t.getKey();
                if ("item.dyed".equals(key) || "item.color".equals(key)) {
                    return;
                }
            }
            if (c != null) {
                String raw = c.getString();
                String plain = raw == null ? "" : ChatFormatting.stripFormatting(raw);
                if (plain != null) {
                    String st = plain.trim();
                    if (st.equalsIgnoreCase("Dyed") || st.equalsIgnoreCase("Tinto") || st.equalsIgnoreCase("Tinta")) {
                        return;
                    }
                    if (st.matches("(?i)^(colou?r|colore)\\s*:\\s*#?[0-9a-f]{6}.*$")) {
                        return;
                    }
                }
            }
            tooltip.accept(c);
        };
        super.appendHoverText(stack, context, display, filtered, flag);
        if (!UnstableEntropyCatalystDecay.isDecayEnabled()) {
            return;
        }
        int remaining = UnstableEntropyCatalystDecay.getRemainingTicks(stack);
        int max = Config.unstableEntropyCatalystDecayTicks;
        float ratio = max > 0 ? remaining / (float) max : 0f;
        int filled = Math.round(ratio * BAR_LENGTH);
        ChatFormatting color = ratio > 0.5f ? ChatFormatting.GREEN
                : (ratio > 0.25f ? ChatFormatting.YELLOW : ChatFormatting.RED);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < BAR_LENGTH; i++) {
            bar.append(i < filled ? '█' : '░');
        }
        filtered.accept(Component.literal(bar.toString()).withStyle(color));
        filtered.accept(Component.translatable("tooltip.iska_utils.unstable_entropy_catalyst.decay", remaining / 20, max / 20)
                .withStyle(ChatFormatting.GRAY));
        if (Config.unstableEntropyCatalystDecayKillsPlayer) {
            filtered.accept(Component.translatable("tooltip.iska_utils.unstable_entropy_catalyst.lethal").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
