package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.unfamily.iskautils.data.DynamicToolBehaviorScanner;
import net.unfamily.iskautils.data.ToolBehaviorDefinition;
import net.unfamily.iskautils.util.ToolBehaviorAoe;

import java.util.List;

@EventBusSubscriber
public final class ToolBehaviorEvents {
    private static final ThreadLocal<Boolean> AOE_GUARD = ThreadLocal.withInitial(() -> false);

    private ToolBehaviorEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || AOE_GUARD.get()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty()) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(tool.getItem());
        ToolBehaviorDefinition def = DynamicToolBehaviorScanner.getForItem(itemId);
        if (def == null || def.range() <= 0) {
            return;
        }
        if (def.behavior() == net.unfamily.iskautils.data.ToolBehaviorType.LUMBERJACK
                && !ToolBehaviorAoe.matchesHarvest(event.getState(), def)) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos origin = event.getPos();
        List<BlockPos> targets = ToolBehaviorAoe.collectTargets(origin, def);
        if (targets.isEmpty()) {
            return;
        }

        AOE_GUARD.set(true);
        try {
            for (BlockPos target : targets) {
                if (target.equals(origin)) {
                    continue;
                }
                BlockState state = level.getBlockState(target);
                if (state.isAir() || !ToolBehaviorAoe.matchesHarvest(state, def)) {
                    continue;
                }
                if (!tool.isCorrectToolForDrops(state)) {
                    continue;
                }
                player.gameMode.destroyBlock(target);
            }
        } finally {
            AOE_GUARD.set(false);
        }
    }
}
