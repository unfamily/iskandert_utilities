package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.component.UnstableEntropyCatalystDecay;

import java.util.List;

public class UnstableEntropyCatalystItem extends Item {

    private static final int BAR_LENGTH = 15;

    public UnstableEntropyCatalystItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.removeIf(c -> {
            if (c == null) {
                return false;
            }
            var contents = c.getContents();
            if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
                String key = t.getKey();
                return "item.dyed".equals(key) || "item.color".equals(key);
            }
            return false;
        });
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
        tooltip.add(Component.literal(bar.toString()).withStyle(color));
        tooltip.add(Component.translatable("tooltip.iska_utils.unstable_entropy_catalyst.decay", remaining / 20, max / 20)
                .withStyle(ChatFormatting.GRAY));
        if (Config.unstableEntropyCatalystDecayKillsPlayer) {
            tooltip.add(Component.translatable("tooltip.iska_utils.unstable_entropy_catalyst.lethal").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
