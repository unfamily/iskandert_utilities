package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.ModItems;

/**
 * Opens the Gift on the first right-click and drops one random Christmas bauble.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class GiftBlockBreakEventHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockState clickedBlock = level.getBlockState(event.getPos());
        if (!clickedBlock.is(ModBlocks.GIFT.get())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

        Player player = event.getEntity();
        if (player == null) {
            return;
        }

        openGift(level, player, event.getPos());
    }

    private static void openGift(ServerLevel level, Player player, BlockPos pos) {
        Item reward = pickRandomBauble(player);
        ItemStack stack = new ItemStack(reward, 1);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static Item pickRandomBauble(Player player) {
        return switch (player.getRandom().nextInt(3)) {
            case 0 -> ModItems.CLASSIC_CHRISTMAS_BAUBLE.get();
            case 1 -> ModItems.GREEDY_CHRISTMAS_BAUBLE.get();
            default -> ModItems.ENTROPIC_CHRISTMAS_BAUBLE.get();
        };
    }
}
