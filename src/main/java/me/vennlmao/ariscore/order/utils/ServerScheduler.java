package me.vennlmao.ariscore.order.utils;

import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class ServerScheduler {
   private static final boolean FOLIA;

   private ServerScheduler() {
   }

   public static boolean isFolia() {
      return FOLIA;
   }

   public static void runGlobalTask(Plugin plugin, Runnable task) {
      if (plugin != null && task != null) {
         plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
      }
   }

   public static void runGlobalTaskLater(Plugin plugin, Runnable task, long delayTicks) {
      if (plugin != null && task != null) {
         if (delayTicks <= 0L) {
            runGlobalTask(plugin, task);
         } else {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduled -> task.run(), delayTicks);
         }
      }
   }

   public static void runPlayerTask(Plugin plugin, Player player, Runnable task) {
      runEntityTask(plugin, player, task);
   }

   public static void runPlayerTaskLater(Plugin plugin, Player player, Runnable task, long delayTicks) {
      runEntityTaskLater(plugin, player, task, delayTicks);
   }

   public static void runEntityTask(Plugin plugin, Entity entity, Runnable task) {
      if (plugin != null && entity != null && task != null) {
         entity.getScheduler().run(plugin, scheduled -> task.run(), null);
      }
   }

   public static void runEntityTaskLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
      if (plugin != null && entity != null && task != null) {
         if (delayTicks <= 0L) {
            runEntityTask(plugin, entity, task);
         } else {
            entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, delayTicks);
         }
      }
   }

   public static void runRegionTask(Plugin plugin, Location location, Runnable task) {
      if (plugin != null && task != null) {
         if (location != null && location.getWorld() != null) {
            plugin.getServer().getRegionScheduler().execute(plugin, location, task);
         } else {
            runGlobalTask(plugin, task);
         }
      }
   }

   public static void runRegionTask(Plugin plugin, World world, int chunkX, int chunkZ, Runnable task) {
      if (plugin != null && world != null && task != null) {
         plugin.getServer().getRegionScheduler().execute(plugin, world, chunkX, chunkZ, task);
      }
   }

   public static void runRegionTaskLater(Plugin plugin, Location location, Runnable task, long delayTicks) {
      if (plugin != null && task != null) {
         if (location == null || location.getWorld() == null) {
            runGlobalTaskLater(plugin, task, delayTicks);
         } else if (delayTicks <= 0L) {
            runRegionTask(plugin, location, task);
         } else {
            plugin.getServer().getRegionScheduler().runDelayed(plugin, location, scheduled -> task.run(), delayTicks);
         }
      }
   }

   public static void runAsync(Plugin plugin, Runnable task) {
      if (plugin != null && task != null) {
         plugin.getServer().getAsyncScheduler().runNow(plugin, scheduled -> task.run());
      }
   }

   public static void runAsyncLater(Plugin plugin, Runnable task, long delay, TimeUnit unit) {
      if (plugin != null && task != null && unit != null) {
         if (delay <= 0L) {
            runAsync(plugin, task);
         } else {
            plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduled -> task.run(), delay, unit);
         }
      }
   }

   public static void cancelAll(Plugin plugin) {
      if (plugin != null) {
         try {
            plugin.getServer().getGlobalRegionScheduler().cancelTasks(plugin);
         } catch (Throwable var3) {
         }

         try {
            plugin.getServer().getAsyncScheduler().cancelTasks(plugin);
         } catch (Throwable var2) {
         }
      }
   }

   static {
      boolean folia = false;

      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         folia = true;
      } catch (ClassNotFoundException var2) {
      }

      FOLIA = folia;
   }
}
