package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.CustomModelDataUtil;
import net.unfamily.iskautils.util.RelicBalanceFormat;
import net.unfamily.iskautils.util.RelicActivationUtil;
import net.unfamily.iskautils.util.RelicTooltipUtil;

import java.util.function.Consumer;

/**
 * The Roots relic.
 * Mining speed boost is handled via events.
 */
public class TheRootsItem extends Item {
    private static final String STAGE_ID = "iska_utils_internal-the_roots_equip";

    public TheRootsItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        // Ensure item icon (creative tabs, etc.) uses the correct model variant on the client.
        // Safe on dedicated server too: it's just CustomModelData.
        syncClientCustomModelData(stack);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) return;
        RelicActivationUtil.syncCurioOnlyStage(player, stack, STAGE_ID);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isUnixLike()) {
            return Component.translatable("item.iska_utils.the_roots.unix");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        double maxMult = Config.theRootsBreakSpeedMinMultiplier + Config.theRootsBreakSpeedMaxBonus;
        RelicTooltipUtil.appendDescLines(
                tooltip,
                "the_roots",
                2,
                RelicBalanceFormat.speedBonusPercent(Config.theRootsBreakSpeedMinMultiplier),
                RelicBalanceFormat.speedBonusPercent(maxMult));
    }

    public static boolean isUnixLike() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("linux") || os.contains("mac") || os.contains("unix") || os.contains("android");
    }

    /** Client-only: drives {@code items/the_roots.json} range_dispatch. */
    public static void syncClientCustomModelData(ItemStack stack) {
        // Model fallback is "the_root". We set custom_model_data=1 only on non-unix to switch to "the_roots".
        CustomModelDataUtil.setFloat0(stack, isUnixLike() ? 0.0F : 1.0F);
    }
}

