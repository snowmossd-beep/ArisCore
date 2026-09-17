package me.vennlmao.ariscore.combat.listeners;

import me.vennlmao.ariscore.combat.CombatModule;
import me.vennlmao.ariscore.combat.utils.MessageUtil;
import me.vennlmao.ariscore.combat.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ItemCooldownListener implements Listener {

    private final CombatModule module;

    public ItemCooldownListener(CombatModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!module.getItemCooldownManager().isEnabled()) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        Material material = item.getType();
        if (!module.getItemCooldownManager().isTracked(material)) return;

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (module.getItemCooldownManager().isOnCooldown(id, material)) {
            event.setCancelled(true);
            int remaining = module.getItemCooldownManager().getRemainingSeconds(id, material);
            String itemName = formatItemName(material);

            MessageUtil.sendChatList(player, "item_cooldown",
                    s -> s.replace("{seconds}", String.valueOf(remaining)).replace("{item}", itemName));
            MessageUtil.sendActionbar(player, "item_cooldown_ab",
                    s -> s.replace("{seconds}", String.valueOf(remaining)).replace("{item}", itemName));
            SoundUtil.play(player, "cooldown");
            return;
        }

        module.getItemCooldownManager().applyCooldown(id, material);
    }

    private String formatItemName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
