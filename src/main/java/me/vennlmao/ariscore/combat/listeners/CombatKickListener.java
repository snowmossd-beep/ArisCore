package me.vennlmao.ariscore.combat.listeners;

import me.vennlmao.ariscore.combat.CombatModule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class CombatKickListener implements Listener {

    private final CombatModule module;

    public CombatKickListener(CombatModule module) {
        this.module = module;
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        module.getCombatManager().punish(player, "kick");
    }
}
