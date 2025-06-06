package net.unfamily.iskautils;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;
import net.minecraft.tags.FluidTags;
import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.advancements.AdvancementHolder;

@EventBusSubscriber
public class StickyOilProcedure {
	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		boolean matched = false;

		if (entity == null)
			return;
		
		FluidState currentFluidState = world.getFluidState(BlockPos.containing(x, y, z));
		
		for (String stickyFluid : Config.stickyFluids) {
			
			if (stickyFluid.startsWith("#")) {
				// It's a tag
				String tagName = stickyFluid.substring(1);
				matched = currentFluidState.is(FluidTags.create(ResourceLocation.parse(tagName)));
			} else {
				// It's a fluid ID
				matched = currentFluidState.toString().contains(stickyFluid);
			}
			
			if (matched) {
				entity.makeStuckInBlock(Blocks.AIR.defaultBlockState(), new Vec3(0.25, 0.05, 0.25));
				if (entity instanceof ServerPlayer _player) {
					break;
				}
			}

			for (String crudeOil : Config.crudeOils) {
				matched = false;
			
				if (crudeOil.startsWith("#")) {
					// It's a tag
					String tagName = crudeOil.substring(1);
					matched = currentFluidState.is(FluidTags.create(ResourceLocation.parse(tagName)));
				} else {
					// It's a fluid ID
					matched = currentFluidState.toString().contains(stickyFluid);
				}
				if (entity instanceof ServerPlayer _player) {
					AdvancementHolder _adv = _player.server.getAdvancements().get(ResourceLocation.parse("iska_utils:sticky_oil"));
					if (_adv != null) {
						AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
						if (!_ap.isDone()) {
							for (String criteria : _ap.getRemainingCriteria())
								_player.getAdvancements().award(_adv, criteria);
						}
					}
				}
			}
		}
	}
}
