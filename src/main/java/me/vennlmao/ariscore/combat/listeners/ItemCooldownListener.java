package me.vennlmao.ariscore.combat.listeners;

import me.vennlmao.ariscore.combat.CombatModule;
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

        if (!module.getCombatManager().isInCombat(id)) return;

        if (module.getItemCooldownManager().isOnCooldown(id, material)) {
            event.setCancelled(true);
            SoundUtil.play(player, "cooldown");
            return;
        }

        int seconds = module.getItemCooldownManager().getCooldownSeconds(material);
        module.getItemCooldownManager().applyCooldown(id, material);
        player.setCooldown(material, seconds * 20);
    }
            }
