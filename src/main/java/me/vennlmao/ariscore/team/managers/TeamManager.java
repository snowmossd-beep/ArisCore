package me.vennlmao.ariscore.team.managers;

import me.vennlmao.ariscore.team.TeamModule;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeamManager {

    private final TeamModule module;
    private final TeamDatabaseManager db;
    private final Map<String, TeamData> teams = new HashMap<>();
    private final Map<UUID, String> playerTeam = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> pendingInvites = new HashMap<>();

    public TeamManager(TeamModule module, TeamDatabaseManager db) {
        this.module = module;
        this.db = db;
        load();
    }

    private void load() {
        Map<String, TeamData> loaded = db.loadAllTeams();
        teams.putAll(loaded);
        for (TeamData team : loaded.values())
            for (UUID uuid : team.getMembers().keySet())
                playerTeam.put(uuid, team.getName());
    }

    public boolean createTeam(Player leader, String name) {
        if (teams.containsKey(name.toLowerCase())) return false;
        if (playerTeam.containsKey(leader.getUniqueId())) return false;

        TeamData team = new TeamData(name);
        long now = System.currentTimeMillis();
        team.addMember(leader.getUniqueId(), TeamRole.LEADER, now);

        teams.put(name.toLowerCase(), team);
        playerTeam.put(leader.getUniqueId(), name);
        db.saveTeam(team);
        db.saveMember(leader.getUniqueId(), name, TeamRole.LEADER, now);
        return true;
    }

    public void disbandTeam(String name) {
        TeamData team = getTeam(name);
        if (team == null) return;
        Set<UUID> members = new HashSet<>(team.getMembers().keySet());
        for (UUID uuid : members) {
            playerTeam.remove(uuid);
            db.removeMember(uuid);
        }
        teams.remove(name.toLowerCase());
        db.deleteTeam(name);
        module.getTeamEnderChestManager().delete(name);
    }

    public boolean addMember(String teamName, Player player) {
        TeamData team = getTeam(teamName);
        if (team == null) return false;
        if (team.getMemberCount() >= module.getConfig().getInt("team.max-members", 45)) return false;

        long now = System.currentTimeMillis();
        team.addMember(player.getUniqueId(), TeamRole.MEMBER, now);
        playerTeam.put(player.getUniqueId(), teamName);
        db.saveMember(player.getUniqueId(), teamName, TeamRole.MEMBER, now);
        return true;
    }

    public void removeMember(UUID uuid) {
        String teamName = playerTeam.remove(uuid);
        if (teamName == null) return;
        TeamData team = getTeam(teamName);
        if (team == null) return;
        team.removeMember(uuid);
        db.removeMember(uuid);
    }

    public boolean setRole(TeamData team, UUID uuid, TeamRole role) {
        if (!team.isMember(uuid)) return false;
        team.setRole(uuid, role);
        db.saveMember(uuid, team.getName(), role, team.getJoinDate(uuid));
        return true;
    }

    public void transferLeadership(TeamData team, UUID newLeader) {
        UUID oldLeader = team.getLeader();
        if (oldLeader != null) setRole(team, oldLeader, TeamRole.ADMIN);
        setRole(team, newLeader, TeamRole.LEADER);
        team.setLeader(newLeader);
        db.saveTeam(team);
    }

    public void setHome(String teamName, Location loc) {
        TeamData team = getTeam(teamName);
        if (team == null) return;
        team.setHome(loc);
        db.saveTeam(team);
    }

    public void deleteHome(String teamName) {
        TeamData team = getTeam(teamName);
        if (team == null) return;
        team.removeHome();
        db.saveTeam(team);
    }

    public void setPvp(String teamName, boolean enabled) {
        TeamData team = getTeam(teamName);
        if (team == null) return;
        team.setPvpEnabled(enabled);
        db.saveTeam(team);
    }

    public void setTag(String teamName, String tag) {
        TeamData team = getTeam(teamName);
        if (team == null) return;
        team.setTag(tag);
        db.saveTeam(team);
    }

    public TeamData getTeam(String name) { return teams.get(name.toLowerCase()); }
    public TeamData getPlayerTeam(UUID uuid) { String n = playerTeam.get(uuid); return n != null ? getTeam(n) : null; }
    public String getPlayerTeamName(UUID uuid) { return playerTeam.get(uuid); }
    public boolean hasTeam(UUID uuid) { return playerTeam.containsKey(uuid); }
    public boolean teamExists(String name) { return teams.containsKey(name.toLowerCase()); }
    public Collection<TeamData> getAllTeams() { return teams.values(); }

    public void addInvite(UUID invited, UUID inviter) {
        long expiryMs = module.getConfig().getInt("team.invite-expiry-seconds", 60) * 1000L;
        pendingInvites.computeIfAbsent(invited, k -> new HashMap<>())
                .put(inviter, System.currentTimeMillis() + expiryMs);
    }

    private void purgeExpired(UUID uuid) {
        Map<UUID, Long> invites = pendingInvites.get(uuid);
        if (invites == null) return;
        long now = System.currentTimeMillis();
        invites.values().removeIf(expiry -> expiry < now);
        if (invites.isEmpty()) pendingInvites.remove(uuid);
    }

    public boolean hasInvite(UUID uuid, String teamName) {
        purgeExpired(uuid);
        Map<UUID, Long> invites = pendingInvites.get(uuid);
        if (invites == null) return false;
        return invites.keySet().stream().anyMatch(o -> {
            TeamData t = getPlayerTeam(o);
            return t != null && t.getName().equalsIgnoreCase(teamName);
        });
    }

    public boolean hasAnyInvite(UUID uuid) {
        purgeExpired(uuid);
        Map<UUID, Long> invites = pendingInvites.get(uuid);
        return invites != null && !invites.isEmpty();
    }

    public String getInviteTeam(UUID uuid) {
        purgeExpired(uuid);
        Map<UUID, Long> invites = pendingInvites.get(uuid);
        if (invites == null || invites.isEmpty()) return null;
        UUID o = invites.keySet().iterator().next();
        TeamData t = getPlayerTeam(o);
        return t != null ? t.getName() : null;
    }

    public void removeInvite(UUID uuid) { pendingInvites.remove(uuid); }
    }
            
