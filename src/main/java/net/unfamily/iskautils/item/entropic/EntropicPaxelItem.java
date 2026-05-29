package net.unfamily.iskautils.item.entropic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return EntropicInteractions.onAxeUse(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult shovel = EntropicInteractions.onShovelUseOn(context);
        if (shovel != InteractionResult.PASS) {
            return shovel;
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        EntropicTooltip.appendToolLines(tooltip, "entropic_paxel");
    }
}
