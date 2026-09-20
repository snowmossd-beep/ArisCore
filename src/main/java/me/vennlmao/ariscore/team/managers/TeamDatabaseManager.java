package me.vennlmao.ariscore.team.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.team.TeamModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamDatabaseManager implements AutoCloseable {

    private final TeamModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public TeamDatabaseManager(TeamModule module) { this.module = module; }

    public void init() {
        mysql = module.getConfig().getBoolean("mysql.enabled", false);
        HikariConfig config = new HikariConfig();
        if (mysql) {
            config.setJdbcUrl("jdbc:mysql://" + module.getConfig().getString("mysql.host") + ":" +
                    module.getConfig().getInt("mysql.port") + "/" +
                    module.getConfig().getString("mysql.database") + "?useSSL=" +
                    module.getConfig().getBoolean("mysql.use-ssl") + "&autoReconnect=true");
            config.setUsername(module.getConfig().getString("mysql.username"));
            config.setPassword(module.getConfig().getString("mysql.password"));
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(module.getPlugin().getDataFolder(), "team/team.db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("ArisTeam-Pool");
        dataSource = new HikariDataSource(config);
        createTables();
    }

    private String p() { return module.getConfig().getString("mysql.table-prefix", "ariscore_"); }

    private void createTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + p() + "teams (" +
                    "name VARCHAR(64) PRIMARY KEY," +
                    "tag VARCHAR(16)," +
                    "leader VARCHAR(36)," +
                    "created_at BIGINT DEFAULT 0," +
                    "home_world VARCHAR(64)," +
                    "home_x DOUBLE DEFAULT 0," +
                    "home_y DOUBLE DEFAULT 0," +
                    "home_z DOUBLE DEFAULT 0," +
                    "home_yaw FLOAT DEFAULT 0," +
                    "home_pitch FLOAT DEFAULT 0," +
                    "pvp_enabled TINYINT DEFAULT 0" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS " + p() + "team_members (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "team_name VARCHAR(64) NOT NULL," +
                    "role VARCHAR(16) NOT NULL," +
                    "join_date BIGINT DEFAULT 0" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS " + p() + "team_endchest (" +
                    "team_name VARCHAR(64) PRIMARY KEY," +
                    "contents TEXT" +
                    ")");
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Tables error: " + e.getMessage());
        }
    }

    public Map<String, TeamData> loadAllTeams() {
        Map<String, TeamData> teams = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM " + p() + "teams");
            while (rs.next()) {
                String name = rs.getString("name");
                TeamData team = new TeamData(name);
                team.setTag(rs.getString("tag"));
                team.setCreatedAt(rs.getLong("created_at"));
                team.setPvpEnabled(rs.getInt("pvp_enabled") == 1);
                String leaderStr = rs.getString("leader");
                if (leaderStr != null) team.setLeader(UUID.fromString(leaderStr));
                String world = rs.getString("home_world");
                if (world != null) {
                    World w = Bukkit.getWorld(world);
                    if (w != null) team.setHome(new Location(w, rs.getDouble("home_x"), rs.getDouble("home_y"),
                            rs.getDouble("home_z"), rs.getFloat("home_yaw"), rs.getFloat("home_pitch")));
                }
                teams.put(name.toLowerCase(), team);
            }
            ResultSet mrs = conn.createStatement().executeQuery("SELECT * FROM " + p() + "team_members");
            while (mrs.next()) {
                TeamData team = teams.get(mrs.getString("team_name").toLowerCase());
                if (team == null) continue;
                team.addMember(UUID.fromString(mrs.getString("uuid")),
                        TeamRole.valueOf(mrs.getString("role")), mrs.getLong("join_date"));
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Load error: " + e.getMessage());
        }
        return teams;
    }

    public void saveTeam(TeamData team) {
        String sql = mysql
                ? "INSERT INTO " + p() + "teams (name,tag,leader,created_at,home_world,home_x,home_y,home_z,home_yaw,home_pitch,pvp_enabled) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE tag=?,leader=?,created_at=?,home_world=?,home_x=?,home_y=?,home_z=?,home_yaw=?,home_pitch=?,pvp_enabled=?"
                : "INSERT OR REPLACE INTO " + p() + "teams (name,tag,leader,created_at,home_world,home_x,home_y,home_z,home_yaw,home_pitch,pvp_enabled) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            Location h = team.getHome();
            String w = h != null && h.getWorld() != null ? h.getWorld().getName() : null;
            double x = h != null ? h.getX() : 0, y = h != null ? h.getY() : 0, z = h != null ? h.getZ() : 0;
            float yaw = h != null ? h.getYaw() : 0, pitch = h != null ? h.getPitch() : 0;
            int pvp = team.isPvpEnabled() ? 1 : 0;
            String leader = team.getLeader() != null ? team.getLeader().toString() : null;

            ps.setString(1, team.getName()); ps.setString(2, team.getTag()); ps.setString(3, leader);
            ps.setLong(4, team.getCreatedAt()); ps.setString(5, w);
            ps.setDouble(6, x); ps.setDouble(7, y); ps.setDouble(8, z);
            ps.setFloat(9, yaw); ps.setFloat(10, pitch); ps.setInt(11, pvp);
            if (mysql) {
                ps.setString(12, team.getTag()); ps.setString(13, leader); ps.setLong(14, team.getCreatedAt());
                ps.setString(15, w); ps.setDouble(16, x); ps.setDouble(17, y); ps.setDouble(18, z);
                ps.setFloat(19, yaw); ps.setFloat(20, pitch); ps.setInt(21, pvp);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Save team error: " + e.getMessage());
        }
    }

    public void deleteTeam(String name) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement p1 = conn.prepareStatement("DELETE FROM " + p() + "teams WHERE name=?");
            p1.setString(1, name); p1.executeUpdate();
            PreparedStatement p2 = conn.prepareStatement("DELETE FROM " + p() + "team_members WHERE team_name=?");
            p2.setString(1, name); p2.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Delete error: " + e.getMessage());
        }
    }

    public void saveMember(UUID uuid, String teamName, TeamRole role, long joinDate) {
        String sql = mysql
                ? "INSERT INTO " + p() + "team_members (uuid,team_name,role,join_date) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE team_name=?,role=?,join_date=?"
                : "INSERT OR REPLACE INTO " + p() + "team_members (uuid,team_name,role,join_date) VALUES (?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, teamName); ps.setString(3, role.name()); ps.setLong(4, joinDate);
            if (mysql) {
                ps.setString(5, teamName); ps.setString(6, role.name()); ps.setLong(7, joinDate);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Save member error: " + e.getMessage());
        }
    }

    public void removeMember(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + p() + "team_members WHERE uuid=?")) {
            ps.setString(1, uuid.toString()); ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Remove member error: " + e.getMessage());
        }
    }

    public String loadEndchest(String teamName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT contents FROM " + p() + "team_endchest WHERE team_name=?")) {
            ps.setString(1, teamName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("contents");
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Load endchest error: " + e.getMessage());
        }
        return null;
    }

    public void saveEndchest(String teamName, String base64) {
        String sql = mysql
                ? "INSERT INTO " + p() + "team_endchest (team_name,contents) VALUES (?,?) ON DUPLICATE KEY UPDATE contents=?"
                : "INSERT OR REPLACE INTO " + p() + "team_endchest (team_name,contents) VALUES (?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamName); ps.setString(2, base64);
            if (mysql) ps.setString(3, base64);
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Save endchest error: " + e.getMessage());
        }
    }

    public void deleteEndchest(String teamName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + p() + "team_endchest WHERE team_name=?")) {
            ps.setString(1, teamName); ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Delete endchest error: " + e.getMessage());
        }
    }

    public void close() { if (dataSource != null && !dataSource.isClosed()) dataSource.close(); }
                                                }
                    
