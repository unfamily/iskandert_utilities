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
     * Gets the team data (for internal use)
     */
    public TeamData getTeamDataInstance() {
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
     * Adds currency to a team's balance
     */
    public boolean addTeamCurrency(String teamName, String currencyId, double amount) {
        TeamData data = getTeamData();
        return data.addTeamValutes(teamName, currencyId, amount);
    }
    
    /**
     * Adds valutes to a team's balance (legacy method)
     */
    public boolean addTeamValutes(String teamName, String valuteId, double amount) {
        return addTeamCurrency(teamName, valuteId, amount);
    }

    /**
     * Removes currency from a team's balance
     */
    public boolean removeTeamCurrency(String teamName, String currencyId, double amount) {
        TeamData data = getTeamData();
        return data.removeTeamValutes(teamName, currencyId, amount);
    }
    
    /**
     * Removes valutes from a team's balance (legacy method)
     */
    public boolean removeTeamValutes(String teamName, String valuteId, double amount) {
        return removeTeamCurrency(teamName, valuteId, amount);
    }
    
    /**
     * Gets a team's currency balance
     */
    public double getTeamCurrencyBalance(String teamName, String currencyId) {
        TeamData data = getTeamData();
        return data.getTeamValuteBalance(teamName, currencyId);
    }
    
    /**
     * Gets a team's valute balance (legacy method)
     */
    public double getTeamValuteBalance(String teamName, String valuteId) {
        return getTeamCurrencyBalance(teamName, valuteId);
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
     * Cancels a team invitation (only leader or assistants can do this)
     */
    public boolean cancelTeamInvitation(String teamName, ServerPlayer canceller, ServerPlayer invitee) {
        TeamData data = getTeamData();
        return data.cancelTeamInvitation(teamName, canceller.getUUID(), invitee.getUUID());
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
        private final Map<UUID, Team> teamsById = new HashMap<>(); // Map for access by ID
        private final Map<UUID, String> playerTeams = new HashMap<>();
        private final Map<UUID, Map<String, Long>> playerInvitations = new HashMap<>(); // teamName -> timestamp
        
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
                    data.teamsById.put(team.getTeamId(), team); // Populate the ID map
                    
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
                        CompoundTag playerInvitationsTag = invitationsTag.getCompound(playerIdStr);
                        Map<String, Long> invitations = new HashMap<>();
                        for (String teamName : playerInvitationsTag.getAllKeys()) {
                            invitations.put(teamName, playerInvitationsTag.getLong(teamName));
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
            for (Map.Entry<UUID, Map<String, Long>> entry : playerInvitations.entrySet()) {
                CompoundTag playerInvitationsTag = new CompoundTag();
                for (Map.Entry<String, Long> invitationEntry : entry.getValue().entrySet()) {
                    playerInvitationsTag.putLong(invitationEntry.getKey(), invitationEntry.getValue());
                }
                invitationsTag.put(entry.getKey().toString(), playerInvitationsTag);
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
            teamsById.remove(team.getTeamId()); // Remove from ID map
            setDirty();
            

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
            
            // Add invitation with timestamp
            playerInvitations.computeIfAbsent(invitee, k -> new HashMap<>()).put(teamName, System.currentTimeMillis());
            setDirty();
            
            return true;
        }
        
        public boolean acceptTeamInvitation(UUID player, String teamName) {
            Map<String, Long> invitations = playerInvitations.get(player);
            if (invitations == null || !invitations.containsKey(teamName)) {
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
            
            return true;
        }
        
        public List<String> getPlayerInvitations(UUID player) {
            Map<String, Long> invitations = playerInvitations.get(player);
            if (invitations == null) {
                return new ArrayList<>();
            }

            // Filter out expired invitations (older than 24 hours = 86400000 milliseconds)
            long currentTime = System.currentTimeMillis();
            long expiryTime = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

            List<String> validInvitations = new ArrayList<>();
            for (Map.Entry<String, Long> entry : invitations.entrySet()) {
                if (currentTime - entry.getValue() < expiryTime) {
                    validInvitations.add(entry.getKey());
                }
            }

            return validInvitations;
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
        
        public boolean cancelTeamInvitation(String teamName, UUID canceller, UUID invitee) {
            Team team = teams.get(teamName);
            if (team == null) {
                return false; // Team doesn't exist
            }

            // Only leader or assistants can cancel invitations
            if (!team.getLeader().equals(canceller) && !team.getAssistants().contains(canceller)) {
                return false; // Canceller is not authorized
            }

            Map<String, Long> invitations = playerInvitations.get(invitee);
            if (invitations == null || !invitations.containsKey(teamName)) {
                return false; // No invitation to cancel
            }

            // Remove the invitation
            invitations.remove(teamName);
            if (invitations.isEmpty()) {
                playerInvitations.remove(invitee);
            }
            setDirty();

            return true;
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
            // teamsById remains unchanged because the team ID doesn't change
            
            // Update player-team mapping for all members
            for (UUID memberId : team.getMembers()) {
                playerTeams.put(memberId, newTeamName);
            }
            
            setDirty();
            

            return true;
        }

        public void cleanupExpiredInvitations() {
            long currentTime = System.currentTimeMillis();
            long expiryTime = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

            boolean changed = false;
            Iterator<Map.Entry<UUID, Map<String, Long>>> playerIterator = playerInvitations.entrySet().iterator();

            while (playerIterator.hasNext()) {
                Map.Entry<UUID, Map<String, Long>> playerEntry = playerIterator.next();
                Map<String, Long> invitations = playerEntry.getValue();

                Iterator<Map.Entry<String, Long>> invitationIterator = invitations.entrySet().iterator();
                while (invitationIterator.hasNext()) {
                    Map.Entry<String, Long> invitationEntry = invitationIterator.next();
                    if (currentTime - invitationEntry.getValue() >= expiryTime) {
                        invitationIterator.remove();
                        changed = true;
                    }
                }

                if (invitations.isEmpty()) {
                    playerIterator.remove();
                    changed = true;
                }
            }

            if (changed) {
                setDirty();
            }
        }
    }
    
    /**
     * Represents a team
     */
    public static class Team {
        private UUID teamId; // Unique immutable team ID
        private String name;
        private UUID leader;
        private final Set<UUID> members;
        private final Set<UUID> assistants;
        private final Map<String, Double> valuteBalances;
        
        public Team(String name, UUID leader) {
            this.teamId = UUID.randomUUID(); // Generate a unique ID
            this.name = name;
            this.leader = leader;
            this.members = new HashSet<>();
            this.assistants = new HashSet<>();
            this.valuteBalances = new HashMap<>();
            
            // Add leader as first member
            this.members.add(leader);
        }
        
        // Constructor for loading from NBT
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