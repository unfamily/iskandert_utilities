package net.unfamily.iskautils.data.load;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import net.unfamily.iskautils.script.EntryIfBranch;

import java.util.List;

public record FactoryIfBranch(EntryIfBranch branch, List<FactoryLoader.Output> outputs) {

    public FactoryIfBranch {
        outputs = outputs != null ? List.copyOf(outputs) : List.of();
    }

    public boolean matches(ServerPlayer player, SuspiciousDeliveryStageHost host) {
        return branch.matches(player, host);
    }
}
