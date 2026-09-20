package me.vennlmao.ariscore.team.managers;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.gui.TeamEnderChestHolder;
import me.vennlmao.ariscore.team.utils.ColorUtil;
import me.vennlmao.ariscore.team.utils.InventorySerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class TeamEnderChestManager {

    private final TeamModule module;
    private final TeamDatabaseManager db;
    private final Map<String, Inventory> cache = new HashMap<>();

    public TeamEnderChestManager(TeamModule module, TeamDatabaseManager db) {
        this.module = module;
        this.db = db;
    }

    public Inventory getInventory(String teamName) {
        String key = teamName.toLowerCase();
        Inventory cached = cache.get(key);
        if (cached != null) return cached;

        int size = module.getConfig().getInt("endchest.size", 27);
        String title = module.getConfig().getString("endchest.title", "&5Team Ender Chest")
                .replace("{team-name}", teamName);

        TeamEnderChestHolder holder = new TeamEnderChestHolder(teamName);
        Inventory inv = Bukkit.createInventory(holder, size, ColorUtil.parse(title));
        holder.setInventory(inv);

        String base64 = db.loadEndchest(key);
        ItemStack[] contents = InventorySerializer.fromBase64(base64, size);
        inv.setContents(contents);

        cache.put(key, inv);
        return inv;
    }

    public void save(String teamName, Inventory inventory) {
        String base64 = InventorySerializer.toBase64(inventory.getContents());
        db.saveEndchest(teamName.toLowerCase(), base64);
    }

    public void unload(String teamName) {
        cache.remove(teamName.toLowerCase());
    }

    public void delete(String teamName) {
        cache.remove(teamName.toLowerCase());
        db.deleteEndchest(teamName.toLowerCase());
    }

    public void saveAll() {
        for (Map.Entry<String, Inventory> entry : cache.entrySet()) {
            db.saveEndchest(entry.getKey(), InventorySerializer.toBase64(entry.getValue().getContents()));
        }
    }
}
