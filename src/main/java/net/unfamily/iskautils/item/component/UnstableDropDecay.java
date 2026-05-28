package net.unfamily.iskautils.item.component;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.CustomModelDataUtil;

public final class UnstableDropDecay {
    private static final String NBT_REMAINING = "unstable_drop_remaining_ticks";

    private UnstableDropDecay() {}

    public static boolean isDecayEnabled() {
        return Config.unstableDropDecayTicks > 0;
    }

    public static int getRemainingTicks(ItemStack stack) {
        if (!isDecayEnabled()) {
            return Config.unstableDropDecayTicks;
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return Config.unstableDropDecayTicks;
        }
        CompoundTag tag = custom.copyTag();
        if (!tag.contains(NBT_REMAINING)) {
            return Config.unstableDropDecayTicks;
        }
        return tag.getIntOr(NBT_REMAINING, Config.unstableDropDecayTicks);
    }

    public static void setRemainingTicks(ItemStack stack, int ticks) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(NBT_REMAINING, Math.max(0, ticks));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        CustomModelDataUtil.setFloat0(stack, instabilityIndex(stack));
    }

    public static float instability(ItemStack stack) {
        int max = Config.unstableDropDecayTicks;
        if (max <= 0) {
            return 0f;
        }
        int remaining = getRemainingTicks(stack);
        return 1f - (remaining / (float) max);
    }

    private static float instabilityIndex(ItemStack stack) {
        return instability(stack) * 3f;
    }
}
