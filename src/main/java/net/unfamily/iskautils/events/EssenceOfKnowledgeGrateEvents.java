package net.unfamily.iskautils.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.EssenceOfKnowledgeGrateBlock;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class EssenceOfKnowledgeGrateEvents {
    private EssenceOfKnowledgeGrateEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        EssenceOfKnowledgeGrateBlock.clearIfLeft(event.getEntity());
    }
}
