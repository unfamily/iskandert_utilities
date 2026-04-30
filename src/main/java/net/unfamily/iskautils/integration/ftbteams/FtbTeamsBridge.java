package net.unfamily.iskautils.integration.ftbteams;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

public final class FtbTeamsBridge {
    private static final String MOD_ID = "ftbteams";
    private static final String API_CLASS = "dev.ftb.mods.ftbteams.api.FTBTeamsAPI";
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public record FtbTeamInfo(String teamKey, String displayName, UUID ownerId) {
    }

    private FtbTeamsBridge() {
    }

    public static boolean isAvailable() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        try {
            Object api = apiInstanceOrNull();
            return api != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Returns the effective team info for the given player (party if present, otherwise personal team).
     * teamKey is {@code team.getTeamId().toString()}.
     */
    public static Optional<FtbTeamInfo> getEffectiveTeamInfo(ServerPlayer player) {
        if (player == null || !ModList.get().isLoaded(MOD_ID)) {
            return Optional.empty();
        }
        try {
            Object api = apiInstanceOrNull();
            if (api == null) {
                return Optional.empty();
            }

            Method isManagerLoaded = api.getClass().getMethod("isManagerLoaded");
            boolean loaded = (boolean) isManagerLoaded.invoke(api);
            if (!loaded) {
                return Optional.empty();
            }

            Method getManager = api.getClass().getMethod("getManager");
            Object manager = getManager.invoke(api);
            if (manager == null) {
                return Optional.empty();
            }

            Method getTeamForPlayer = manager.getClass().getMethod("getTeamForPlayer", ServerPlayer.class);
            Object optTeam = getTeamForPlayer.invoke(manager, player);
            if (optTeam == null) {
                return Optional.empty();
            }

            Method isPresent = optTeam.getClass().getMethod("isPresent");
            boolean present = (boolean) isPresent.invoke(optTeam);
            if (!present) {
                return Optional.empty();
            }

            Method get = optTeam.getClass().getMethod("get");
            Object team = get.invoke(optTeam);
            if (team == null) {
                return Optional.empty();
            }

            UUID teamId = invokeUuid(team, "getTeamId");
            if (teamId == null || NIL_UUID.equals(teamId)) {
                return Optional.empty();
            }

            UUID ownerId = invokeUuid(team, "getOwner");
            if (ownerId == null || NIL_UUID.equals(ownerId)) {
                // In some cases (notably during early init / singleplayer), FTB may temporarily expose NIL owner.
                // If the player is OWNER rank for this team, treat the player as owner to allow shop team creation.
                if (isPlayerOwnerOfTeam(team, player.getUUID())) {
                    ownerId = player.getUUID();
                } else {
                    return Optional.empty();
                }
            }

            String displayName = resolveDisplayName(team);
            if (displayName == null || displayName.isBlank()) {
                displayName = resolveNameString(team);
            }

            return Optional.of(new FtbTeamInfo(teamId.toString(), displayName != null ? displayName : teamId.toString(), ownerId));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private static boolean isPlayerOwnerOfTeam(Object team, UUID playerId) {
        try {
            Method getRankForPlayer = team.getClass().getMethod("getRankForPlayer", UUID.class);
            Object rank = getRankForPlayer.invoke(team, playerId);
            if (rank == null) {
                return false;
            }
            Method isOwner = rank.getClass().getMethod("isOwner");
            Object v = isOwner.invoke(rank);
            return v instanceof Boolean && (Boolean) v;
        } catch (Throwable t) {
            return false;
        }
    }

    private static UUID invokeUuid(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return v instanceof UUID ? (UUID) v : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String resolveDisplayName(Object team) {
        try {
            // TeamProperties.DISPLAY_NAME (static field)
            Class<?> propsClass = Class.forName("dev.ftb.mods.ftbteams.api.property.TeamProperties");
            Object displayNameProp = propsClass.getField("DISPLAY_NAME").get(null);
            if (displayNameProp == null) {
                return null;
            }

            // Team.getProperty(TeamProperty)
            Method getProperty = null;
            for (Method m : team.getClass().getMethods()) {
                if (m.getName().equals("getProperty") && m.getParameterCount() == 1) {
                    getProperty = m;
                    break;
                }
            }
            if (getProperty == null) {
                return null;
            }

            Object v = getProperty.invoke(team, displayNameProp);
            return v instanceof String ? (String) v : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String resolveNameString(Object team) {
        try {
            Method getName = team.getClass().getMethod("getName");
            Object component = getName.invoke(team);
            if (component == null) {
                return null;
            }
            Method getString = component.getClass().getMethod("getString");
            Object s = getString.invoke(component);
            return s instanceof String ? (String) s : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Optional<FtbTeamInfo> getTeamInfoByTeamKey(String teamKey) {
        if (teamKey == null || teamKey.isBlank() || !ModList.get().isLoaded(MOD_ID)) {
            return Optional.empty();
        }
        try {
            UUID teamId = UUID.fromString(teamKey);
            Object api = apiInstanceOrNull();
            if (api == null) {
                return Optional.empty();
            }

            Method isManagerLoaded = api.getClass().getMethod("isManagerLoaded");
            boolean loaded = (boolean) isManagerLoaded.invoke(api);
            if (!loaded) {
                return Optional.empty();
            }

            Method getManager = api.getClass().getMethod("getManager");
            Object manager = getManager.invoke(api);
            if (manager == null) {
                return Optional.empty();
            }

            Method getTeamByID = manager.getClass().getMethod("getTeamByID", UUID.class);
            Object optTeam = getTeamByID.invoke(manager, teamId);
            if (optTeam == null) {
                return Optional.empty();
            }

            Method isPresent = optTeam.getClass().getMethod("isPresent");
            boolean present = (boolean) isPresent.invoke(optTeam);
            if (!present) {
                return Optional.empty();
            }

            Method get = optTeam.getClass().getMethod("get");
            Object team = get.invoke(optTeam);
            if (team == null) {
                return Optional.empty();
            }

            UUID ownerId = invokeUuid(team, "getOwner");
            if (ownerId == null || NIL_UUID.equals(ownerId)) {
                return Optional.empty();
            }

            String displayName = resolveDisplayName(team);
            if (displayName == null || displayName.isBlank()) {
                displayName = resolveNameString(team);
            }

            return Optional.of(new FtbTeamInfo(teamId.toString(), displayName != null ? displayName : teamId.toString(), ownerId));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private static Object apiInstanceOrNull() throws Exception {
        Class<?> apiClass = Class.forName(API_CLASS);
        Method apiMethod = apiClass.getMethod("api");
        return apiMethod.invoke(null);
    }
}

