package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Base class for cursed relics.
 * Concrete effects are implemented elsewhere (events / keybind integration).
 */
public class CursedRelicItem extends Item {
    private static final String TOTEM_OF_PAIN_PATH = "totem_of_pain";

    public CursedRelicItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void appendCursedArtifactTooltip(List<Component> tooltip, String path) {
        tooltip.add(Component.translatable("tooltip.iska_utils." + path + ".cursed"));
        tooltip.add(Component.translatable("tooltip.iska_utils." + path + ".desc0"));
        tooltip.add(Component.translatable("tooltip.iska_utils." + path + ".desc1"));
        if (TOTEM_OF_PAIN_PATH.equals(path)) {
            tooltip.add(Component.translatable("tooltip.iska_utils." + path + ".desc2"));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
        appendCursedArtifactTooltip(tooltip, id.getPath());
    }
}

