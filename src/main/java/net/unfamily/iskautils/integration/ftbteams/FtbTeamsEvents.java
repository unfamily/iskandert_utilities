package net.unfamily.iskautils.integration.ftbteams;

import dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerTransferredTeamOwnershipEvent;
import dev.ftb.mods.ftbteams.api.event.TeamCreatedEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.api.event.TeamPropertiesChangedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.shop.ShopTeamManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FtbTeamsEvents {
    private static final String MOD_ID = "ftbteams";
    private static boolean initialized = false;
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    private FtbTeamsEvents() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        if (!ModList.get().isLoaded(MOD_ID) || !Config.ftbTeamsSyncEnabled || !FtbTeamsBridge.isAvailable()) {
            return;
        }

        initialized = true;

        TeamEvent.CREATED.register(FtbTeamsEvents::onTeamCreated);
        TeamEvent.OWNERSHIP_TRANSFERRED.register(FtbTeamsEvents::onOwnershipTransferred);
        TeamEvent.PLAYER_CHANGED.register(FtbTeamsEvents::onPlayerChangedTeam);
        TeamEvent.PROPERTIES_CHANGED.register(FtbTeamsEvents::onTeamPropertiesChanged);
    }

    private static void onTeamCreated(TeamCreatedEvent event) {
        var team = event.getTeam();
        UUID teamId = team.getTeamId();
        UUID owner = team.getOwner();
        if (teamId == null || owner == null || NIL_UUID.equals(owner)) {
            return;
        }
        String displayName = team.getProperty(TeamProperties.DISPLAY_NAME);
        if (displayName == null || displayName.isBlank()) {
            displayName = team.getName().getString();
        }

        MinecraftServer server = FTBTeamsAPI.api() != null && FTBTeamsAPI.api().isManagerLoaded()
                ? FTBTeamsAPI.api().getManager().getServer()
                : null;
        if (server == null || server.overworld() == null) {
            ServerPlayer creator = event.getCreator();
            server = creator != null ? creator.getServer() : null;
        }

        if (server == null || server.overworld() == null) {
            return;
        }

        applySnapshot(server, teamId.toString(), displayName, team);
    }

    private static void onOwnershipTransferred(PlayerTransferredTeamOwnershipEvent event) {
        var team = event.getTeam();
        UUID teamId = team.getTeamId();
        UUID newOwner = team.getOwner();
        if (teamId == null || newOwner == null || NIL_UUID.equals(newOwner)) {
            return;
        }
        String displayName = team.getProperty(TeamProperties.DISPLAY_NAME);
        if (displayName == null || displayName.isBlank()) {
            displayName = team.getName().getString();
        }

        MinecraftServer server = FTBTeamsAPI.api() != null && FTBTeamsAPI.api().isManagerLoaded()
                ? FTBTeamsAPI.api().getManager().getServer()
                : null;
        if (server == null || server.overworld() == null) {
            ServerPlayer from = event.getFrom();
            server = from != null ? from.getServer() : null;
        }

        if (server == null || server.overworld() == null) {
            return;
        }

        applySnapshot(server, teamId.toString(), displayName, team);
    }

    private static void onPlayerChangedTeam(PlayerChangedTeamEvent event) {
        UUID playerId = event.getPlayerId();
        if (playerId == null) {
            return;
        }

        UUID teamId = event.getTeam().getTeamId();
        UUID owner = event.getTeam().getOwner();
        if (teamId == null || owner == null || NIL_UUID.equals(owner)) {
            return;
        }
        String displayName = event.getTeam().getProperty(TeamProperties.DISPLAY_NAME);
        if (displayName == null || displayName.isBlank()) {
            displayName = event.getTeam().getName().getString();
        }

        MinecraftServer server = FTBTeamsAPI.api() != null && FTBTeamsAPI.api().isManagerLoaded()
                ? FTBTeamsAPI.api().getManager().getServer()
                : null;
        if (server == null || server.overworld() == null) {
            // Fallback for online player if provided
            ServerPlayer p = event.getPlayer();
            server = p != null ? p.getServer() : null;
        }

        if (server == null || server.overworld() == null) {
            return;
        }

        ShopTeamManager mgr = ShopTeamManager.getInstance(server.overworld());
        applySnapshot(server, teamId.toString(), displayName, event.getTeam());
        mgr.setPlayerTeamMapping(playerId, teamId.toString());
    }

    private static void onTeamPropertiesChanged(TeamPropertiesChangedEvent event) {
        var team = event.getTeam();
        UUID teamId = team.getTeamId();
        UUID owner = team.getOwner();
        if (teamId == null || owner == null || NIL_UUID.equals(owner)) {
            return;
        }

        String displayName = team.getProperty(TeamProperties.DISPLAY_NAME);
        if (displayName == null || displayName.isBlank()) {
            displayName = team.getName().getString();
        }

        MinecraftServer server = FTBTeamsAPI.api() != null && FTBTeamsAPI.api().isManagerLoaded()
                ? FTBTeamsAPI.api().getManager().getServer()
                : null;
        if (server == null || server.overworld() == null) {
            return;
        }

        applySnapshot(server, teamId.toString(), displayName, team);
    }

    private static void applySnapshot(MinecraftServer server, String teamKey, String displayName, dev.ftb.mods.ftbteams.api.Team team) {
        if (server == null || server.overworld() == null || team == null) {
            return;
        }

        UUID owner = team.getOwner();
        if (owner == null || NIL_UUID.equals(owner)) {
            return;
        }

        var ranks = team.getPlayersByRank(TeamRank.NONE);
        Set<UUID> members = new HashSet<>();
        Set<UUID> assistants = new HashSet<>();
        for (var e : ranks.entrySet()) {
            UUID id = e.getKey();
            TeamRank rank = e.getValue();
            if (id == null || rank == null) {
                continue;
            }
            if (rank.isAtLeast(TeamRank.MEMBER)) {
                members.add(id);
            }
            if (rank.isAtLeast(TeamRank.OFFICER) && !rank.isOwner()) {
                assistants.add(id);
            }
        }

        ShopTeamManager mgr = ShopTeamManager.getInstance(server.overworld());
        mgr.applyFtbTeamSnapshot(teamKey, displayName, owner, members, assistants);
    }
}

