package net.unfamily.iskautils.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * Syncs item display models via {@link DataComponents#CUSTOM_MODEL_DATA} (MC 1.26+).
 * Legacy NBT {@code CustomModelData} inside {@code custom_data} is not read by the client renderer.
 */
public final class CustomModelDataUtil {
    private CustomModelDataUtil() {}

    /** Sets float index 0 used by {@code minecraft:custom_model_data} item model properties. */
    public static void setFloat0(ItemStack stack, float value) {
        if (value == 0.0F) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            removeLegacyCustomModelDataTag(stack);
            return;
        }
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(value), List.of(), List.of(), List.of()));
        removeLegacyCustomModelDataTag(stack);
    }

    private static void removeLegacyCustomModelDataTag(ItemStack stack) {
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return;
        }
        var tag = custom.copyTag();
        if (!tag.contains("CustomModelData")) {
            return;
        }
        tag.remove("CustomModelData");
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
    }
}
