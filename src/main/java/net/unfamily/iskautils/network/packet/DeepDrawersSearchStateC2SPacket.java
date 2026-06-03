package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.block.entity.DeepDrawersBlockEntity;
import net.unfamily.iskautils.client.gui.DeepDrawersMenu;
import org.jetbrains.annotations.NotNull;

/**
 * Syncs Deep Drawers GUI search filter and filter-scroll position to the server
 * so slot interactions cannot reach non-matching entries while filtering.
 */
public class DeepDrawersSearchStateC2SPacket {

    private final BlockPos pos;
    private final String query;
    private final int filterScrollOffset;

    public DeepDrawersSearchStateC2SPacket(@NotNull BlockPos pos, @NotNull String query, int filterScrollOffset) {
        this.pos = pos;
        this.query = query == null ? "" : query;
        this.filterScrollOffset = filterScrollOffset;
    }

    public void handle(ServerPlayer player) {
        if (player == null || player.level() == null) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof DeepDrawersBlockEntity)) {
            return;
        }
        if (!(player.containerMenu instanceof DeepDrawersMenu menu)) {
            return;
        }

        menu.applySearchFilter(query, player.level().registryAccess());
        if (menu.isSearchFilterActive()) {
            menu.setFilterScrollOffset(filterScrollOffset);
        }
    }
}
