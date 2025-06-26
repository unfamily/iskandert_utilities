package net.unfamily.iskautils.shop;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;
import org.slf4j.Logger;

import java.util.*;

/**
 * Manages shop teams for sharing valutes between team members
 */
public class ShopTeamManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEAM_DATA_NAME = "iska_utils_shop_teams";
    
    private static ShopTeamManager INSTANCE;
    private final ServerLevel level;
    
    private ShopTeamManager(ServerLevel level) {
        this.level = level;
    }
    
    /**
     * Gets the singleton instance
     */
    public static ShopTeamManager getInstance(ServerLevel level) {
        if (INSTANCE == null || INSTANCE.level != level) {
            INSTANCE = new ShopTeamManager(level);
        }
        return INSTANCE;
    }
    
    /**
     * Gets the team data from world storage
     */
    private TeamData getTeamData() {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(TeamData::new, TeamData::load),
            TEAM_DATA_NAME
        );
    }
    
    /**
     * Creates a new team
     */
    public boolean createTeam(String teamName, ServerPlayer leader) {
        TeamData data = getTeamData();
        return data.createTeam(teamName, leader.getUUID());
    }
    
    /**
     * Deletes a team (only leader can do this)
     */
    public boolean deleteTeam(String teamName, ServerPlayer player) {
        TeamData data = getTeamData();
        return data.deleteTeam(teamName, player.getUUID());
    }
    
    /**
     * Adds a player to a team
     */
    public boolean addPlayerToTeam(String teamName, ServerPlayer player) {
        TeamData data = getTeamData();
        return data.addPlayerToTeam(teamName, player.getUUID());
    }
    
    /**
     * Removes a player from a team
     */
    public boolean removePlayerFromTeam(String teamName, ServerPlayer player) {
        TeamData data = getTeamData();
        return data.removePlayerFromTeam(teamName, player.getUUID());
    }
    
    /**
     * Gets the team a player belongs to
     */
    public String getPlayerTeam(ServerPlayer player) {
        TeamData data = getTeamData();
        return data.getPlayerTeam(player.getUUID());
    }
    
    /**
     * Gets all members of a team
     */
    public List<UUID> getTeamMembers(String teamName) {
        TeamData data = getTeamData();
        return data.getTeamMembers(teamName);
    }
    
    /**
     * Gets the leader of a team
     */
    public UUID getTeamLeader(String teamName) {
        TeamData data = getTeamData();
        return data.getTeamLeader(teamName);
    }
    
    /**
     * Adds valutes to a team's balance
     */
    public boolean addTeamValutes(String teamName, String valuteId, double amount) {
        TeamData data = getTeamData();
        return data.addTeamValutes(teamName, valuteId, amount);
    }
    
    /**
     * Removes valutes from a team's balance
     */
    public boolean removeTeamValutes(String teamName, String valuteId, double amount) {
        TeamData data = getTeamData();
        return data.removeTeamValutes(teamName, valuteId, amount);
    }
    
    /**
     * Gets a team's valute balance
     */
    public double getTeamValuteBalance(String teamName, String valuteId) {
        TeamData data = getTeamData();
        return data.getTeamValuteBalance(teamName, valuteId);
    }
    
    /**
     * Gets all teams
     */
    public Set<String> getAllTeams() {
        TeamData data = getTeamData();
        return data.getAllTeams();
    }
    
    /**
     * Checks if a player is in a team
     */
    public boolean isPlayerInTeam(ServerPlayer player) {
        return getPlayerTeam(player) != null;
    }
    
    /**
     * Checks if a player is the leader of a team
     */
    public boolean isPlayerTeamLeader(ServerPlayer player, String teamName) {
        UUID leader = getTeamLeader(teamName);
        return leader != null && leader.equals(player.getUUID());
    }
    
    /**
     * Checks if a player is an assistant of a team
     */
    public boolean isPlayerTeamAssistant(ServerPlayer player, String teamName) {
        TeamData data = getTeamData();
        return data.isPlayerTeamAssistant(player.getUUID(), teamName);
    }
    
    /**
     * Checks if a player has permission to modify a team (leader or assistant)
     */
    public boolean canModifyTeam(ServerPlayer player, String teamName) {
        return isPlayerTeamLeader(player, teamName) || isPlayerTeamAssistant(player, teamName);
    }
    
    /**
     * Invites a player to a team
     */
    public boolean invitePlayerToTeam(String teamName, ServerPlayer inviter, ServerPlayer invitee) {
        TeamData data = getTeamData();
        return data.invitePlayerToTeam(teamName, inviter.getUUID(), invitee.getUUID());
    }
    
    /**
     * Accepts a team invitation
     */
    public boolean acceptTeamInvitation(ServerPlayer player, String teamName) {
        TeamData data = getTeamData();
        return data.acceptTeamInvitation(player.getUUID(), teamName);
    }
    
    /**
     * Gets pending invitations for a player
     */
    public List<String> getPlayerInvitations(ServerPlayer player) {
        TeamData data = getTeamData();
        return data.getPlayerInvitations(player.getUUID());
    }
    
    /**
     * Leaves the current team
     */
    public boolean leaveTeam(ServerPlayer player) {
        TeamData data = getTeamData();
        return data.leaveTeam(player.getUUID());
    }
    
    /**
     * Transfers leadership to another player
     */
    public boolean transferLeadership(String teamName, ServerPlayer currentLeader, ServerPlayer newLeader) {
        TeamData data = getTeamData();
        return data.transferLeadership(teamName, currentLeader.getUUID(), newLeader.getUUID());
    }
    
    /**
     * Adds an assistant to a team
     */
    public boolean addTeamAssistant(String teamName, ServerPlayer leader, ServerPlayer assistant) {
        TeamData data = getTeamData();
        return data.addTeamAssistant(teamName, leader.getUUID(), assistant.getUUID());
    }
    
    /**
     * Removes an assistant from a team
     */
    public boolean removeTeamAssistant(String teamName, ServerPlayer leader, ServerPlayer assistant) {
        TeamData data = getTeamData();
        return data.removeTeamAssistant(teamName, leader.getUUID(), assistant.getUUID());
    }
    
    /**
     * Gets all assistants of a team
     */
    public List<UUID> getTeamAssistants(String teamName) {
        TeamData data = getTeamData();
        return data.getTeamAssistants(teamName);
    }
    
    /**
     * Renames a team
     */
    public boolean renameTeam(String oldTeamName, String newTeamName, ServerPlayer player) {
        TeamData data = getTeamData();
        return data.renameTeam(oldTeamName, newTeamName, player.getUUID());
    }
    
    /**
     * Gets a team by its unique ID
     */
    public Team getTeamById(UUID teamId) {
        TeamData data = getTeamData();
        return data.getTeamById(teamId);
    }
    
    /**
     * Gets a team's unique ID by name
     */
    public UUID getTeamIdByName(String teamName) {
        TeamData data = getTeamData();
        return data.getTeamIdByName(teamName);
    }
    
    /**
     * Gets a team's name by its unique ID
     */
    public String getTeamNameById(UUID teamId) {
        Team team = getTeamById(teamId);
        return team != null ? team.getName() : null;
    }
    
    /**
     * Gets a player's team by UUID
     */
    public String getPlayerTeam(UUID playerId) {
        return getTeamData().getPlayerTeam(playerId);
    }
    
    /**
     * Gets all team names for autocompletion
     */
    public List<String> getAllTeamNames() {
        return new ArrayList<>(getAllTeams());
    }
    
    /**
     * Team data saved in world storage
     */
    public static class TeamData extends SavedData {
        private final Map<String, Team> teams = new HashMap<>();
        private final Map<UUID, Team> teamsById = new HashMap<>(); // Mappa per accesso tramite ID
        private final Map<UUID, String> playerTeams = new HashMap<>();
        private final Map<UUID, Set<String>> playerInvitations = new HashMap<>();
        
        public TeamData() {
        }
        
        public static TeamData load(CompoundTag tag, HolderLookup.Provider provider) {
            TeamData data = new TeamData();
            
            if (tag.contains("teams")) {
                CompoundTag teamsTag = tag.getCompound("teams");
                for (String teamName : teamsTag.getAllKeys()) {
                    CompoundTag teamTag = teamsTag.getCompound(teamName);
                    Team team = Team.load(teamTag);
                    data.teams.put(teamName, team);
                    data.teamsById.put(team.getTeamId(), team); // Popola la mappa per ID
                    
                    // Update player-team mapping
                    for (UUID playerId : team.getMembers()) {
                        data.playerTeams.put(playerId, teamName);
                    }
                }
            }
            
            if (tag.contains("invitations")) {
                CompoundTag invitationsTag = tag.getCompound("invitations");
                for (String playerIdStr : invitationsTag.getAllKeys()) {
                    try {
                        UUID playerId = UUID.fromString(playerIdStr);
                        ListTag teamList = invitationsTag.getList(playerIdStr, 8); // String tag type
                        Set<String> invitations = new HashSet<>();
                        for (int i = 0; i < teamList.size(); i++) {
                            invitations.add(teamList.getString(i));
                        }
                        data.playerInvitations.put(playerId, invitations);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid UUID in invitations: {}", playerIdStr);
                    }
                }
            }
            
            return data;
        }
        
        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            CompoundTag teamsTag = new CompoundTag();
            
            for (Map.Entry<String, Team> entry : teams.entrySet()) {
                teamsTag.put(entry.getKey(), entry.getValue().save());
            }
            
            tag.put("teams", teamsTag);
            
            CompoundTag invitationsTag = new CompoundTag();
            for (Map.Entry<UUID, Set<String>> entry : playerInvitations.entrySet()) {
                ListTag teamList = new ListTag();
                for (String teamName : entry.getValue()) {
                    teamList.add(StringTag.valueOf(teamName));
                }
                invitationsTag.put(entry.getKey().toString(), teamList);
            }
            tag.put("invitations", invitationsTag);
            
            return tag;
        }
        
        public boolean createTeam(String teamName, UUID leader) {
            if (teams.containsKey(teamName)) {
                return false; // Team already exists
            }
            
            if (playerTeams.containsKey(leader)) {
                return false; // Player already in a team
            }
            
            Team team = new Team(teamName, leader);
            teams.put(teamName, team);
            teamsById.put(team.getTeamId(), team);
            playerTeams.put(leader, teamName);
            setDirty();
            
            LOGGER.info("Created team '{}' with leader {}", teamName, leader);
            return true;
        }
        
        public boolean deleteTeam(String teamName, UUID player) {
            Team team = teams.get(teamName);
            if (team == null || !team.getLeader().equals(player)) {
                return false; // Team doesn't exist or player is not leader
            }
            
            // Remove all players from the team
            for (UUID memberId : team.getMembers()) {
                playerTeams.remove(memberId);
            }
            
            teams.remove(teamName);
            teamsById.remove(team.getTeamId()); // Rimuovi dalla mappa per ID
            setDirty();
            
            LOGGER.info("Deleted team '{}' by leader {}", teamName, player);
            return true;
        }
        
        public boolean addPlayerToTeam(String teamName, UUID player) {
            Team team = teams.get(teamName);
            if (team == null) {
                return false; // Team doesn't exist
            }
            
            if (playerTeams.containsKey(player)) {
                return false; // Player already in a team
            }
            
            team.addMember(player);
            playerTeams.put(player, teamName);
            setDirty();
            
            LOGGER.info("Added player {} to team '{}'", player, teamName);
            return true;
        }
        
        public boolean removePlayerFromTeam(String teamName, UUID player) {
            Team team = teams.get(teamName);
            if (team == null) {
                return false; // Team doesn't exist
            }
            
            if (team.getLeader().equals(player)) {
                return false; // Can't remove leader
            }
            
            if (!team.getMembers().contains(player)) {
                return false; // Player not in team
            }
            
            team.removeMember(player);
            playerTeams.remove(player);
            setDirty();
            
            LOGGER.info("Removed player {} from team '{}'", player, teamName);
            return true;
        }
        
        public String getPlayerTeam(UUID player) {
            return playerTeams.get(player);
        }
        
        public List<UUID> getTeamMembers(String teamName) {
            Team team = teams.get(teamName);
            return team != null ? new ArrayList<>(team.getMembers()) : new ArrayList<>();
        }
        
        public UUID getTeamLeader(String teamName) {
            Team team = teams.get(teamName);
            return team != null ? team.getLeader() : null;
        }
        
        public boolean addTeamValutes(String teamName, String valuteId, double amount) {
            Team team = teams.get(teamName);
            if (team == null) {
                return false;
            }
            
            team.addValutes(valuteId, amount);
            setDirty();
            
            LOGGER.info("Added {} {} to team '{}'", amount, valuteId, teamName);
            return true;
        }
        
        public boolean removeTeamValutes(String teamName, String valuteId, double amount) {
            Team team = teams.get(teamName);
            if (team == null) {
                return false;
            }
            
            if (team.getValuteBalance(valuteId) < amount) {
                return false; // Insufficient balance
            }
            
            team.removeValutes(valuteId, amount);
            setDirty();
            
            LOGGER.info("Removed {} {} from team '{}'", amount, valuteId, teamName);
            return true;
        }
        
        public double getTeamValuteBalance(String teamName, String valuteId) {
            Team team = teams.get(teamName);
            return team != null ? team.getValuteBalance(valuteId) : 0.0;
        }
        
        public Set<String> getAllTeams() {
            return new HashSet<>(teams.keySet());
        }
        
        public Team getTeamById(UUID teamId) {
            return teamsById.get(teamId);
        }
        
        public UUID getTeamIdByName(String teamName) {
            Team team = teams.get(teamName);
            return team != null ? team.getTeamId() : null;
        }
        
        public boolean invitePlayerToTeam(String teamName, UUID inviter, UUID invitee) {
            Team team = teams.get(teamName);
            if (team == null) {
                return false; // Team doesn't exist
            }
            
            if (!team.getMembers().contains(inviter)) {
                return false; // Inviter not in team
            }
            
            if (playerTeams.containsKey(invitee)) {
                return false; // Invitee already in a team
            }
            
            // Add invitation
            playerInvitations.computeIfAbsent(invitee, k -> new HashSet<>()).add(teamName);
            setDirty();
            
            LOGGER.info("Player {} invited {} to team '{}'", inviter, invitee, teamName);
            return true;
        }
        
        public boolean acceptTeamInvitation(UUID player, String teamName) {
            Set<String> invitations = playerInvitations.get(player);
            if (invitations == null || !invitations.contains(teamName)) {
                return false; // No invitation for this team
            }
            
            if (playerTeams.containsKey(player)) {
                return false; // Player already in a team
            }
            
            Team team = teams.get(teamName);
            if (team == null) {
                return false; // Team doesn't exist
            }
            
            // Add player to team
            team.addMember(player);
            playerTeams.put(player, teamName);
            
            // Remove invitation
            invitations.remove(teamName);
            if (invitations.isEmpty()) {
                playerInvitations.remove(player);
            }
            
            setDirty();
            
            LOGGER.info("Player {} accepted invitation to team '{}'", player, teamName);
            return true;
        }
        
        public List<String> getPlayerInvitations(UUID player) {
            Set<String> invitations = playerInvitations.get(player);
            return invitations != null ? new ArrayList<>(invitations) : new ArrayList<>();
        }
        
        public boolean leaveTeam(UUID player) {
            String teamName = playerTeams.get(player);
            if (teamName == null) {
                return false; // Player not in a team
            }
            
            Team team = teams.get(teamName);
            if (team == null) {
                return false; // Team doesn't exist
            }
            
            if (team.getLeader().equals(player)) {
                return false; // Leader can't leave, must delete team
            }
            
            team.removeMember(player);
            playerTeams.remove(player);
            setDirty();
            
            LOGGER.info("Player {} left team '{}'", player, teamName);
            return true;
        }
        
        public boolean transferLeadership(String teamName, UUID currentLeader, UUID newLeader) {
            Team team = teams.get(teamName);
            if (team == null) {
                return false; // Team doesn't exist
            }
            
            if (!team.getLeader().equals(currentLeader)) {
                return false; // Current player is not the leader
            }
            
            if (!team.getMembers().contains(newLeader)) {
                return false; // New leader is not in the team
            }
            
            if (currentLeader.equals(newLeader)) {
                return false; // Can't transfer to yourself
            }
            
            team.setLeader(newLeader);
            setDirty();
            
            LOGGER.info("Leadership of team '{}' transferred from {} to {}", teamName, currentLeader, newLeader);
            return true;
        }
        
        public boolean addTeamAssistant(String teamName, UUID leader, UUID assistant) {
            Team team = teams.get(teamName);
            if (team == null) {
                return false; // Team doesn't exist
            }
            
            if (!team.getLeader().equals(leader)) {
                return false; // Only leader can add assistants
            }
            
            if (!team.getMembers().contains(assistant)) {
                return false; // Assistant must be a team member
            }
            
            if (team.getLeader().equals(assistant)) {
                return false; // Leader can't be assistant
            }
            
            team.addAssistant(assistant);
            setDirty();
            
            LOGGER.info("Player {} added as assistant to team '{}'", assistant, teamName);
            return true;
        }
        
        public boolean removeTeamAssistant(String teamName, UUID leader, UUID assistant) {
            Team team = teams.get(teamName);
            if (team == null) {
                return false; // Team doesn't exist
            }
            
            if (!team.getLeader().equals(leader)) {
                return false; // Only leader can remove assistants
            }
            
            team.removeAssistant(assistant);
            setDirty();
            
            LOGGER.info("Player {} removed as assistant from team '{}'", assistant, teamName);
            return true;
        }
        
        public List<UUID> getTeamAssistants(String teamName) {
            Team team = teams.get(teamName);
            return team != null ? new ArrayList<>(team.getAssistants()) : new ArrayList<>();
        }
        
        public boolean isPlayerTeamAssistant(UUID player, String teamName) {
            Team team = teams.get(teamName);
            return team != null && team.getAssistants().contains(player);
        }
        
        public boolean renameTeam(String oldTeamName, String newTeamName, UUID player) {
            Team team = teams.get(oldTeamName);
            if (team == null) {
                return false; // Team doesn't exist
            }
            
            // Check if player is leader or assistant
            if (!team.getLeader().equals(player) && !team.getAssistants().contains(player)) {
                return false; // Player is not leader or assistant
            }
            
            if (teams.containsKey(newTeamName)) {
                return false; // New team name already exists
            }
            
            // Remove old team and add new one
            teams.remove(oldTeamName);
            team.setName(newTeamName);
            teams.put(newTeamName, team);
            // teamsById rimane invariata perché l'ID del team non cambia
            
            // Update player-team mapping for all members
            for (UUID memberId : team.getMembers()) {
                playerTeams.put(memberId, newTeamName);
            }
            
            setDirty();
            
            LOGGER.info("Team '{}' renamed to '{}' by {}", oldTeamName, newTeamName, player);
            return true;
        }
    }
    
    /**
     * Represents a team
     */
    public static class Team {
        private UUID teamId; // ID univoco immutabile del team
        private String name;
        private UUID leader;
        private final Set<UUID> members;
        private final Set<UUID> assistants;
        private final Map<String, Double> valuteBalances;
        
        public Team(String name, UUID leader) {
            this.teamId = UUID.randomUUID(); // Genera un ID univoco
            this.name = name;
            this.leader = leader;
            this.members = new HashSet<>();
            this.assistants = new HashSet<>();
            this.valuteBalances = new HashMap<>();
            
            // Add leader as first member
            this.members.add(leader);
        }
        
        // Costruttore per il caricamento da NBT
        private Team(UUID teamId, String name, UUID leader) {
            this.teamId = teamId;
            this.name = name;
            this.leader = leader;
            this.members = new HashSet<>();
            this.assistants = new HashSet<>();
            this.valuteBalances = new HashMap<>();
            
            // Add leader as first member
            this.members.add(leader);
        }
        
        public UUID getTeamId() {
            return teamId;
        }
        
        public String getName() {
            return name;
        }
        
        public UUID getLeader() {
            return leader;
        }
        
        public void setLeader(UUID newLeader) {
            this.leader = newLeader;
        }
        
        public Set<UUID> getMembers() {
            return new HashSet<>(members);
        }
        
        public Set<UUID> getAssistants() {
            return new HashSet<>(assistants);
        }
        
        public void addMember(UUID player) {
            members.add(player);
        }
        
        public void removeMember(UUID player) {
            members.remove(player);
            assistants.remove(player); // Remove from assistants if they leave
        }
        
        public void addAssistant(UUID player) {
            assistants.add(player);
        }
        
        public void removeAssistant(UUID player) {
            assistants.remove(player);
        }
        
        public void addValutes(String valuteId, double amount) {
            valuteBalances.put(valuteId, valuteBalances.getOrDefault(valuteId, 0.0) + amount);
        }
        
        public void removeValutes(String valuteId, double amount) {
            double current = valuteBalances.getOrDefault(valuteId, 0.0);
            valuteBalances.put(valuteId, Math.max(0.0, current - amount));
        }
        
        public double getValuteBalance(String valuteId) {
            return valuteBalances.getOrDefault(valuteId, 0.0);
        }
        
        public Map<String, Double> getAllValuteBalances() {
            return new HashMap<>(valuteBalances);
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("teamId", teamId);
            tag.putString("name", name);
            tag.putUUID("leader", leader);
            
            ListTag membersTag = new ListTag();
            for (UUID member : members) {
                membersTag.add(StringTag.valueOf(member.toString()));
            }
            tag.put("members", membersTag);
            
            ListTag assistantsTag = new ListTag();
            for (UUID assistant : assistants) {
                assistantsTag.add(StringTag.valueOf(assistant.toString()));
            }
            tag.put("assistants", assistantsTag);
            
            CompoundTag valutesTag = new CompoundTag();
            for (Map.Entry<String, Double> entry : valuteBalances.entrySet()) {
                valutesTag.putDouble(entry.getKey(), entry.getValue());
            }
            tag.put("valutes", valutesTag);
            
            return tag;
        }
        
        public static Team load(CompoundTag tag) {
            UUID teamId = tag.getUUID("teamId");
            String name = tag.getString("name");
            UUID leader = tag.getUUID("leader");
            
            Team team = new Team(teamId, name, leader);
            
            if (tag.contains("members")) {
                ListTag membersTag = tag.getList("members", 8); // String tag type
                for (int i = 0; i < membersTag.size(); i++) {
                    try {
                        UUID member = UUID.fromString(membersTag.getString(i));
                        if (!member.equals(leader)) { // Don't add leader twice
                            team.addMember(member);
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid UUID in team members: {}", membersTag.getString(i));
                    }
                }
            }
            
            if (tag.contains("assistants")) {
                ListTag assistantsTag = tag.getList("assistants", 8); // String tag type
                for (int i = 0; i < assistantsTag.size(); i++) {
                    try {
                        UUID assistant = UUID.fromString(assistantsTag.getString(i));
                        team.addAssistant(assistant);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid UUID in team assistants: {}", assistantsTag.getString(i));
                    }
                }
            }
            
            if (tag.contains("valutes")) {
                CompoundTag valutesTag = tag.getCompound("valutes");
                for (String valuteId : valutesTag.getAllKeys()) {
                    team.valuteBalances.put(valuteId, valutesTag.getDouble(valuteId));
                }
            }
            
            return team;
        }
    }
} 