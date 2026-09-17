package me.vennlmao.ariscore.combat.listeners;

import me.vennlmao.ariscore.combat.CombatModule;
import me.vennlmao.ariscore.combat.utils.MessageUtil;
import me.vennlmao.ariscore.combat.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CombatCommandListener implements Listener {

    private final CombatModule module;

    public CombatCommandListener(CombatModule module) {
        this.module = module;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!module.getCombatManager().isInCombat(player.getUniqueId())) return;

        String label = extractLabel(event.getMessage());

        List<String> allowed = module.getConfig().getStringList("commands.allowed-in-combat");
        for (String entry : allowed) {
            if (entry.equalsIgnoreCase(label)) return;
        }

        event.setCancelled(true);
        MessageUtil.sendChatList(player, "command_blocked_in_combat",
                s -> s.replace("{command}", label));
        SoundUtil.play(player, "error");
    }

    private String extractLabel(String message) {
        String raw = message.startsWith("/") ? message.substring(1) : message;
        int spaceIndex = raw.indexOf(' ');
        String label = spaceIndex >= 0 ? raw.substring(0, spaceIndex) : raw;
        int colonIndex = label.indexOf(':');
        return colonIndex >= 0 ? label.substring(colonIndex + 1) : label;
    }
}
