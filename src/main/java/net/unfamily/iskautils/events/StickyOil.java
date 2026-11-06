package net.unfamily.iskautils.events;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.unfamily.iskalib.fluid.StickyFluidEvents;
import net.unfamily.iskautils.IskaUtils;

/**
 * Awards the Utilities sticky-oil advancement when Library reports the player is in crude oil.
 * Sticky slowdown and fluid lists live in {@code iska_lib}.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class StickyOil {
	private StickyOil() {}

	@SubscribeEvent
	public static void onInCrudeOil(StickyFluidEvents.InCrudeOil event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		AdvancementHolder adv = player.server.getAdvancements().get(ResourceLocation.parse("iska_utils:sticky_oil"));
		if (adv == null) {
			return;
		}
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
		if (progress.isDone()) {
			return;
		}
		for (String criteria : progress.getRemainingCriteria()) {
			player.getAdvancements().award(adv, criteria);
		}
	}
}
