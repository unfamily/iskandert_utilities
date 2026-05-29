package net.unfamily.iskautils.item.entropic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

/**
 * Pickaxe + axe + shovel in one indestructible tool (1.21.1).
 */
public class EntropicPaxelItem extends Item {
    public EntropicPaxelItem(Properties properties) {
        super(properties
                .component(DataComponents.TOOL, EntropicGear.createPaxelTool())
                .attributes(EntropicGear.paxelAttributes()));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability);
    }

    @Override
    public int getEnchantmentValue() {
        return EntropicTier.INSTANCE.getEnchantmentValue();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.entropic.unbreakable"));
    }
}
