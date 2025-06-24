package net.unfamily.iskautils.shop;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.*;

/**
 * Manages shop teams for sharing valutes between team members
 */
public class ShopTeamManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static ShopTeamManager INSTANCE;
    private final ServerLevel level;
    
    // In-memory storage
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<UUID, Set<String>> playerInvitations = new HashMap<>();
    
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
     * Creates a new team
     */
    public boolean createTeam(String teamName, ServerPlayer leader) {
        if (teams.containsKey(teamName)) {
            return false; // Team already exists
        }
        
        if (playerTeams.containsKey(leader.getUUID())) {
            return false; // Player already in a team
        }
        
        Team team = new Team(teamName, leader.getUUID());
        teams.put(teamName, team);
        playerTeams.put(leader.getUUID(), teamName);
        
        LOGGER.info("Created team '{}' with leader {}", teamName, leader.getUUID());
        return true;
    }
    
    /**
     * Deletes a team (only leader can do this)
     */
    public boolean deleteTeam(String teamName, ServerPlayer player) {
        Team team = teams.get(teamName);
        if (team == null || !team.getLeader().equals(player.getUUID())) {
            return false; // Team doesn't exist or player is not leader
        }
        
        // Remove all players from the team
        for (UUID memberId : team.getMembers()) {
            playerTeams.remove(memberId);
        }
        
        teams.remove(teamName);
        
        LOGGER.info("Deleted team '{}' by leader {}", teamName, player.getUUID());
        return true;
    }
    
    /**
     * Adds a player to a team
     */
    public boolean addPlayerToTeam(String teamName, ServerPlayer player) {
        Team team = teams.get(teamName);
        if (team == null) {
            return false; // Team doesn't exist
        }
        
        if (playerTeams.containsKey(player.getUUID())) {
            return false; // Player already in a team
        }
        
        team.addMember(player.getUUID());
        playerTeams.put(player.getUUID(), teamName);
        
        LOGGER.info("Added player {} to team '{}'", player.getUUID(), teamName);
        return true;
    }
    
    /**
     * Removes a player from a team
     */
    public boolean removePlayerFromTeam(String teamName, ServerPlayer player) {
        Team team = teams.get(teamName);
        if (team == null) {
            return false; // Team doesn't exist
        }
        
        if (team.getLeader().equals(player.getUUID())) {
            return false; // Can't remove leader
        }
        
        if (!team.getMembers().contains(player.getUUID())) {
            return false; // Player not in team
        }
        
        team.removeMember(player.getUUID());
        playerTeams.remove(player.getUUID());
        
        LOGGER.info("Removed player {} from team '{}'", player.getUUID(), teamName);
        return true;
    }
    
    /**
     * Gets the team a player belongs to
     */
    public String getPlayerTeam(ServerPlayer player) {
        return playerTeams.get(player.getUUID());
    }
    
    /**
     * Gets all members of a team
     */
    public List<UUID> getTeamMembers(String teamName) {
        Team team = teams.get(teamName);
        return team != null ? new ArrayList<>(team.getMembers()) : new ArrayList<>();
    }
    
    /**
     * Gets the leader of a team
     */
    public UUID getTeamLeader(String teamName) {
        Team team = teams.get(teamName);
        return team != null ? team.getLeader() : null;
    }
    
    /**
     * Adds valutes to a team's balance
     */
    public boolean addTeamValutes(String teamName, String valuteId, double amount) {
        Team team = teams.get(teamName);
        if (team == null) {
            return false;
        }
        
        team.addValutes(valuteId, amount);
        
        LOGGER.info("Added {} {} to team '{}'", amount, valuteId, teamName);
        return true;
    }
    
    /**
     * Removes valutes from a team's balance
     */
    public boolean removeTeamValutes(String teamName, String valuteId, double amount) {
        Team team = teams.get(teamName);
        if (team == null) {
            return false;
        }
        
        if (team.getValuteBalance(valuteId) < amount) {
            return false; // Insufficient balance
        }
        
        team.removeValutes(valuteId, amount);
        
        LOGGER.info("Removed {} {} from team '{}'", amount, valuteId, teamName);
        return true;
    }
    
    /**
     * Gets a team's valute balance
     */
    public double getTeamValuteBalance(String teamName, String valuteId) {
        Team team = teams.get(teamName);
        return team != null ? team.getValuteBalance(valuteId) : 0.0;
    }
    
    /**
     * Gets all teams
     */
    public Set<String> getAllTeams() {
        return new HashSet<>(teams.keySet());
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
        Team team = teams.get(teamName);
        return team != null && team.getAssistants().contains(player.getUUID());
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
        Team team = teams.get(teamName);
        if (team == null) {
            return false; // Team doesn't exist
        }
        
        if (!team.getMembers().contains(inviter.getUUID())) {
            return false; // Inviter not in team
        }
        
        if (playerTeams.containsKey(invitee.getUUID())) {
            return false; // Invitee already in a team
        }
        
        // Add invitation
        playerInvitations.computeIfAbsent(invitee.getUUID(), k -> new HashSet<>()).add(teamName);
        
        LOGGER.info("Player {} invited {} to team '{}'", inviter.getUUID(), invitee.getUUID(), teamName);
        return true;
    }
    
    /**
     * Accepts a team invitation
     */
    public boolean acceptTeamInvitation(ServerPlayer player, String teamName) {
        Set<String> invitations = playerInvitations.get(player.getUUID());
        if (invitations == null || !invitations.contains(teamName)) {
            return false; // No invitation for this team
        }
        
        Team team = teams.get(teamName);
        if (team == null) {
            return false; // Team doesn't exist anymore
        }
        
        if (playerTeams.containsKey(player.getUUID())) {
            return false; // Player already in a team
        }
        
        // Add player to team
        team.addMember(player.getUUID());
        playerTeams.put(player.getUUID(), teamName);
        
        // Remove invitation
        invitations.remove(teamName);
        if (invitations.isEmpty()) {
            playerInvitations.remove(player.getUUID());
        }
        
        LOGGER.info("Player {} accepted invitation to team '{}'", player.getUUID(), teamName);
        return true;
    }
    
    /**
     * Gets pending invitations for a player
     */
    public List<String> getPlayerInvitations(ServerPlayer player) {
        Set<String> invitations = playerInvitations.get(player.getUUID());
        return invitations != null ? new ArrayList<>(invitations) : new ArrayList<>();
    }
    
    /**
     * Leaves the current team
     */
    public boolean leaveTeam(ServerPlayer player) {
        String teamName = playerTeams.get(player.getUUID());
        if (teamName == null) {
            return false; // Player not in a team
        }
        
        Team team = teams.get(teamName);
        if (team == null) {
            return false; // Team doesn't exist
        }
        
        if (team.getLeader().equals(player.getUUID())) {
            return false; // Leader can't leave, must transfer leadership or delete team
        }
        
        team.removeMember(player.getUUID());
        playerTeams.remove(player.getUUID());
        
        LOGGER.info("Player {} left team '{}'", player.getUUID(), teamName);
        return true;
    }
    
    /**
     * Transfers team leadership
     */
    public boolean transferLeadership(String teamName, ServerPlayer currentLeader, ServerPlayer newLeader) {
        Team team = teams.get(teamName);
        if (team == null) {
            return false; // Team doesn't exist
        }
        
        if (!team.getLeader().equals(currentLeader.getUUID())) {
            return false; // Current player is not leader
        }
        
        if (!team.getMembers().contains(newLeader.getUUID())) {
            return false; // New leader not in team
        }
        
        team.setLeader(newLeader.getUUID());
        
        LOGGER.info("Team '{}' leadership transferred from {} to {}", teamName, currentLeader.getUUID(), newLeader.getUUID());
        return true;
    }
    
    /**
     * Adds a team assistant
     */
    public boolean addTeamAssistant(String teamName, ServerPlayer leader, ServerPlayer assistant) {
        Team team = teams.get(teamName);
        if (team == null) {
            return false; // Team doesn't exist
        }
        
        if (!team.getLeader().equals(leader.getUUID())) {
            return false; // Only leader can add assistants
        }
        
        if (!team.getMembers().contains(assistant.getUUID())) {
            return false; // Assistant must be a team member
        }
        
        team.addAssistant(assistant.getUUID());
        
        LOGGER.info("Player {} added as assistant to team '{}'", assistant.getUUID(), teamName);
        return true;
    }
    
    /**
     * Removes a team assistant
     */
    public boolean removeTeamAssistant(String teamName, ServerPlayer leader, ServerPlayer assistant) {
        Team team = teams.get(teamName);
        if (team == null) {
            return false; // Team doesn't exist
        }
        
        if (!team.getLeader().equals(leader.getUUID())) {
            return false; // Only leader can remove assistants
        }
        
        team.removeAssistant(assistant.getUUID());
        
        LOGGER.info("Player {} removed as assistant from team '{}'", assistant.getUUID(), teamName);
        return true;
    }
    
    /**
     * Gets team assistants
     */
    public List<UUID> getTeamAssistants(String teamName) {
        Team team = teams.get(teamName);
        return team != null ? new ArrayList<>(team.getAssistants()) : new ArrayList<>();
    }
    
    /**
     * Renames a team
     */
    public boolean renameTeam(String oldTeamName, String newTeamName, ServerPlayer player) {
        Team team = teams.get(oldTeamName);
        if (team == null) {
            return false; // Team doesn't exist
        }
        
        // Check if player is leader or assistant
        if (!team.getLeader().equals(player.getUUID()) && !team.getAssistants().contains(player.getUUID())) {
            return false; // Player is not leader or assistant
        }
        
        if (teams.containsKey(newTeamName)) {
            return false; // New team name already exists
        }
        
        // Remove old team and add new one
        teams.remove(oldTeamName);
        team.setName(newTeamName);
        teams.put(newTeamName, team);
        
        // Update player-team mapping for all members
        for (UUID memberId : team.getMembers()) {
            playerTeams.put(memberId, newTeamName);
        }
        
        LOGGER.info("Team '{}' renamed to '{}' by {}", oldTeamName, newTeamName, player.getUUID());
        return true;
    }
    
    /**
     * Represents a team
     */
    public static class Team {
        private String name;
        private UUID leader;
        private final Set<UUID> members;
        private final Set<UUID> assistants;
        private final Map<String, Double> valuteBalances;
        
        public Team(String name, UUID leader) {
            this.name = name;
            this.leader = leader;
            this.members = new HashSet<>();
            this.assistants = new HashSet<>();
            this.valuteBalances = new HashMap<>();
            
            // Add leader as first member
            this.members.add(leader);
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
    }
} 