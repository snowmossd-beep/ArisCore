package me.vennlmao.ariscore.tab;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.tab.commands.TabCommand;
import me.vennlmao.ariscore.tab.managers.ConditionEvaluator;
import me.vennlmao.ariscore.tab.managers.PapiManager;
import me.vennlmao.ariscore.tab.managers.ScoreboardManager;
import me.vennlmao.ariscore.tab.managers.TabConfigManager;
import me.vennlmao.ariscore.tab.managers.TabListManager;
import me.vennlmao.ariscore.LicenseManager;

public class TabModule {

    private final ArisCore plugin;
    private TabConfigManager configManager;
    private PapiManager papiManager;
    private ConditionEvaluator conditionEvaluator;
    private TabListManager tabListManager;
    private ScoreboardManager scoreboardManager;

    public TabModule(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] TabModule disabled: invalid license.");
            return;
        }
        configManager      = new TabConfigManager(plugin);
        configManager.load();

        papiManager        = new PapiManager();
        conditionEvaluator = new ConditionEvaluator(papiManager);
        tabListManager     = new TabListManager(plugin, papiManager, conditionEvaluator, configManager);
        scoreboardManager  = new ScoreboardManager(plugin, papiManager, conditionEvaluator, configManager);

        plugin.getServer().getPluginManager().registerEvents(tabListManager,    plugin);
        plugin.getServer().getPluginManager().registerEvents(scoreboardManager, plugin);

        tabListManager.start();
        scoreboardManager.start();

        TabCommand cmd = new TabCommand(this);
        if (plugin.getCommand("tab") != null) {
            plugin.getCommand("tab").setExecutor(cmd);
            plugin.getCommand("tab").setTabCompleter(cmd);
        }

        plugin.getLogger().info("[Tab] Module enabled.");
    }

    public void disable() {
        if (tabListManager    != null) tabListManager.stop();
        if (scoreboardManager != null) scoreboardManager.stop();
        plugin.getLogger().info("[Tab] Module disabled.");
    }

    public void reload() {
        configManager.load();
        tabListManager.reload();
        scoreboardManager.reload();
    }

    public TabConfigManager  getConfigManager()      { return configManager; }
    public PapiManager       getPapiManager()        { return papiManager; }
    public TabListManager    getTabListManager()     { return tabListManager; }
    public ScoreboardManager getScoreboardManager()  { return scoreboardManager; }
    }
