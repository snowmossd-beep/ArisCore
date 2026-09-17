package me.vennlmao.ariscore.rtpqueue.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.rtpqueue.RtpQueueModule;
import me.vennlmao.ariscore.rtpqueue.gui.GuiUtil;
import me.vennlmao.ariscore.rtpqueue.utils.MessageUtil;
import me.vennlmao.ariscore.rtpqueue.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class QueueManager {

    private static final Random RANDOM = new Random();

    private final RtpQueueModule module;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final Map<UUID, String> selectedWorld = new LinkedHashMap<>();
    private final Map<UUID, Inventory> mainViewers = new LinkedHashMap<>();
    private final Set<UUID> worldViewers = new LinkedHashSet<>();

    public QueueManager(RtpQueueModule module) {
        this.module = module;
    }

    public int getMax() {
        return Math.max(1, module.getConfig().getInt("queue.max_players", 2));
    }

    public int size() {
        return queue.size();
    }

    public boolean isQueued(UUID id) {
        return queue.contains(id);
    }

    public List<String> getEnabledWorldKeys() {
        ConfigurationSection worlds = module.getConfig().getConfigurationSection("worlds");
        if (worlds == null) return List.of();

        List<String> keys = new ArrayList<>();
        for (String key : worlds.getKeys(false)) {
            ConfigurationSection sec = worlds.getConfigurationSection(key);
            if (sec == null) continue;
            if (sec.getBoolean("enabled", true)) keys.add(key);
        }
        return keys;
    }

    public String getSelectedWorld(UUID id) {
        return selectedWorld.get(id);
    }

    public void setSelectedWorld(UUID id, String worldKey) {
        selectedWorld.put(id, worldKey);
    }

    public String resolveTeleportWorld(UUID id) {
        List<String> enabled = getEnabledWorldKeys();
        if (enabled.isEmpty()) return null;

        String chosen = selectedWorld.get(id);
        if (chosen != null && enabled.contains(chosen)) return chosen;

        return enabled.get(RANDOM.nextInt(enabled.size()));
    }

    public void addMainViewer(UUID id, Inventory inventory) {
        mainViewers.put(id, inventory);
    }

    public void removeMainViewer(UUID id) {
        mainViewers.remove(id);
    }

    public void addWorldViewer(UUID id) {
        worldViewers.add(id);
    }

    public void removeWorldViewer(UUID id) {
        worldViewers.remove(id);
    }

    public boolean joinQueue(Player player) {
        UUID id = player.getUniqueId();
        if (queue.contains(id)) return false;
        queue.add(id);

        MessageUtil.sendChatList(player, "joined_queue",
                s -> s.replace("{current}", String.valueOf(size())).replace("{max}", String.valueOf(getMax())));
        MessageUtil.sendActionbar(player, "joined_queue_ab",
                s -> s.replace("{current}", String.valueOf(size())).replace("{max}", String.valueOf(getMax())));
        SoundUtil.play(player, "queue_join");

        if (queue.size() >= getMax()) {
            teleportQueue();
        }
        return true;
    }

    public boolean leaveQueue(Player player) {
        UUID id = player.getUniqueId();
        if (!queue.remove(id)) return false;

        MessageUtil.sendChatList(player, "left_queue");
        MessageUtil.sendActionbar(player, "left_queue_ab");
        SoundUtil.play(player, "cancel");
        return true;
    }

    public void removeSilently(UUID id) {
        queue.remove(id);
        selectedWorld.remove(id);
        mainViewers.remove(id);
        worldViewers.remove(id);
    }

    private void teleportQueue() {
        List<UUID> batch = List.copyOf(queue);
        queue.clear();

        int attempts = Math.max(1, module.getConfig().getInt("queue.max_attempts", 20));

        for (UUID id : batch) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                selectedWorld.remove(id);
                continue;
            }

            String worldKey = resolveTeleportWorld(id);
            selectedWorld.remove(id);

            if (worldKey == null) {
                MessageUtil.sendChatList(player, "no_world_configured");
                SoundUtil.play(player, "error");
                continue;
            }

            ConfigurationSection sec = module.getConfig().getConfigurationSection("worlds." + worldKey);
            if (sec == null) {
                MessageUtil.sendChatList(player, "no_world_configured");
                SoundUtil.play(player, "error");
                continue;
            }

            String worldName = sec.getString("world", "");
            World world = worldName.isEmpty() ? null : Bukkit.getWorld(worldName);
            if (world == null) {
                MessageUtil.sendChatList(player, "world_disabled");
                SoundUtil.play(player, "error");
                continue;
            }

            player.closeInventory();
            int countdownSeconds = Math.max(0, module.getConfig().getInt("queue.countdown", 5));
            MessageUtil.sendChatList(player, "queue_full_notice",
                    s -> s.replace("{max}", String.valueOf(getMax())).replace("{seconds}", String.valueOf(countdownSeconds)));
            MessageUtil.sendActionbar(player, "queue_full_notice_ab",
                    s -> s.replace("{max}", String.valueOf(getMax())).replace("{seconds}", String.valueOf(countdownSeconds)));
            SoundUtil.play(player, "queue_full");

            LocationFinder.findSafe(world, sec, attempts).thenAccept(location ->
                    player.getScheduler().run(module.getPlugin(), t -> startCountdown(player, location, worldName), null));
        }
    }

    private void startCountdown(Player player, Location location, String worldName) {
        if (location == null) {
            MessageUtil.sendChatList(player, "no_safe_location");
            MessageUtil.sendActionbar(player, "no_safe_location_ab");
            SoundUtil.play(player, "error");
            return;
        }

        int seconds = Math.max(0, module.getConfig().getInt("queue.countdown", 5));
        int[] remaining = {seconds};

        if (remaining[0] <= 0) {
            finishTeleport(player, location, worldName);
            return;
        }

        MessageUtil.sendTitle(player, "queue_countdown_title", "queue_countdown_subtitle",
                s -> s.replace("{seconds}", String.valueOf(remaining[0])));
        SoundUtil.play(player, "countdown");

        ScheduledTask task = player.getScheduler().runAtFixedRate(module.getPlugin(), scheduledTask -> {
            if (!player.isOnline()) {
                scheduledTask.cancel();
                return;
            }

            int next = remaining[0] - 1;

            if (next <= 0) {
                scheduledTask.cancel();
                finishTeleport(player, location, worldName);
                return;
            }

            remaining[0] = next;
            MessageUtil.sendTitle(player, "queue_countdown_title", "queue_countdown_subtitle",
                    s -> s.replace("{seconds}", String.valueOf(remaining[0])));
            SoundUtil.play(player, "countdown");

        }, null, 20L, 20L);
    }

    private void finishTeleport(Player player, Location location, String worldName) {
        player.teleportAsync(location).thenAccept(success -> {
            if (!success) return;
            player.getScheduler().run(module.getPlugin(), t2 -> {
                MessageUtil.sendChatList(player, "teleport_success",
                        s -> s.replace("{world}", worldName));
                MessageUtil.sendActionbar(player, "teleport_success_ab");
                SoundUtil.play(player, "teleport_success");
            }, null);
        });
    }

    public void refreshViewers() {
        for (Map.Entry<UUID, Inventory> entry : Map.copyOf(mainViewers).entrySet()) {
            UUID id = entry.getKey();
            Inventory tracked = entry.getValue();
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                mainViewers.remove(id);
                continue;
            }
            player.getScheduler().run(module.getPlugin(), t -> {
                Inventory open = player.getOpenInventory().getTopInventory();
                if (open != tracked) {
                    mainViewers.remove(id);
                    return;
                }
                GuiUtil.updateDynamicSlots(module, open, player);
            }, null);
        }
    }

    public void clear() {
        queue.clear();
        selectedWorld.clear();
        mainViewers.clear();
        worldViewers.clear();
    }
}

