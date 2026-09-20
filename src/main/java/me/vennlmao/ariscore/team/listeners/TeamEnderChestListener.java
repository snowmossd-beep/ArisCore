package me.vennlmao.ariscore.team.listeners;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.gui.TeamEnderChestHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class TeamEnderChestListener implements Listener {

    private final TeamModule module;

    public TeamEnderChestListener(TeamModule module) { this.module = module; }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TeamEnderChestHolder holder)) return;
        module.getTeamEnderChestManager().save(holder.getTeamName(), event.getInventory());
    }
          }
