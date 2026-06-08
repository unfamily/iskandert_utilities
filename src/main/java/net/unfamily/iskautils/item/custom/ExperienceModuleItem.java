package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.unfamily.iskautils.util.ScreenAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.Config;

import java.util.List;

public class ExperienceModuleItem extends Item {

    public ExperienceModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (ScreenAccess.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.iska_utils.mob_reaper_module.max", Config.reaperExperienceUpgradeMax)
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.iska_utils.mob_reaper_module.experience_bonus",
                            (int) Math.round(Config.reaperExperienceBonusPerLevel * 100) + "%")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.press_shift")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
