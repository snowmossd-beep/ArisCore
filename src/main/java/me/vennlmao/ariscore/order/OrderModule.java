package me.vennlmao.ariscore.order;

import me.vennlmao.ariscore.LicenseManager;
import me.vennlmao.ariscore.order.commands.OrdersCommand;
import me.vennlmao.ariscore.order.listeners.OrdersListener;
import me.vennlmao.ariscore.order.listeners.SignInputListener;
import me.vennlmao.ariscore.order.managers.DatabaseManager;
import me.vennlmao.ariscore.order.managers.EconomyBridge;
import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.managers.OrdersService;
import me.vennlmao.ariscore.order.utils.ServerScheduler;
import me.vennlmao.ariscore.order.utils.SoundUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class OrderModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private DatabaseManager databaseManager;
    private GuiManager guiManager;
    private OrdersService ordersService;
    private SignInputListener signInputListener;

    public OrderModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] OrderModule disabled: invalid license.");
            return;
        }

        loadConfig();
        SoundUtil.init(this);
        loadMessages();

        guiManager = new GuiManager(plugin);
        ordersService = new OrdersService(guiManager);
        ordersService.setPlugin(plugin);
        ordersService.setMessages(messages);

        EconomyBridge economyBridge = EconomyBridge.setup(plugin);
        if (economyBridge == null) {
            plugin.getLogger().severe("[ArisCore] OrderModule: no economy provider found (Vault). Module disabled.");
            return;
        }

        plugin.getLogger().info("[ArisCore] Order economy ready via " + economyBridge.providerName() + ".");
        ordersService.setEconomy(economyBridge);
        signInputListener = new SignInputListener(plugin, config.getString("sign-input.marker", ""));

        try {
            databaseManager = new DatabaseManager(plugin, messages);
            ordersService.setDatabase(databaseManager);
            plugin.getLogger().info("[ArisCore] Order storage ready, orders loaded.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[ArisCore] Failed to initialize order storage", e);
        }

        if (plugin.getCommand("order") != null) {
            plugin.getCommand("order").setExecutor(new OrdersCommand(ordersService, this));
        }

        plugin.getServer().getPluginManager().registerEvents(signInputListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new OrdersListener(ordersService, signInputListener, plugin), plugin);
    }

    public void disable() {
        ServerScheduler.cancelAll(plugin);
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public void reload() {
        loadConfig();
        loadMessages();
        if (guiManager != null) guiManager.reload();
        if (ordersService != null) ordersService.setMessages(messages);
        if (signInputListener != null) signInputListener.setMarker(config.getString("sign-input.marker", ""));
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "order");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("order/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void loadMessages() {
        File folder = new File(plugin.getDataFolder(), "order");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, "messages.yml");
        if (!file.exists()) plugin.saveResource("order/messages.yml", false);

        messages = YamlConfiguration.loadConfiguration(file);
        InputStream defStream = plugin.getResource("order/messages.yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
            messages.options().copyDefaults(true);

            try {
                ((YamlConfiguration) messages).save(file);
            } catch (IOException ignored) {
            }
        }
    }

    public JavaPlugin getPlugin() { return plugin; }
    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getMessages() { return messages; }
    public GuiManager getGuiManager() { return guiManager; }
    public OrdersService getOrdersService() { return ordersService; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
            }
            
