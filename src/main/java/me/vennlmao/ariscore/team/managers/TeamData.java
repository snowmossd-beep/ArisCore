package me.vennlmao.ariscore.team.managers;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TeamData {

    private final String name;
    private String tag;
    private UUID leader;
    private final Map<UUID, TeamRole> members = new LinkedHashMap<>();
    private final Map<UUID, Long> joinDates = new LinkedHashMap<>();
    private Location home;
    private boolean pvpEnabled;
    private long createdAt;

    public TeamData(String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }

    public Map<UUID, TeamRole> getMembers() { return members; }

    public void addMember(UUID uuid, TeamRole role, long joinDate) {
        members.put(uuid, role);
        joinDates.put(uuid, joinDate);
        if (role == TeamRole.LEADER) leader = uuid;
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        joinDates.remove(uuid);
    }

    public void setRole(UUID uuid, TeamRole role) {
        if (!members.containsKey(uuid)) return;
        members.put(uuid, role);
        if (role == TeamRole.LEADER) leader = uuid;
    }

    public TeamRole getRole(UUID uuid) { return members.get(uuid); }
    public long getJoinDate(UUID uuid) { return joinDates.getOrDefault(uuid, createdAt); }
    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }
    public boolean isLeader(UUID uuid) { return uuid != null && uuid.equals(leader); }
    public int getMemberCount() { return members.size(); }

    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }
    public void removeHome() { this.home = null; }

    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
        }
