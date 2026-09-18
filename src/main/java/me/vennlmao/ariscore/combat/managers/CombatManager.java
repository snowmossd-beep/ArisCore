package me.vennlmao.ariscore.combat.managers;

import me.vennlmao.ariscore.combat.CombatModule;
import me.vennlmao.ariscore.combat.utils.MessageUtil;
import me.vennlmao.ariscore.combat.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    private final CombatModule module;
    private final Map<UUID, Long> combatEndsAt = new LinkedHashMap<>();
    private final Map<UUID, String> opponentName = new LinkedHashMap<>();

    public CombatManager(CombatModule module) {
        this.module = module;
    }

    public int getDefaultSeconds() {
        return Math.max(1, module.getConfig().getInt("combat.default-seconds", 15));
    }

    public boolean isGlowingEnabled() {
        return module.getConfig().getBoolean("combat.glowing-enabled", true);
    }

    public boolean isInCombat(UUID id) {
        Long endsAt = combatEndsAt.get(id);
        return endsAt != null && endsAt > System.currentTimeMillis();
    }

    public int getRemainingSeconds(UUID id) {
        Long endsAt = combatEndsAt.get(id);
        if (endsAt == null) return 0;
        long remainingMs = endsAt - System.currentTimeMillis();
        return remainingMs <= 0 ? 0 : (int) Math.ceil(remainingMs / 1000.0);
    }

    public void tagPvp(Player attacker, Player victim) {
        int seconds = getDefaultSeconds();
        opponentName.put(victim.getUniqueId(), attacker.getName());
        opponentName.put(attacker.getUniqueId(), victim.getName());
        tag(attacker, seconds, false, null);
        tag(victim, seconds, false, null);
    }

    public void tag(Player target, int seconds, boolean fromCommand, CommandSender source) {
        UUID id = target.getUniqueId();
        boolean alreadyTagged = isInCombat(id);
        combatEndsAt.put(id, System.currentTimeMillis() + (seconds * 1000L));

        if (isGlowingEnabled()) target.setGlowing(true);

        if (fromCommand) {
            MessageUtil.sendChatList(target, "admin_tag_target",
                    s -> s.replace("{seconds}", String.valueOf(seconds))
                            .replace("{sender}", source == null ? "console" : source.getName()));
            SoundUtil.play(target, "admin_tag");
            return;
        }

        if (!alreadyTagged) {
            SoundUtil.play(target, "tag");
        }

        sendCombatActionbar(target, seconds);
    }

    private void sendCombatActionbar(Player target, int seconds) {
        String attacker = opponentName.getOrDefault(target.getUniqueId(), "?");
        MessageUtil.sendActionbar(target, "combat_actionbar",
                s -> s.replace("{seconds}", String.valueOf(seconds)).replace("{attacker}", attacker));
    }

    public void untag(UUID id) {
        combatEndsAt.remove(id);
        opponentName.remove(id);
        module.getItemCooldownManager().clearPlayer(id);
        Player player = Bukkit.getPlayer(id);
        if (player != null && player.isOnline()) {
            if (isGlowingEnabled()) player.setGlowing(false);
            for (Material material : module.getItemCooldownManager().getTrackedItems().keySet()) {
                player.setCooldown(material, 0);
            }
        }
    }

    public void punish(Player player, String reason) {
        if (!isInCombat(player.getUniqueId())) return;

        boolean shouldPunish = module.getConfig().getBoolean("combat.punish-on-" + reason, true);
        if (!shouldPunish) {
            untag(player.getUniqueId());
            return;
        }

        if (module.getConfig().getBoolean("combat.drop-items-on-punish", true)) {
            Location loc = player.getLocation();
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType().isAir()) continue;
                loc.getWorld().dropItemNaturally(loc, item);
            }
            player.getInventory().clear();
        }

        if (player.getHealth() > 0) {
            player.setHealth(0.0);
        }

        MessageUtil.sendChatList(Bukkit.getConsoleSender(), "killed_on_" + reason,
                s -> s.replace("{player}", player.getName()));

        untag(player.getUniqueId());
    }

    public void tick() {
        for (Map.Entry<UUID, Long> entry : Map.copyOf(combatEndsAt).entrySet()) {
            UUID id = entry.getKey();
            Player player = Bukkit.getPlayer(id);

            if (player == null || !player.isOnline()) {
                combatEndsAt.remove(id);
                continue;
            }

            int remaining = getRemainingSeconds(id);
            if (remaining <= 0) {
                untag(id);
                player.getScheduler().run(module.getPlugin(), t -> {
                    MessageUtil.sendChatList(player, "combat_end");
                    MessageUtil.sendActionbar(player, "combat_end_ab");
                    SoundUtil.play(player, "combat_end");
                }, null);
                continue;
            }

            player.getScheduler().run(module.getPlugin(), t ->
                    sendCombatActionbar(player, remaining), null);
        }
    }

    public void clear() {
        combatEndsAt.clear();
        opponentName.clear();
    }
    }
