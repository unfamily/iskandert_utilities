package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.events.DruidicAgglomerationSpreadHandler;
import net.unfamily.iskautils.util.DruidicPodzolUtil;

import java.util.List;

public class DruidicAgglomerationItem extends Item {
    public DruidicAgglomerationItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        if (!DruidicPodzolUtil.isConvertible(clicked)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }
        return tryConvert(server, pos, stack, context.getPlayer());
    }

    public static InteractionResult tryConvert(ServerLevel server, BlockPos pos, ItemStack stack, Player player) {
        if (!DruidicAgglomerationSpreadHandler.enqueue(server, pos)) {
            return InteractionResult.PASS;
        }

        server.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8F, 1.2F);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                12, 0.35D, 0.15D, 0.35D, 0.02D);
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.druidic_agglomeration.desc0"));
        tooltip.add(Component.translatable("tooltip.iska_utils.druidic_agglomeration.desc1"));
    }
}
