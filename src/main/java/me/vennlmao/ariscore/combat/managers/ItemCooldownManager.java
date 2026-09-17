package me.vennlmao.ariscore.combat.managers;

import me.vennlmao.ariscore.combat.CombatModule;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemCooldownManager {

    private final CombatModule module;
    private final Map<UUID, Map<Material, Long>> cooldownEndsAt = new LinkedHashMap<>();

    public ItemCooldownManager(CombatModule module) {
        this.module = module;
    }

    public boolean isEnabled() {
        return module.getConfig().getBoolean("item-cooldown.enabled", true);
    }

    public Map<Material, Integer> getTrackedItems() {
        List<String> entries = module.getConfig().getStringList("item-cooldown.items");
        Map<Material, Integer> items = new LinkedHashMap<>();

        for (String entry : entries) {
            String[] parts = entry.trim().split(":");
            if (parts.length < 2) continue;

            Material material = Material.matchMaterial(parts[0].trim().replace("-", "_"));
            if (material == null) continue;

            int seconds;
            try {
                seconds = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }

            if (seconds > 0) items.put(material, seconds);
        }

        return items;
    }

    public boolean isTracked(Material material) {
        return getTrackedItems().containsKey(material);
    }

    public int getCooldownSeconds(Material material) {
        return getTrackedItems().getOrDefault(material, 0);
    }

    public boolean isOnCooldown(UUID id, Material material) {
        Map<Material, Long> perItem = cooldownEndsAt.get(id);
        if (perItem == null) return false;
        Long endsAt = perItem.get(material);
        return endsAt != null && endsAt > System.currentTimeMillis();
    }

    public int getRemainingSeconds(UUID id, Material material) {
        Map<Material, Long> perItem = cooldownEndsAt.get(id);
        if (perItem == null) return 0;
        Long endsAt = perItem.get(material);
        if (endsAt == null) return 0;
        long remainingMs = endsAt - System.currentTimeMillis();
        return remainingMs <= 0 ? 0 : (int) Math.ceil(remainingMs / 1000.0);
    }

    public void applyCooldown(UUID id, Material material) {
        int seconds = getCooldownSeconds(material);
        if (seconds <= 0) return;
        cooldownEndsAt.computeIfAbsent(id, k -> new LinkedHashMap<>())
                .put(material, System.currentTimeMillis() + (seconds * 1000L));
    }

    public void clear() {
        cooldownEndsAt.clear();
    }
}
