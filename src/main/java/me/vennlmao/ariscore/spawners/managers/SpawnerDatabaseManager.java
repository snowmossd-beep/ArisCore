package me.vennlmao.ariscore.spawners.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.spawners.SpawnersModule;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpawnerDatabaseManager implements AutoCloseable {

    private final SpawnersModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public SpawnerDatabaseManager(SpawnersModule module) {
        this.module = module;
    }

    public void init() {
        boolean mysqlEnabled = module.getConfig().getBoolean("mysql.enabled", false);
        boolean sqliteEnabled = module.getConfig().getBoolean("sqlite.enabled", true);

        if (mysqlEnabled && sqliteEnabled) {
            module.getPlugin().getLogger().warning("[Spawners] Cả mysql.enabled và sqlite.enabled đều đang true trong config.yml. " +
                    "Chỉ nên bật 1 trong 2 — sẽ ưu tiên dùng MySQL.");
        } else if (!mysqlEnabled && !sqliteEnabled) {
            module.getPlugin().getLogger().warning("[Spawners] Cả mysql.enabled và sqlite.enabled đều đang false trong config.yml. " +
                    "Sẽ dùng SQLite làm mặc định để tránh mất dữ liệu.");
        }

        mysql = mysqlEnabled;

        HikariConfig config = new HikariConfig();

        if (mysql) {
            String host = module.getConfig().getString("mysql.host", "localhost");
            int port = module.getConfig().getInt("mysql.port", 3306);
            String database = module.getConfig().getString("mysql.database", "arisspawners");
            String username = module.getConfig().getString("mysql.username", "root");
            String password = module.getConfig().getString("mysql.password", "");
            boolean ssl = module.getConfig().getBoolean("mysql.use-ssl", false);

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(module.getPlugin().getDataFolder(), "spawners/spawners.db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("ArisSpawners-Pool");

        dataSource = new HikariDataSource(config);
        createTable();
    }

    private String prefix() {
        return module.getConfig().getString("mysql.table-prefix", "arisspawners_");
    }

    private String tableName() {
        return module.getConfig().getString("mysql.table-name", "spawners");
    }

    private String table() {
        return prefix() + tableName();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + table() + " (" +
                "world VARCHAR(64) NOT NULL," +
                "x INTEGER NOT NULL," +
                "y INTEGER NOT NULL," +
                "z INTEGER NOT NULL," +
                "entity_type VARCHAR(64) NOT NULL," +
                "amount BIGINT NOT NULL," +
                "owner VARCHAR(36)," +
                "stored_xp BIGINT NOT NULL DEFAULT 0," +
                "storage TEXT," +
                "PRIMARY KEY (world, x, y, z)" +
                ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Spawners] Failed to create table: " + e.getMessage());
        }
    }

    public List<SpawnerData> loadAll() {
        List<SpawnerData> list = new ArrayList<>();
        String sql = "SELECT * FROM " + table();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                EntityType type;
                try {
                    type = EntityType.valueOf(rs.getString("entity_type"));
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                String ownerStr = rs.getString("owner");
                UUID owner = (ownerStr == null || ownerStr.isEmpty()) ? null : UUID.fromString(ownerStr);
                SpawnerData data = new SpawnerData(
                        rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                        type, rs.getLong("amount"), owner);
                data.addXp(rs.getLong("stored_xp"));
                SpawnerData.deserializeStorage(data, rs.getString("storage"));
                data.clearDirty();
                list.add(data);
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Spawners] Failed to load spawners: " + e.getMessage());
        }
        return list;
    }

    public void save(SpawnerData data) {
        String sql = mysql
                ? "INSERT INTO " + table() + " (world,x,y,z,entity_type,amount,owner,stored_xp,storage) VALUES (?,?,?,?,?,?,?,?,?) " +
                  "ON DUPLICATE KEY UPDATE entity_type=?,amount=?,owner=?,stored_xp=?,storage=?"
                : "INSERT OR REPLACE INTO " + table() + " (world,x,y,z,entity_type,amount,owner,stored_xp,storage) VALUES (?,?,?,?,?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, data.getWorld());
            ps.setInt(2, data.getX());
            ps.setInt(3, data.getY());
            ps.setInt(4, data.getZ());
            ps.setString(5, data.getEntityType().name());
            ps.setLong(6, data.getAmount());
            ps.setString(7, data.getOwner() != null ? data.getOwner().toString() : null);
            ps.setLong(8, data.getStoredXp());
            ps.setString(9, data.serializeStorage());
            if (mysql) {
                ps.setString(10, data.getEntityType().name());
                ps.setLong(11, data.getAmount());
                ps.setString(12, data.getOwner() != null ? data.getOwner().toString() : null);
                ps.setLong(13, data.getStoredXp());
                ps.setString(14, data.serializeStorage());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Spawners] Failed to save spawner: " + e.getMessage());
        }
    }

    public void delete(SpawnerData data) {
        String sql = "DELETE FROM " + table() + " WHERE world=? AND x=? AND y=? AND z=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, data.getWorld());
            ps.setInt(2, data.getX());
            ps.setInt(3, data.getY());
            ps.setInt(4, data.getZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Spawners] Failed to delete spawner: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
