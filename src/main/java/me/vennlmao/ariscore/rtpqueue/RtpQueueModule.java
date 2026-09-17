package me.vennlmao.ariscore.rtpqueue;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.LicenseManager;
import me.vennlmao.ariscore.rtpqueue.commands.RtpQueueCommand;
import me.vennlmao.ariscore.rtpqueue.listeners.GuiListener;
import me.vennlmao.ariscore.rtpqueue.listeners.QuitListener;
import me.vennlmao.ariscore.rtpqueue.managers.QueueManager;
import me.vennlmao.ariscore.rtpqueue.utils.MessageUtil;
import me.vennlmao.ariscore.rtpqueue.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class RtpQueueModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private QueueManager queueManager;
    private ScheduledTask refreshTask;

    public RtpQueueModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] RtpQueueModule disabled: invalid license.");
            return;
        }
        loadConfig();

        MessageUtil.init(this);
        SoundUtil.init(this);
        queueManager = new QueueManager(this);

        RtpQueueCommand cmd = new RtpQueueCommand(this);
        plugin.getCommand("rtpqueue").setExecutor(cmd);
        plugin.getCommand("rtpq").setExecutor(cmd);

        plugin.getServer().getPluginManager().registerEvents(new GuiListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new QuitListener(this), plugin);

        long interval = Math.max(1L, config.getLong("refresh-interval-ticks", 20L));
        refreshTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate((Plugin) plugin,
                task -> queueManager.refreshViewers(), interval, interval);
    }

    public void disable() {
        if (refreshTask != null) refreshTask.cancel();
        if (queueManager != null) queueManager.clear();
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "rtpqueue");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("rtpqueue/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public QueueManager getQueueManager() { return queueManager; }
}
