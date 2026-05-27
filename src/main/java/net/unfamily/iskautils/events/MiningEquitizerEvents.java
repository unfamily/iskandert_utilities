package net.unfamily.iskautils.events;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.util.MiningEquitizerUtil;

/**
 * Mining Equitizer: remove vanilla underwater / airborne mining penalties reliably via BreakSpeed.
 *
 * This mirrors the "furbetto" approach: modify BreakSpeed instead of relying on fragile redirects.
 */
@EventBusSubscriber
public final class MiningEquitizerEvents {
    private MiningEquitizerEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!MiningEquitizerUtil.hasArtifact(player)) {
            return;
        }

        boolean inWater = player.isEyeInFluid(FluidTags.WATER);
        boolean onGround = player.onGround();

        float penaltyFactor = 1.0F;
        if (inWater) {
            AttributeInstance submerged = player.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
            float submergedFactor = submerged != null ? (float) submerged.getValue() : 1.0F;
            penaltyFactor *= submergedFactor;
        }
        if (!onGround) {
            penaltyFactor *= 0.2F; // vanilla: speed /= 5.0F
        }

        if (penaltyFactor >= 0.99F) {
            return;
        }

        float restored = event.getOriginalSpeed() / penaltyFactor;
        if (restored > event.getNewSpeed()) {
            event.setNewSpeed(restored);
        }
    }
}

