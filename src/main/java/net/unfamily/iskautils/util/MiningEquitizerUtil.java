package net.unfamily.iskautils.util;

import net.minecraft.world.entity.player.Player;
import net.unfamily.iskautils.item.ModItems;

/**
 * Mining Equitizer: active only when equipped in Curios (same rule as other relics).
 */
public final class MiningEquitizerUtil {
    private MiningEquitizerUtil() {}

    /**
     * True when Mining Equitizer is in a Curios slot and not the same stack in vanilla inventory or hands.
     * Same verification as {@link net.unfamily.iskautils.events.CurioEquipStageSync}.
     */
    public static boolean hasArtifact(Player player) {
        return RelicActivationUtil.hasItemActiveInCurioOnly(player, ModItems.MINING_EQUITIZER.get());
    }
}
