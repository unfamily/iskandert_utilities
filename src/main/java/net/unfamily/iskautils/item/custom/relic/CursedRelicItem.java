package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/**
 * Base class for cursed relics.
 * Concrete effects are implemented elsewhere (events / keybind integration).
 */
public class CursedRelicItem extends Item {
    private static final String TOTEM_OF_PAIN_PATH = "totem_of_pain";

    public CursedRelicItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void appendCursedArtifactTooltip(Consumer<Component> tooltip, String path) {
        tooltip.accept(Component.translatable("tooltip.iska_utils." + path + ".cursed"));
        tooltip.accept(Component.translatable("tooltip.iska_utils." + path + ".desc0"));
        tooltip.accept(Component.translatable("tooltip.iska_utils." + path + ".desc1"));
        if (TOTEM_OF_PAIN_PATH.equals(path)) {
            tooltip.accept(Component.translatable("tooltip.iska_utils." + path + ".desc2"));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        appendCursedArtifactTooltip(tooltip, id.getPath());
    }
}

