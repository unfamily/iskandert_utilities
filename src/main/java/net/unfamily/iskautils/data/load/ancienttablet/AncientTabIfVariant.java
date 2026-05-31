package net.unfamily.iskautils.data.load.ancienttablet;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import net.unfamily.iskautils.script.EntryIfBranch;

import java.util.List;

public record AncientTabIfVariant(
        EntryIfBranch branch,
        List<AncientTabletRequirement> require,
        List<AncientTabletRequirement> produce) {

    public AncientTabIfVariant {
        require = require != null ? List.copyOf(require) : List.of();
        produce = produce != null ? List.copyOf(produce) : List.of();
    }

    public boolean matches(ServerPlayer player, SuspiciousDeliveryStageHost host) {
        return branch.matches(player, host);
    }
}
