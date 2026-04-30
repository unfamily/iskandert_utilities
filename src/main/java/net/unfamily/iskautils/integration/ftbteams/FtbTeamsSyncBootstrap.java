package net.unfamily.iskautils.integration.ftbteams;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.unfamily.iskalib.integration.ftbteams.FtbTeamSnapshot;
import net.unfamily.iskalib.integration.ftbteams.FtbTeamsBridge;
import net.unfamily.iskalib.team.ShopTeamManager;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;

import java.util.Optional;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class FtbTeamsSyncBootstrap {
    private static final int PERIOD_TICKS = 200;
    private static int tickCounter = 0;

    private FtbTeamsSyncBootstrap() {}

    private static boolean enabled() {
        return Config.ftbTeamsSyncEnabled && FtbTeamsBridge.isAvailable();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!enabled()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!enabled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            refreshPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!enabled()) {
            return;
        }
        tickCounter++;
        if (tickCounter % PERIOD_TICKS != 0) {
            return;
        }

        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshPlayer(player);
        }
    }

    private static void refreshPlayer(ServerPlayer player) {
        Optional<FtbTeamSnapshot> snapshotOpt = FtbTeamsBridge.getEffectiveTeamSnapshot(player);
        if (snapshotOpt.isEmpty()) {
            return;
        }
        FtbTeamSnapshot snapshot = snapshotOpt.get();

        ServerLevel level = (ServerLevel) player.level();
        ShopTeamManager teamManager = ShopTeamManager.getInstance(level);
        teamManager.ensureShopTeamForFtb(snapshot.teamKey(), snapshot.displayName(), snapshot.ownerId());
        teamManager.applyExternalTeamSnapshot(snapshot.teamKey(), snapshot.displayName(), snapshot.ownerId(), snapshot.members(), snapshot.assistants());
    }
}

