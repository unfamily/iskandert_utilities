package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.stage.StageRegistry;
import net.unfamily.iskautils.util.CurioEquipUtil;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.util.RelicActivationUtil;
import net.unfamily.iskautils.util.RelicEquipStages;

import java.util.HashSet;
import java.util.Set;

/**
 * Resets and re-applies internal equip stages from Curios slots each tick.
 */
@EventBusSubscriber
public final class CurioEquipStageSync {
    private CurioEquipStageSync() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }

        for (String stageId : RelicEquipStages.allStages()) {
            StageRegistry.removePlayerStage(sp, stageId, true);
        }

        if (!ModUtils.isCuriosLoaded()) {
            return;
        }

        Set<String> applied = new HashSet<>();
        CurioEquipUtil.forEachEquippedCurioStack(sp, stack -> applyStageForStack(sp, stack, applied));
    }

    private static void applyStageForStack(ServerPlayer player, ItemStack stack, Set<String> applied) {
        if (stack.isEmpty()) {
            return;
        }
        if (ModUtils.isStackInVanillaPlayerInventory(player, stack)) {
            return;
        }
        if (RelicActivationUtil.isStackInHands(player, stack)) {
            return;
        }

        Item item = stack.getItem();
        String stageId = RelicEquipStages.stageForItem(item);
        if (stageId == null) {
            stageId = RelicEquipStages.stageForCursedRelic(item);
        }
        if (stageId == null || applied.contains(stageId)) {
            return;
        }

        StageRegistry.addPlayerStage(player, stageId, true);
        applied.add(stageId);
    }
}
