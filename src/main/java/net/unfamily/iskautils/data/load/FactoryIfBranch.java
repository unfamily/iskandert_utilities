package net.unfamily.iskautils.data.load;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import net.unfamily.iskautils.script.EntryIfBranch;
import org.jetbrains.annotations.Nullable;

public record FactoryIfBranch(EntryIfBranch branch, List<FactoryLoader.Output> outputs) {

    public FactoryIfBranch {
        outputs = outputs != null ? List.copyOf(outputs) : List.of();
    }

    public boolean matches(ServerPlayer player, SuspiciousDeliveryStageHost host) {
        return branch.matches(player, host);
    }

    public boolean matchesOffline(
            @Nullable UUID ownerUuid, @Nullable Level level, SuspiciousDeliveryStageHost host) {
        return branch.matchesOffline(ownerUuid, level, host);
    }
}
