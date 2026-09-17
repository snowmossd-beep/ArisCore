package me.vennlmao.ariscore.combat;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.LicenseManager;
import me.vennlmao.ariscore.combat.commands.CombatReloadCommand;
import me.vennlmao.ariscore.combat.commands.CombatTagCommand;
import me.vennlmao.ariscore.combat.listeners.CombatCommandListener;
import me.vennlmao.ariscore.combat.listeners.CombatDamageListener;
import me.vennlmao.ariscore.combat.listeners.CombatDeathListener;
import me.vennlmao.ariscore.combat.listeners.CombatKickListener;
import me.vennlmao.ariscore.combat.listeners.CombatQuitListener;
import me.vennlmao.ariscore.combat.listeners.ItemCooldownListener;
import me.vennlmao.ariscore.combat.managers.CombatManager;
import me.vennlmao.ariscore.combat.managers.ItemCooldownManager;
import me.vennlmao.ariscore.combat.utils.MessageUtil;
import me.vennlmao.ariscore.combat.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class CombatModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private CombatManager combatManager;
    private ItemCooldownManager itemCooldownManager;
    private ScheduledTask tickTask;

    public CombatModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] CombatModule disabled: invalid license.");
            return;
        }
        loadConfig();

        MessageUtil.init(this);
        SoundUtil.init(this);

        combatManager = new CombatManager(this);
        itemCooldownManager = new ItemCooldownManager(this);

        plugin.getCommand("combattag").setExecutor(new CombatTagCommand(this));
        plugin.getCommand("ariscombat").setExecutor(new CombatReloadCommand(this));

        plugin.getServer().getPluginManager().registerEvents(new CombatDamageListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CombatQuitListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CombatKickListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CombatDeathListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CombatCommandListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ItemCooldownListener(this), plugin);

        long interval = Math.max(1L, config.getLong("combat.actionbar-interval-ticks", 20L));
        tickTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate((Plugin) plugin,
                task -> combatManager.tick(), interval, interval);
    }

    public void disable() {
        if (tickTask != null) tickTask.cancel();
        if (combatManager != null) combatManager.clear();
        if (itemCooldownManager != null) itemCooldownManager.clear();
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "combat");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("combat/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public CombatManager getCombatManager() { return combatManager; }
    public ItemCooldownManager getItemCooldownManager() { return itemCooldownManager; }
}
