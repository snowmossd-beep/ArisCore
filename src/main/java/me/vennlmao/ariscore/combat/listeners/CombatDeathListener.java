package me.vennlmao.ariscore.combat.listeners;

import me.vennlmao.ariscore.combat.CombatModule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class CombatDeathListener implements Listener {

    private final CombatModule module;

    public CombatDeathListener(CombatModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!module.getConfig().getBoolean("combat.lightning-on-death", true)) return;

        Location location = event.getEntity().getLocation();
        World world = location.getWorld();
        if (world == null) return;

        world.strikeLightningEffect(location);
    }
}
