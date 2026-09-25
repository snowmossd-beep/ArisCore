package me.vennlmao.ariscore.order.managers;

import java.lang.reflect.Method;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyBridge {
   private final EconomyBridge.Backend backend;
   private final String providerName;

   private EconomyBridge(EconomyBridge.Backend backend, String providerName) {
      this.backend = backend;
      this.providerName = providerName;
   }

   public String providerName() {
      return this.providerName;
   }

   public boolean has(OfflinePlayer player, double amount) {
      return player != null && !(amount <= 0.0) ? this.backend.has(player, amount) : true;
   }

   public boolean withdraw(OfflinePlayer player, double amount) {
      return player != null && !(amount <= 0.0) ? this.backend.withdraw(player, amount) : true;
   }

   public boolean deposit(OfflinePlayer player, double amount) {
      return player != null && !(amount <= 0.0) ? this.backend.deposit(player, amount) : true;
   }

   public double getBalance(OfflinePlayer player) {
      return player == null ? 0.0 : this.backend.getBalance(player);
   }

   public static EconomyBridge setup(JavaPlugin plugin) {
      EconomyBridge donut = tryDonutEconomy(plugin);
      return donut != null ? donut : tryVault(plugin);
   }

   private static EconomyBridge tryDonutEconomy(final JavaPlugin plugin) {
      Plugin ecoPlugin = Bukkit.getPluginManager().getPlugin("DonutEconomy");
      if (ecoPlugin != null && ecoPlugin.isEnabled()) {
         try {
            Method getEconomy = ecoPlugin.getClass().getMethod("getEconomy");
            final Object economyService = getEconomy.invoke(ecoPlugin);
            if (economyService == null) {
               return null;
            } else {
               final Method has = economyService.getClass().getMethod("has", OfflinePlayer.class, double.class);
               final Method withdraw = economyService.getClass().getMethod("withdraw", OfflinePlayer.class, double.class);
               final Method deposit = economyService.getClass().getMethod("deposit", OfflinePlayer.class, double.class);
               final Method getBalance = economyService.getClass().getMethod("getBalance", OfflinePlayer.class);
               EconomyBridge.Backend backend = new EconomyBridge.Backend() {
                  @Override
                  public boolean has(OfflinePlayer player, double amount) {
                     try {
                        return Boolean.TRUE.equals(has.invoke(economyService, player, amount));
                     } catch (ReflectiveOperationException var5) {
                        plugin.getLogger().log(Level.WARNING, "DonutEconomy has() failed", (Throwable)var5);
                        return false;
                     }
                  }

                  @Override
                  public boolean withdraw(OfflinePlayer player, double amount) {
                     try {
                        return Boolean.TRUE.equals(withdraw.invoke(economyService, player, amount));
                     } catch (ReflectiveOperationException var5) {
                        plugin.getLogger().log(Level.WARNING, "DonutEconomy withdraw() failed", (Throwable)var5);
                        return false;
                     }
                  }

                  @Override
                  public boolean deposit(OfflinePlayer player, double amount) {
                     try {
                        return Boolean.TRUE.equals(deposit.invoke(economyService, player, amount));
                     } catch (ReflectiveOperationException var5) {
                        plugin.getLogger().log(Level.WARNING, "DonutEconomy deposit() failed", (Throwable)var5);
                        return false;
                     }
                  }

                  @Override
                  public double getBalance(OfflinePlayer player) {
                     try {
                        return getBalance.invoke(economyService, player) instanceof Number n ? n.doubleValue() : 0.0;
                     } catch (ReflectiveOperationException var4) {
                        plugin.getLogger().log(Level.WARNING, "DonutEconomy getBalance() failed", (Throwable)var4);
                        return 0.0;
                     }
                  }
               };
               plugin.getLogger().info("Using economy provider: DonutEconomy");
               return new EconomyBridge(backend, "DonutEconomy");
            }
         } catch (Throwable var10) {
            plugin.getLogger().log(Level.WARNING, "Failed to hook DonutEconomy", var10);
            return null;
         }
      } else {
         return null;
      }
   }

   private static EconomyBridge tryVault(JavaPlugin plugin) {
      if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
         return null;
      } else {
         try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp != null && rsp.getProvider() != null) {
               final Object economy = rsp.getProvider();
               final Method has = economyClass.getMethod("has", OfflinePlayer.class, double.class);
               final Method withdrawPlayer = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
               final Method depositPlayer = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
               final Method getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
               final Method transactionSuccess = Class.forName("net.milkbowl.vault.economy.EconomyResponse").getMethod("transactionSuccess");
               EconomyBridge.Backend backend = new EconomyBridge.Backend() {
                  @Override
                  public boolean has(OfflinePlayer player, double amount) {
                     try {
                        return Boolean.TRUE.equals(has.invoke(economy, player, amount));
                     } catch (ReflectiveOperationException var5) {
                        return false;
                     }
                  }

                  @Override
                  public boolean withdraw(OfflinePlayer player, double amount) {
                     try {
                        Object response = withdrawPlayer.invoke(economy, player, amount);
                        return response != null && Boolean.TRUE.equals(transactionSuccess.invoke(response));
                     } catch (ReflectiveOperationException var5) {
                        return false;
                     }
                  }

                  @Override
                  public boolean deposit(OfflinePlayer player, double amount) {
                     try {
                        Object response = depositPlayer.invoke(economy, player, amount);
                        return response != null && Boolean.TRUE.equals(transactionSuccess.invoke(response));
                     } catch (ReflectiveOperationException var5) {
                        return false;
                     }
                  }

                  @Override
                  public double getBalance(OfflinePlayer player) {
                     try {
                        return getBalance.invoke(economy, player) instanceof Number n ? n.doubleValue() : 0.0;
                     } catch (ReflectiveOperationException var4) {
                        return 0.0;
                     }
                  }
               };
               plugin.getLogger().info("Using economy provider: Vault");
               return new EconomyBridge(backend, "Vault");
            } else {
               return null;
            }
         } catch (Throwable var10) {
            plugin.getLogger().log(Level.WARNING, "Failed to hook Vault", var10);
            return null;
         }
      }
   }

   private interface Backend {
      boolean has(OfflinePlayer var1, double var2);

      boolean withdraw(OfflinePlayer var1, double var2);

      boolean deposit(OfflinePlayer var1, double var2);

      double getBalance(OfflinePlayer var1);
   }
}
