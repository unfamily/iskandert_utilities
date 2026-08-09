package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.EtherealFrameBlock;
import net.unfamily.iskautils.block.entity.EtherealFrameBlockEntity;

/**
 * Left-click with a reinforcement material applies wither-proof reinforcement to the frame network.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class EtherealFrameReinforceHandler {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!Config.etherealFrameReinforcementEnabled) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EtherealFrameBlock)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        Block material = blockItem.getBlock();
        if (!material.defaultBlockState().is(EtherealFrameBlock.REINFORCEMENT_MATERIALS)) {
            return;
        }

        // Cancel dig while holding a reinforcement material on a frame.
        event.setCanceled(true);

        if (level.isClientSide()) {
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EtherealFrameBlockEntity frame) {
            frame.tryReinforceNetwork(event.getEntity(), stack, material);
        }
    }
}
