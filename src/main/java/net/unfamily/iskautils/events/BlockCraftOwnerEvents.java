package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.util.BlockCraftOwner;

/**
 * Assigns {@code ownerPlayer} when a player places a block entity that implements
 * {@link BlockCraftOwner.BlockCraftOwnerHolder}. Existing machines without owner stay valid.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class BlockCraftOwnerEvents {
    private BlockCraftOwnerEvents() {}

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof BlockCraftOwner.BlockCraftOwnerHolder holder && holder.getOwnerPlayerUuid() == null) {
            holder.setOwnerPlayerUuid(player.getUUID());
            be.setChanged();
        }
    }
}
