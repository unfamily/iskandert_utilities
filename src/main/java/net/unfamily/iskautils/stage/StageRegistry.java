package net.unfamily.iskautils.stage;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.unfamily.iskautils.shop.ShopTeamManager;
import org.slf4j.Logger;

import java.util.*;


/**
 * Registry for game stages (player, world, team)
 */
public class StageRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLAYER_STAGES_OBJECTIVE = "iska_player_stage";
    private static final String WORLD_STAGE_DATA_NAME = "iska_utils_world_stages";
    private static final String TEAM_STAGE_DATA_NAME = "iska_utils_team_stages";
    
    private static StageRegistry INSTANCE;
    
    private final MinecraftServer server;
    
    /**
     * Creates a new stage registry for the given server
     */
    private StageRegistry(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Gets the singleton instance
     */
    public static StageRegistry getInstance(MinecraftServer server) {
        if (INSTANCE == null || INSTANCE.server != server) {
            INSTANCE = new StageRegistry(server);
        }
        return INSTANCE;
    }
    
    /**
     * Ensures the player stages scoreboard objective exists
     */
    private void ensurePlayerObjectiveExists() {
        Scoreboard scoreboard = server.getScoreboard();
        if (scoreboard.getObjective(PLAYER_STAGES_OBJECTIVE) == null) {
            scoreboard.addObjective(
                PLAYER_STAGES_OBJECTIVE, 
                ObjectiveCriteria.DUMMY, 
                Component.literal("Player Stages"), 
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null
            );
            LOGGER.info("Created player stages scoreboard objective: {}", PLAYER_STAGES_OBJECTIVE);
        }
    }
    
    /**
     * Checks if a player has a specific stage
     */
    public boolean hasPlayerStage(ServerPlayer player, String stage) {
        PlayerStageData data = getPlayerStageData(player);
        return data != null && data.hasStage(stage);
    }
    
    /**
     * Checks if the world has a specific stage
     */
    public boolean hasWorldStage(String stage) {
        WorldStageData data = getWorldStageData(server.getLevel(Level.OVERWORLD));
        return data != null && data.hasStage(stage);
    }
    
    /**
     * Checks if a team has a specific stage
     */
    public boolean hasTeamStage(String teamName, String stage) {
        TeamStageData data = getTeamStageData(server.getLevel(Level.OVERWORLD));
        return data != null && data.hasTeamStage(teamName, stage);
    }
    
    /**
     * Checks if the player's team has a specific stage
     */
    public boolean hasPlayerTeamStage(ServerPlayer player, String stage) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        if (teamName == null) {
            return false; // Player not in a team
        }
        return hasTeamStage(teamName, stage);
    }
    
    /**
     * Gets player stage data
     */
    private PlayerStageData getPlayerStageData(ServerPlayer player) {
        // Usa le NBT del giocatore
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains("iskautils")) {
            persistentData.put("iskautils", new CompoundTag());
        }
        
        CompoundTag iskaData = persistentData.getCompound("iskautils");
        if (!iskaData.contains("stages")) {
            iskaData.put("stages", new ListTag());
            persistentData.put("iskautils", iskaData);
        }
        
        return new PlayerStageData(player);
    }
    
    /**
     * Gets world stage data
     */
    private WorldStageData getWorldStageData(ServerLevel level) {
        if (level == null) {
            LOGGER.warn("Overworld not available, can't access world stages");
            return null;
        }
        
        // Usa SavedData.Factory come in MCreator
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(WorldStageData::new, WorldStageData::load),
            WORLD_STAGE_DATA_NAME
        );
    }
    
    /**
     * Gets team stage data
     */
    public TeamStageData getTeamStageData(ServerLevel level) {
        if (level == null) {
            LOGGER.warn("Overworld not available, can't access team stages");
            return null;
        }
        
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(TeamStageData::new, TeamStageData::load),
            TEAM_STAGE_DATA_NAME
        );
    }
    
    /**
     * Sets a player stage
     */
    public boolean setPlayerStage(ServerPlayer player, String stage, boolean value) {
        return setPlayerStage(player, stage, value, false);
    }

    /**
     * Sets a player stage with option to hide the log entry
     */
    public boolean setPlayerStage(ServerPlayer player, String stage, boolean value, boolean hideLog) {
        PlayerStageData data = getPlayerStageData(player);
        if (data == null) {
            return false;
        }
        
        if (value) {
            data.addStage(stage);
        } else {
            data.removeStage(stage);
        }
        
        if (!hideLog) {
            LOGGER.info("Set player stage '{}' to {} for player {}", stage, value, player.getName().getString());
        }
        return true;
    }
    
    /**
     * Sets a world stage
     */
    public boolean setWorldStage(String stage, boolean value) {
        return setWorldStage(stage, value, false);
    }

    /**
     * Sets a world stage with option to hide the log entry
     */
    public boolean setWorldStage(String stage, boolean value, boolean hideLog) {
        WorldStageData data = getWorldStageData(server.getLevel(Level.OVERWORLD));
        if (data == null) {
            LOGGER.error("Failed to access world stage data");
            return false;
        }
        
        if (value) {
            data.addStage(stage);
        } else {
            data.removeStage(stage);
        }
        
        data.setDirty();
        if (!hideLog) {
            LOGGER.info("Set world stage '{}' to {}", stage, value);
        }
        return true;
    }
    
    /**
     * Sets a team stage
     */
    public boolean setTeamStage(String teamName, String stage, boolean value) {
        return setTeamStage(teamName, stage, value, false);
    }

    /**
     * Sets a team stage with option to hide the log entry
     */
    public boolean setTeamStage(String teamName, String stage, boolean value, boolean hideLog) {
        TeamStageData data = getTeamStageData(server.getLevel(Level.OVERWORLD));
        if (data == null) {
            LOGGER.error("Failed to access team stage data");
            return false;
        }
        
        if (value) {
            data.addTeamStage(teamName, stage);
        } else {
            data.removeTeamStage(teamName, stage);
        }
        
        data.setDirty();
        if (!hideLog) {
            LOGGER.info("Set team stage '{}' to {} for team '{}'", stage, value, teamName);
        }
        return true;
    }
    
    /**
     * Sets a stage for the player's team
     */
    public boolean setPlayerTeamStage(ServerPlayer player, String stage, boolean value) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        if (teamName == null) {
            LOGGER.warn("Player {} is not in a team, cannot set team stage", player.getName().getString());
            return false;
        }
        return setTeamStage(teamName, stage, value);
    }
    
    /**
     * Gets all player stages for a player
     */
    public List<String> getPlayerStages(ServerPlayer player) {
        PlayerStageData data = getPlayerStageData(player);
        if (data == null) {
            return Collections.emptyList();
        }
        return data.getStages();
    }
    
    /**
     * Gets all world stages
     */
    public List<String> getWorldStages() {
        WorldStageData data = getWorldStageData(server.getLevel(Level.OVERWORLD));
        if (data == null) {
            return Collections.emptyList();
        }
        return data.getStages();
    }
    
    /**
     * Gets all team stages for a specific team
     */
    public List<String> getTeamStages(String teamName) {
        TeamStageData data = getTeamStageData(server.getLevel(Level.OVERWORLD));
        if (data == null) {
            return Collections.emptyList();
        }
        return data.getTeamStages(teamName);
    }
    
    /**
     * Gets all team stages for the player's team
     */
    public List<String> getPlayerTeamStages(ServerPlayer player) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        if (teamName == null) {
            return Collections.emptyList();
        }
        return getTeamStages(teamName);
    }
    
    /**
     * Gets all registered stages (across all types)
     */
    public Set<String> getAllRegisteredStages() {
        Set<String> result = new HashSet<>();
        
        // Add world stages
        result.addAll(getWorldStages());
        
        // Add team stages
        TeamStageData teamData = getTeamStageData(server.getLevel(Level.OVERWORLD));
        if (teamData != null) {
            result.addAll(teamData.getAllTeamStages());
        }
        
        // Add player stages - this would require scanning all players
        // which might be expensive, so we'll skip it for now
        
        return result;
    }
    
    // ===== METODI STATICI PER FACILITARE L'INTEGRAZIONE CON SCRIPT =====
    
    /**
     * Checks if a player has a specific stage (static method for scripts)
     * @param player The player entity
     * @param stage The stage name
     * @return True if the player has the stage
     */
    public static boolean playerHasStage(Entity player, String stage) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }
        
        MinecraftServer server = ((ServerPlayer) player).getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).hasPlayerStage(serverPlayer, stage);
    }
    
    /**
     * Checks if the world has a specific stage (static method for scripts)
     * @param level The level/world
     * @param stage The stage name
     * @return True if the world has the stage
     */
    public static boolean worldHasStage(LevelAccessor level, String stage) {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
            return false;
        }
        
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).hasWorldStage(stage);
    }
    
    /**
     * Checks if a team has a specific stage (static method for scripts)
     * @param level The level/world
     * @param teamName The team name
     * @param stage The stage name
     * @return True if the team has the stage
     */
    public static boolean teamHasStage(LevelAccessor level, String teamName, String stage) {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
            return false;
        }
        
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).hasTeamStage(teamName, stage);
    }
    
    /**
     * Checks if the player's team has a specific stage (static method for scripts)
     * @param player The player entity
     * @param stage The stage name
     * @return True if the player's team has the stage
     */
    public static boolean playerTeamHasStage(Entity player, String stage) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }
        
        MinecraftServer server = ((ServerPlayer) player).getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).hasPlayerTeamStage(serverPlayer, stage);
    }
    
    /**
     * Adds a stage to a player (static method for scripts)
     * @param player The player entity
     * @param stage The stage name
     * @return True if successful
     */
    public static boolean addPlayerStage(Entity player, String stage) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }
        
        MinecraftServer server = ((ServerPlayer) player).getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setPlayerStage(serverPlayer, stage, true, false);
    }

    /**
     * Adds a stage to a player (static method for scripts) with option to hide the log entry
     */
    public static boolean addPlayerStage(Entity player, String stage, boolean hideLog) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }
        
        MinecraftServer server = ((ServerPlayer) player).getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setPlayerStage(serverPlayer, stage, true, hideLog);
    }
    
    /**
     * Removes a stage from a player (static method for scripts)
     * @param player The player entity
     * @param stage The stage name
     * @return True if successful
     */
    public static boolean removePlayerStage(Entity player, String stage) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }
        
        MinecraftServer server = ((ServerPlayer) player).getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setPlayerStage(serverPlayer, stage, false, false);
    }

    /**
     * Removes a stage from a player (static method for scripts) with option to hide the log entry
     */
    public static boolean removePlayerStage(Entity player, String stage, boolean hideLog) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }
        
        MinecraftServer server = ((ServerPlayer) player).getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setPlayerStage(serverPlayer, stage, false, hideLog);
    }
    
    /**
     * Adds a stage to the world (static method for scripts)
     * @param level The level/world
     * @param stage The stage name
     * @return True if successful
     */
    public static boolean addWorldStage(LevelAccessor level, String stage) {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
            return false;
        }
        
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setWorldStage(stage, true);
    }
    
    /**
     * Removes a stage from the world (static method for scripts)
     * @param level The level/world
     * @param stage The stage name
     * @return True if successful
     */
    public static boolean removeWorldStage(LevelAccessor level, String stage) {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
            return false;
        }
        
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setWorldStage(stage, false);
    }
    
    /**
     * Adds a stage to a team (static method for scripts)
     * @param level The level/world
     * @param teamName The team name
     * @param stage The stage name
     * @return True if successful
     */
    public static boolean addTeamStage(LevelAccessor level, String teamName, String stage) {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
            return false;
        }
        
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setTeamStage(teamName, stage, true);
    }
    
    /**
     * Removes a stage from a team (static method for scripts)
     * @param level The level/world
     * @param teamName The team name
     * @param stage The stage name
     * @return True if successful
     */
    public static boolean removeTeamStage(LevelAccessor level, String teamName, String stage) {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
            return false;
        }
        
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setTeamStage(teamName, stage, false);
    }
    
    /**
     * Adds a stage to the player's team (static method for scripts)
     * @param player The player entity
     * @param stage The stage name
     * @return True if successful
     */
    public static boolean addPlayerTeamStage(Entity player, String stage) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }
        
        MinecraftServer server = ((ServerPlayer) player).getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setPlayerTeamStage(serverPlayer, stage, true);
    }
    
    /**
     * Removes a stage from the player's team (static method for scripts)
     * @param player The player entity
     * @param stage The stage name
     * @return True if successful
     */
    public static boolean removePlayerTeamStage(Entity player, String stage) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }
        
        MinecraftServer server = ((ServerPlayer) player).getServer();
        if (server == null) {
            return false;
        }
        
        return getInstance(server).setPlayerTeamStage(serverPlayer, stage, false);
    }
    
    /**
     * Player stage data stored in player NBT
     */
    private static class PlayerStageData {
        private final ServerPlayer player;
        
        public PlayerStageData(ServerPlayer player) {
            this.player = player;
        }
        
        /**
         * Checks if the player has a specific stage
         */
        public boolean hasStage(String stage) {
            ListTag stagesList = getStagesList();
            for (int i = 0; i < stagesList.size(); i++) {
                if (stagesList.getString(i).equals(stage)) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Gets the list of all stages the player has
         */
        public List<String> getStages() {
            List<String> result = new ArrayList<>();
            ListTag stagesList = getStagesList();
            for (int i = 0; i < stagesList.size(); i++) {
                result.add(stagesList.getString(i));
            }
            return result;
        }
        
        /**
         * Adds a stage to the player
         */
        public void addStage(String stage) {
            if (!hasStage(stage)) {
                ListTag stagesList = getStagesList();
                stagesList.add(StringTag.valueOf(stage));
                saveStagesList(stagesList);
            } else {
                
            }
        }
        
        /**
         * Removes a stage from the player
         */
        public void removeStage(String stage) {
            ListTag stagesList = getStagesList();
            ListTag newList = new ListTag();
            boolean removed = false;
            
            for (int i = 0; i < stagesList.size(); i++) {
                String current = stagesList.getString(i);
                if (!current.equals(stage)) {
                    newList.add(StringTag.valueOf(current));
                } else {
                    removed = true;
                }
            }
            
            saveStagesList(newList);
            
        }
        
        /**
         * Gets the stages list from player data
         */
        private ListTag getStagesList() {
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag iskaData = persistentData.getCompound("iskautils");
            ListTag stagesList = iskaData.getList("stages", 8); // 8 = string tag type
            return stagesList;
        }
        
        /**
         * Saves the stages list to player data
         */
        private void saveStagesList(ListTag stagesList) {
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag iskaData = persistentData.getCompound("iskautils");
            iskaData.put("stages", stagesList);
            persistentData.put("iskautils", iskaData);
        }
        
        /**
         * Imposta un valore float per il giocatore
         */
        public void setPlayerFloatValue(String key, float value) {
            CompoundTag persistentData = player.getPersistentData();
            if (!persistentData.contains("iskautils")) {
                persistentData.put("iskautils", new CompoundTag());
            }
            
            CompoundTag iskaData = persistentData.getCompound("iskautils");
            if (!iskaData.contains("floatValues")) {
                iskaData.put("floatValues", new CompoundTag());
            }
            
            CompoundTag floatValues = iskaData.getCompound("floatValues");
            floatValues.putFloat(key, value);
            iskaData.put("floatValues", floatValues);
            persistentData.put("iskautils", iskaData);
        }
        
        /**
         * Ottiene un valore float per il giocatore
         */
        public float getPlayerFloatValue(String key, float defaultValue) {
            CompoundTag persistentData = player.getPersistentData();
            if (!persistentData.contains("iskautils")) {
                return defaultValue;
            }
            
            CompoundTag iskaData = persistentData.getCompound("iskautils");
            if (!iskaData.contains("floatValues")) {
                return defaultValue;
            }
            
            CompoundTag floatValues = iskaData.getCompound("floatValues");
            return floatValues.contains(key) ? floatValues.getFloat(key) : defaultValue;
        }
        
        /**
         * Rimuove un valore float per il giocatore
         */
        public void removePlayerFloatValue(String key) {
            CompoundTag persistentData = player.getPersistentData();
            if (!persistentData.contains("iskautils")) {
                return;
            }
            
            CompoundTag iskaData = persistentData.getCompound("iskautils");
            if (!iskaData.contains("floatValues")) {
                return;
            }
            
            CompoundTag floatValues = iskaData.getCompound("floatValues");
            floatValues.remove(key);
            iskaData.put("floatValues", floatValues);
            persistentData.put("iskautils", iskaData);
        }
    }
    
    /**
     * World stage data saved in world data
     */
    public static class WorldStageData extends SavedData {
        private final List<String> stages = new ArrayList<>();
        
        public WorldStageData() {
        }
        
        public static WorldStageData load(CompoundTag tag, HolderLookup.Provider provider) {
            WorldStageData data = new WorldStageData();
            
            if (tag.contains("stages")) {
                ListTag stagesList = tag.getList("stages", 8); // 8 is the NBT tag type for String
                for (int i = 0; i < stagesList.size(); i++) {
                    data.stages.add(stagesList.getString(i));
                }
            }
            
            return data;
        }
        
        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            ListTag stagesList = new ListTag();
            for (String stage : stages) {
                stagesList.add(StringTag.valueOf(stage));
            }
            tag.put("stages", stagesList);
            
            return tag;
        }
        
        /**
         * Check if a stage exists
         */
        public boolean hasStage(String stage) {
            return stages.contains(stage);
        }
        
        /**
         * Add a stage
         */
        public void addStage(String stage) {
            if (!stages.contains(stage)) {
                stages.add(stage);
                setDirty();
            }
        }
        
        /**
         * Remove a stage
         */
        public void removeStage(String stage) {
            if (stages.remove(stage)) {
                setDirty();
            }
        }
        
        /**
         * Get all stages
         */
        public List<String> getStages() {
            return new ArrayList<>(stages);
        }
    }
    
    /**
     * Team stage data saved in world data
     */
    public static class TeamStageData extends SavedData {
        private final Map<String, List<String>> teamStages = new HashMap<>();
        
        public TeamStageData() {
        }
        
        public static TeamStageData load(CompoundTag tag, HolderLookup.Provider provider) {
            TeamStageData data = new TeamStageData();
            
            if (tag.contains("team_stages")) {
                CompoundTag teamsTag = tag.getCompound("team_stages");
                for (String teamName : teamsTag.getAllKeys()) {
                    ListTag stagesList = teamsTag.getList(teamName, 8); // 8 is the NBT tag type for String
                    List<String> stages = new ArrayList<>();
                    for (int i = 0; i < stagesList.size(); i++) {
                        stages.add(stagesList.getString(i));
                    }
                    data.teamStages.put(teamName, stages);
                }
            }
            
            return data;
        }
        
        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            CompoundTag teamsTag = new CompoundTag();
            
            for (Map.Entry<String, List<String>> entry : teamStages.entrySet()) {
                ListTag stagesList = new ListTag();
                for (String stage : entry.getValue()) {
                    stagesList.add(StringTag.valueOf(stage));
                }
                teamsTag.put(entry.getKey(), stagesList);
            }
            
            tag.put("team_stages", teamsTag);
            return tag;
        }
        
        /**
         * Check if a team has a specific stage
         */
        public boolean hasTeamStage(String teamName, String stage) {
            List<String> stages = teamStages.get(teamName);
            return stages != null && stages.contains(stage);
        }
        
        /**
         * Add a stage to a team
         */
        public void addTeamStage(String teamName, String stage) {
            teamStages.computeIfAbsent(teamName, k -> new ArrayList<>());
            List<String> stages = teamStages.get(teamName);
            if (!stages.contains(stage)) {
                stages.add(stage);
                setDirty();
                
            } else {
                
            }
        }
        
        /**
         * Remove a stage from a team
         */
        public void removeTeamStage(String teamName, String stage) {
            List<String> stages = teamStages.get(teamName);
            if (stages != null && stages.remove(stage)) {
                setDirty();
                
            }
        }
        
        /**
         * Get all stages for a specific team
         */
        public List<String> getTeamStages(String teamName) {
            List<String> stages = teamStages.get(teamName);
            return stages != null ? new ArrayList<>(stages) : new ArrayList<>();
        }
        
        /**
         * Get all team stages across all teams
         */
        public Set<String> getAllTeamStages() {
            Set<String> allStages = new HashSet<>();
            for (List<String> stages : teamStages.values()) {
                allStages.addAll(stages);
            }
            return allStages;
        }
        
        /**
         * Get all teams that have stages
         */
        public Set<String> getAllTeams() {
            return new HashSet<>(teamStages.keySet());
        }
        
        /**
         * Remove all stages for a team (when team is deleted)
         */
        public void removeTeam(String teamName) {
            if (teamStages.remove(teamName) != null) {
                setDirty();
                
            }
        }
    }
} 