package net.unfamily.iskautils.item.component;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;

public final class UnstableEntropyCatalystDecay {
    private static final String NBT_REMAINING = "unstable_entropy_catalyst_remaining_ticks";

    private UnstableEntropyCatalystDecay() {}

    public static boolean isDecayEnabled() {
        return Config.unstableEntropyCatalystDecayTicks > 0;
    }

    public static int getRemainingTicks(ItemStack stack) {
        if (!isDecayEnabled()) {
            return Config.unstableEntropyCatalystDecayTicks;
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return Config.unstableEntropyCatalystDecayTicks;
        }
        CompoundTag tag = custom.copyTag();
        if (!tag.contains(NBT_REMAINING)) {
            return Config.unstableEntropyCatalystDecayTicks;
        }
        return tag.getIntOr(NBT_REMAINING, Config.unstableEntropyCatalystDecayTicks);
    }

    public static void setRemainingTicks(ItemStack stack, int ticks) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(NBT_REMAINING, Math.max(0, ticks));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(calcTintRgb(stack)));
        TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        stack.set(DataComponents.TOOLTIP_DISPLAY, display.withHidden(DataComponents.DYED_COLOR, true));
    }

    private static int calcTintRgb(ItemStack stack) {
        float t = Math.min(1f, Math.max(0f, instability(stack)));
        java.util.List<Integer> colors = Config.unstableEntropyCatalystDecayTintColors;
        if (colors == null || colors.isEmpty()) {
            return 0xFF0000;
        }
        if (colors.size() == 1) {
            return colors.get(0) & 0xFFFFFF;
        }
        float seg = t * (colors.size() - 1);
        int i = (int) Math.floor(seg);
        if (i < 0) {
            i = 0;
        } else if (i >= colors.size() - 1) {
            i = colors.size() - 2;
        }
        float u = seg - i;
        int c0 = colors.get(i) & 0xFFFFFF;
        int c1 = colors.get(i + 1) & 0xFFFFFF;
        int r0 = (c0 >> 16) & 0xFF, g0 = (c0 >> 8) & 0xFF, b0 = c0 & 0xFF;
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r = (int) (r0 + (r1 - r0) * u);
        int g = (int) (g0 + (g1 - g0) * u);
        int b = (int) (b0 + (b1 - b0) * u);
        return (r << 16) | (g << 8) | b;
    }

    public static float instability(ItemStack stack) {
        int max = Config.unstableEntropyCatalystDecayTicks;
        if (max <= 0) {
            return 0f;
        }
        int remaining = getRemainingTicks(stack);
        return 1f - (remaining / (float) max);
    }
}
