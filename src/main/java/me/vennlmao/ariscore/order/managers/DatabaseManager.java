package me.vennlmao.ariscore.order.managers;

import me.vennlmao.ariscore.order.utils.OrderEntry;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;

public final class DatabaseManager {
   private final HikariDataSource dataSource;
   private final Plugin plugin;

   public DatabaseManager(Plugin plugin, FileConfiguration config) {
      this.plugin = plugin;
      HikariConfig hikari = new HikariConfig();
      File dbFile = new File(plugin.getDataFolder(), "order/order.db");
      dbFile.getParentFile().mkdirs();
      hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
      hikari.setDriverClassName("org.sqlite.JDBC");
      hikari.setMaximumPoolSize(1);
      hikari.setConnectionTestQuery("SELECT 1");
      hikari.setPoolName("ArisCore-Order-Pool");
      this.dataSource = new HikariDataSource(hikari);
      this.createTable();
   }

   private void createTable() {
      String autoInc = "INTEGER PRIMARY KEY AUTOINCREMENT";
      String sql = "CREATE TABLE IF NOT EXISTS donut_orders (id           "
         + autoInc
         + ",owner        VARCHAR(36)  NOT NULL,owner_name   VARCHAR(64)  NOT NULL,material     VARCHAR(64)  NOT NULL,amount       INT          NOT NULL,price        DOUBLE       NOT NULL,delivered    INT          NOT NULL DEFAULT 0,collected    INT          NOT NULL DEFAULT 0,paid         DOUBLE       NOT NULL DEFAULT 0,paid_creator DOUBLE       NOT NULL,expires_at   BIGINT       NOT NULL,completed    INT          NOT NULL DEFAULT 0,enchants     TEXT         NOT NULL DEFAULT '')";

      try (
         Connection con = this.dataSource.getConnection();
         Statement stmt = con.createStatement();
      ) {
         stmt.executeUpdate(sql);
      } catch (SQLException var16) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to create donut_orders table", (Throwable)var16);
      }

      try (
         Connection con = this.dataSource.getConnection();
         Statement stmt = con.createStatement();
      ) {
         stmt.executeUpdate("ALTER TABLE donut_orders ADD COLUMN collected INT NOT NULL DEFAULT 0");
      } catch (SQLException var13) {
      }
   }

   public List<OrderEntry> loadAllOrders() {
      List<OrderEntry> list = new ArrayList<>();
      String sql = "SELECT * FROM donut_orders WHERE expires_at > ?";

      try (
         Connection con = this.dataSource.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
      ) {
         ps.setLong(1, System.currentTimeMillis());
         ResultSet rs = ps.executeQuery();

         while (rs.next()) {
            Material mat = Material.matchMaterial(rs.getString("material"));
            if (mat != null) {
               OrderEntry e = new OrderEntry(
                  UUID.fromString(rs.getString("owner")),
                  rs.getString("owner_name"),
                  mat,
                  rs.getInt("amount"),
                  rs.getDouble("price"),
                  rs.getInt("delivered"),
                  rs.getInt("collected"),
                  rs.getDouble("paid"),
                  rs.getDouble("paid_creator"),
                  rs.getLong("expires_at"),
                  rs.getLong("id"),
                  this.deserializeEnchants(rs.getString("enchants"))
               );
               e.setCompleted(rs.getInt("completed") == 1);
               list.add(e);
            }
         }
      } catch (SQLException var12) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to load all orders", (Throwable)var12);
      }

      return list;
   }

   private String serializeEnchants(Map<Enchantment, Integer> enchants) {
      if (enchants != null && !enchants.isEmpty()) {
         StringBuilder sb = new StringBuilder();

         for (Entry<Enchantment, Integer> e : enchants.entrySet()) {
            if (sb.length() > 0) {
               sb.append(',');
            }

            sb.append(e.getKey().getKey().toString()).append(':').append(e.getValue());
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   private Map<Enchantment, Integer> deserializeEnchants(String raw) {
      Map<Enchantment, Integer> map = new LinkedHashMap<>();
      if (raw != null && !raw.isBlank()) {
         for (String token : raw.split(",")) {
            int colon = token.lastIndexOf(58);
            if (colon >= 0) {
               String key = token.substring(0, colon);

               int level;
               try {
                  level = Integer.parseInt(token.substring(colon + 1));
               } catch (NumberFormatException var11) {
                  continue;
               }

               Enchantment ench = Enchantment.getByKey(NamespacedKey.fromString(key));
               if (ench != null) {
                  map.put(ench, level);
               }
            }
         }

         return map;
      } else {
         return map;
      }
   }

   public long insertOrder(OrderEntry entry) {
      String sql = "INSERT INTO donut_orders (owner, owner_name, material, amount, price, delivered, collected, paid, paid_creator, expires_at, enchants) VALUES (?,?,?,?,?,?,?,?,?,?,?)";

      try {
         long var6;
         try (
            Connection con = this.dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql, 1);
         ) {
            ps.setString(1, entry.getOwner().toString());
            ps.setString(2, entry.getOwnerName());
            ps.setString(3, entry.getMaterial().name());
            ps.setInt(4, entry.getAmount());
            ps.setDouble(5, entry.getPricePerItem());
            ps.setInt(6, entry.getDelivered());
            ps.setInt(7, entry.getCollected());
            ps.setDouble(8, entry.getPaid());
            ps.setDouble(9, entry.getPaidByCreator());
            ps.setLong(10, entry.getExpiresAtMillis());
            ps.setString(11, this.serializeEnchants(entry.getEnchantments()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) {
               return -1L;
            }

            var6 = keys.getLong(1);
         }

         return var6;
      } catch (SQLException var12) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to insert order", (Throwable)var12);
         return -1L;
      }
   }

   public void updateDelivery(OrderEntry entry) {
      if (entry != null) {
         this.updateDelivery(entry.getDbId(), entry.getDelivered(), entry.getCollected(), entry.getPaid(), entry.isCompleted(), entry.getExpiresAtMillis());
      }
   }

   public void updateDelivery(long dbId, int delivered, int collected, double paid, boolean completed, long expiresAtMillis) {
      if (dbId > 0L) {
         String sql = "UPDATE donut_orders SET delivered = ?, collected = ?, paid = ?, completed = ?, expires_at = ? WHERE id = ?";

         try (
            Connection con = this.dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
         ) {
            ps.setInt(1, delivered);
            ps.setInt(2, collected);
            ps.setDouble(3, paid);
            ps.setInt(4, completed ? 1 : 0);
            ps.setLong(5, expiresAtMillis);
            ps.setLong(6, dbId);
            ps.executeUpdate();
         } catch (SQLException var19) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to update delivery for order " + dbId, (Throwable)var19);
         }
      }
   }

   public void deleteOrder(OrderEntry entry) {
      if (entry != null) {
         this.deleteOrder(entry.getDbId());
      }
   }

   public void deleteOrder(long dbId) {
      if (dbId > 0L) {
         String sql = "DELETE FROM donut_orders WHERE id = ?";

         try (
            Connection con = this.dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
         ) {
            ps.setLong(1, dbId);
            ps.executeUpdate();
         } catch (SQLException var12) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to delete order " + dbId, (Throwable)var12);
         }
      }
   }

   public void close() {
      if (this.dataSource != null && !this.dataSource.isClosed()) {
         this.dataSource.close();
      }
   }
}
