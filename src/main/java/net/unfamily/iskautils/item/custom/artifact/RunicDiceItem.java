package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactBalanceFormat;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.function.Consumer;

/**
 * Charm artifact. Random attack-speed bonus stored on the equipped stack.
 */
public class RunicDiceItem extends Item {
    public static final String NBT_ATTACK_SPEED_BONUS = "runic_dice_attack_speed_bonus";

    public RunicDiceItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static double getStoredBonus(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1.0D;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(NBT_ATTACK_SPEED_BONUS)) {
            return -1.0D;
        }
        return tag.getDouble(NBT_ATTACK_SPEED_BONUS).orElse(-1.0D);
    }

    public static void setStoredBonus(ItemStack stack, double bonus) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putDouble(NBT_ATTACK_SPEED_BONUS, bonus);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static double rollBonus(RandomSource random) {
        double min = Math.min(Config.runicDiceAttackSpeedMin, Config.runicDiceAttackSpeedMax);
        double max = Math.max(Config.runicDiceAttackSpeedMin, Config.runicDiceAttackSpeedMax);
        if (max <= min) {
            return min;
        }
        return min + random.nextDouble() * (max - min);
    }

    public static void ensureRoll(ItemStack stack, RandomSource random) {
        if (getStoredBonus(stack) < 0.0D) {
            setStoredBonus(stack, rollBonus(random));
        }
    }

    public static void reroll(ItemStack stack, RandomSource random) {
        setStoredBonus(stack, rollBonus(random));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        ArtifactTooltipUtil.addLoreLine(tooltip, "tooltip.iska_utils.runic_dice.desc0");
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.runic_dice.desc1");
        ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.runic_dice.desc2",
                ArtifactBalanceFormat.percent(Config.runicDiceAttackSpeedMin),
                ArtifactBalanceFormat.percent(Config.runicDiceAttackSpeedMax));
        double bonus = getStoredBonus(stack);
        if (bonus >= 0.0D) {
            ArtifactTooltipUtil.addTechLine(tooltip, "tooltip.iska_utils.runic_dice.desc3",
                    ArtifactBalanceFormat.percent(bonus));
        }
    }
}
