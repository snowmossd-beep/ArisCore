package me.vennlmao.ariscore.rtpqueue.gui;

import me.vennlmao.ariscore.rtpqueue.RtpQueueModule;
import me.vennlmao.ariscore.rtpqueue.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiUtil {

    public static Inventory buildMainGui(RtpQueueModule module, Player player) {
        String title = module.getConfig().getString("gui.main.title", "");
        int size = module.getConfig().getInt("gui.main.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, MessageUtil.parse(title));

        applyStaticItem(module, inv, "gui.main.slots.confirm", player, size);
        updateDynamicSlots(module, inv, player);

        return inv;
    }

    public static void updateDynamicSlots(RtpQueueModule module, Inventory inv, Player player) {
        boolean queued = module.getQueueManager().isQueued(player.getUniqueId());
        applyCancelItem(module, inv, queued, player);
        applyInfoItem(module, inv, player);
        applyPingItem(module, inv, player);
        applyWorldItem(module, inv, player);
    }

    private static void applyWorldItem(RtpQueueModule module, Inventory inv, Player player) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection("gui.main.slots.world");
        if (sec == null) return;
        int slot = sec.getInt("slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;

        String worldKey = module.getQueueManager().getSelectedWorld(player.getUniqueId());
        String worldDisplay;
        if (worldKey != null) {
            worldDisplay = GuiUtil.stripColor(module.getConfig().getString("worlds." + worldKey + ".display-name", worldKey));
        } else {
            worldDisplay = GuiUtil.stripColor(sec.getString("no-selection", "Ngẫu nhiên"));
        }

        List<String> lore = sec.getStringList("lore");
        List<String> replaced = new ArrayList<>();
        for (String line : lore) {
            replaced.add(line.replace("{selected_world}", worldDisplay));
        }

        inv.setItem(slot, buildItem(sec, replaced, player));
    }

    private static void applyCancelItem(RtpQueueModule module, Inventory inv, boolean queued, Player player) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection("gui.main.slots.cancel");
        if (sec == null) return;
        int slot = sec.getInt("slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;

        String path = queued ? "lore-queued" : "lore-idle";
        List<String> lore = sec.getStringList(path);
        if (lore.isEmpty()) lore = sec.getStringList("lore");

        inv.setItem(slot, buildItem(sec, lore, player));
    }

    private static void applyInfoItem(RtpQueueModule module, Inventory inv, Player player) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection("gui.main.slots.info");
        if (sec == null) return;
        int slot = sec.getInt("slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;

        int current = module.getQueueManager().size();
        int max = module.getQueueManager().getMax();

        List<String> lore = sec.getStringList("lore");
        List<String> replaced = new ArrayList<>();
        for (String line : lore) {
            replaced.add(line.replace("{current}", String.valueOf(current)).replace("{max}", String.valueOf(max)));
        }

        inv.setItem(slot, buildItem(sec, replaced, player));
    }

    private static void applyPingItem(RtpQueueModule module, Inventory inv, Player player) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection("gui.main.slots.ping");
        if (sec == null) return;
        int slot = sec.getInt("slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;

        int ping = player.getPing();

        List<String> lore = sec.getStringList("lore");
        List<String> replaced = new ArrayList<>();
        for (String line : lore) {
            replaced.add(line.replace("{ping}", String.valueOf(ping)));
        }

        inv.setItem(slot, buildItem(sec, replaced, player));
    }

    private static void applyStaticItem(RtpQueueModule module, Inventory inv, String path, Player player, int size) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection(path);
        if (sec == null) return;
        int slot = sec.getInt("slot", -1);
        if (slot < 0 || slot >= size) return;

        inv.setItem(slot, buildItem(sec, sec.getStringList("lore"), player));
    }

    public static Inventory buildWorldSelectGui(RtpQueueModule module, Player player) {
        String title = module.getConfig().getString("gui.world_select.title", "");
        int size = module.getConfig().getInt("gui.world_select.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, MessageUtil.parse(title));

        ConfigurationSection worlds = module.getConfig().getConfigurationSection("worlds");
        if (worlds == null) return inv;

        String selected = module.getQueueManager().getSelectedWorld(player.getUniqueId());

        for (String key : worlds.getKeys(false)) {
            ConfigurationSection sec = worlds.getConfigurationSection(key);
            if (sec == null) continue;
            if (!sec.getBoolean("enabled", true)) continue;
            int slot = sec.getInt("slot", -1);
            if (slot < 0 || slot >= size) continue;

            boolean isSelected = key.equals(selected);
            List<String> lore = sec.getStringList(isSelected ? "lore-selected" : "lore");
            if (lore.isEmpty()) lore = sec.getStringList("lore");

            inv.setItem(slot, buildItem(sec, lore, player));
        }

        return inv;
    }

    private static ItemStack buildItem(ConfigurationSection sec, List<String> lore, Player player) {
        String matName = sec.getString("material", "STONE");
        ItemStack item;

        if (matName.equalsIgnoreCase("PLAYER_HEAD_SELF") && player != null) {
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(player);
                item.setItemMeta(skullMeta);
            }
        } else {
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.STONE;
            item = new ItemStack(mat);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = sec.getString("display-name", "");
        meta.displayName(MessageUtil.parse(displayName));

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(MessageUtil.parse(line));
        }
        meta.lore(loreComponents);

        item.setItemMeta(meta);
        return item;
    }

    public static String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("&[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("&#[0-9A-Fa-f]{6}", "")
                .trim();
    }
    }
