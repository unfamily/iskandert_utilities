package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ExperienceFluidMath;

import java.util.function.Consumer;

public class JellyOfKnowledgeItem extends Item {
    public JellyOfKnowledgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.jelly_of_knowledge.use0").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.iska_utils.jelly_of_knowledge.use1").withStyle(ChatFormatting.DARK_GRAY));
        int xp = Math.max(1, Config.knowledgeCompressorJellyXpPoints);
        int mb = ExperienceFluidMath.jellyMbCost();
        tooltip.accept(Component.translatable("tooltip.iska_utils.jelly_of_knowledge.xp", xp).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.iska_utils.jelly_of_knowledge.mb", mb).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        int xpPerJelly = Math.max(1, Config.knowledgeCompressorJellyXpPoints);
        if (player.isShiftKeyDown()) {
            int count = stack.getCount();
            if (count <= 0) {
                return InteractionResult.PASS;
            }
            stack.shrink(count);
            serverPlayer.giveExperiencePoints(xpPerJelly * count);
        } else {
            stack.shrink(1);
            serverPlayer.giveExperiencePoints(xpPerJelly);
        }
        return InteractionResult.CONSUME;
    }
}
