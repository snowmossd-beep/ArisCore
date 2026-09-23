package me.vennlmao.ariscore.tab.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class NameTagManager implements Listener {

    private static final String TEAM_PREFIX = "nt_";
    private static final String NAME_PLACEHOLDER = "%player_name%";
    private static final int MAX_LENGTH = 64;

    private final ArisCore plugin;
    private final PapiManager papi;
    private final TabConfigManager config;
    private final Scoreboard sharedScoreboard;

    private final Map<UUID, ScheduledTask> tasks   = new ConcurrentHashMap<>();
    private final Map<UUID, String>        lastTag = new ConcurrentHashMap<>();

    public NameTagManager(ArisCore plugin, PapiManager papi, TabConfigManager config, Scoreboard sharedScoreboard) {
        this.plugin = plugin;
        this.papi   = papi;
        this.config = config;
        this.sharedScoreboard = sharedScoreboard;
    }

    public void start() {
        if (!config.isNametagEnabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isRealPlayer(p)) schedule(p);
        }
    }

    public void stop() {
        tasks.values().forEach(t -> { try { t.cancel(); } catch (Throwable ignored) {} });
        tasks.clear();
        lastTag.clear();
    }

    public void reload() { stop(); start(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!config.isNametagEnabled()) return;
        Player player = e.getPlayer();
        if (!isRealPlayer(player)) return;
        player.getScheduler().runDelayed((Plugin) plugin, t -> schedule(player), () -> {}, 5L);
    }

    private boolean isRealPlayer(Player player) {
        return player.getAddress() != null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        ScheduledTask t = tasks.remove(id);
        if (t != null) try { t.cancel(); } catch (Throwable ignored) {}
        lastTag.remove(id);
    }

    private void schedule(Player player) {
        ScheduledTask old = tasks.remove(player.getUniqueId());
        if (old != null) try { old.cancel(); } catch (Throwable ignored) {}

        long ticks = Math.max(1L, config.getNametagUpdateTicks());
        ScheduledTask task = player.getScheduler().runAtFixedRate(
                (Plugin) plugin, t -> tick(player), () -> {}, 1L, ticks);
        if (task != null) tasks.put(player.getUniqueId(), task);
    }

    private void tick(Player player) {
        if (!player.isOnline()) return;

        if (sharedScoreboard != null && !sharedScoreboard.equals(player.getScoreboard())) {
            try {
                player.setScoreboard(sharedScoreboard);
            } catch (Throwable e) {
                plugin.getLogger().log(Level.WARNING, "[Tab/Nametag] Failed to assign shared scoreboard to "
                        + player.getName(), e);
            }
        }

        String template = config.getNametagTag();
        int idx = template.indexOf(NAME_PLACEHOLDER);
        String prefixTemplate = idx >= 0 ? template.substring(0, idx) : template;
        String suffixTemplate = idx >= 0 ? template.substring(idx + NAME_PLACEHOLDER.length()) : "";

        String prefix = truncate(papi.parse(player, prefixTemplate), MAX_LENGTH);
        String suffix = truncate(papi.parse(player, suffixTemplate), MAX_LENGTH);
        String combined = prefix + "\u0000" + suffix;

        UUID id = player.getUniqueId();

        boolean changed = !combined.equals(lastTag.get(id));
        if (!changed) return;

        lastTag.put(id, combined);

        applyToAllBoards(player, prefix, suffix);
    }

    private void applyToAllBoards(Player player, String prefix, String suffix) {
        try {
            if (!player.isOnline()) return;
            if (sharedScoreboard == null) return;

            String rawName   = player.getName();
            int maxLen       = 16 - TEAM_PREFIX.length();
            String shortName = rawName.length() > maxLen ? rawName.substring(0, maxLen) : rawName;
            String teamName  = TEAM_PREFIX + shortName;

            applyTeam(sharedScoreboard, teamName, rawName, prefix, suffix);

        } catch (Throwable e) {
            plugin.getLogger().log(Level.WARNING, "[Tab/Nametag] Failed to apply team for "
                    + player.getName(), e);
        }
    }

    private void applyTeam(Scoreboard sb, String teamName, String entry, String prefix, String suffix) {
        try {
            Team existingForEntry = sb.getEntryTeam(entry);
            if (existingForEntry != null && !existingForEntry.getName().equals(teamName)) {
                existingForEntry.removeEntry(entry);
            }

            Team team = sb.getTeam(teamName);
            if (team == null) team = sb.registerNewTeam(teamName);
            team.setPrefix(prefix != null ? prefix : "");
            team.setSuffix(suffix != null ? suffix : "");
            if (!team.hasEntry(entry)) team.addEntry(entry);
        } catch (Throwable e) {
            plugin.getLogger().log(Level.WARNING, "[Tab/Nametag] applyTeam failed for entry '" + entry + "'", e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
