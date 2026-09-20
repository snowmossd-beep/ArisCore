package me.vennlmao.ariscore.team.commands;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.managers.TeamRole;
import me.vennlmao.ariscore.team.utils.ColorUtil;
import me.vennlmao.ariscore.team.utils.MessageUtil;
import me.vennlmao.ariscore.team.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final TeamModule module;

    public TeamCommand(TeamModule module) { this.module = module; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
            module.getGuiListener().openMain(player, 0);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "disband" -> handleDisband(player);
            case "invite" -> handleInvite(player, args);
            case "accept", "join" -> handleAccept(player, args);
            case "deny" -> handleDeny(player);
            case "leave" -> handleLeave(player);
            case "kick" -> handleKick(player, args);
            case "promote" -> handlePromote(player, args);
            case "demote" -> handleDemote(player, args);
            case "transfer" -> handleTransfer(player, args);
            case "sethome" -> handleSetHome(player);
            case "delhome" -> handleDelHome(player);
            case "home" -> handleHome(player);
            case "chat" -> handleChat(player);
            case "pvp" -> handlePvp(player);
            case "tag" -> handleTag(player, args);
            case "info" -> handleInfo(player);
            case "list" -> handleList(player);
            case "endchest", "ec" -> handleEnderChest(player);
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        MessageUtil.sendChat(player, "usage");
    }

    private TeamData requireTeam(Player player) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); }
        return team;
    }

    private boolean requirePermission(Player player, TeamData team, String permission) {
        TeamRole role = team.getRole(player.getUniqueId());
        if (role != null && role.has(module, permission)) return true;
        MessageUtil.sendChat(player, "no_permission");
        SoundUtil.play(player, "error");
        return false;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) { sendUsage(player); return; }
        String name = args[1];
        int min = module.getConfig().getInt("team.min-name-length", 3);
        int max = module.getConfig().getInt("team.max-name-length", 16);
        if (name.length() < min) { MessageUtil.sendChat(player, "name_too_short"); SoundUtil.play(player, "error"); return; }
        if (name.length() > max) { MessageUtil.sendChat(player, "name_too_long"); SoundUtil.play(player, "error"); return; }
        if (module.getTeamManager().hasTeam(player.getUniqueId())) { MessageUtil.sendChat(player, "already_in_team"); SoundUtil.play(player, "error"); return; }
        if (module.getTeamManager().teamExists(name)) { MessageUtil.sendChat(player, "name_taken"); SoundUtil.play(player, "error"); return; }

        module.getTeamManager().createTeam(player, name);
        MessageUtil.sendChat(player, "create", s -> s.replace("{team}", name));
        MessageUtil.sendActionbar(player, "create_ab");
        SoundUtil.play(player, "success");
    }

    private void handleDisband(Player player) {
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!team.isLeader(player.getUniqueId())) { MessageUtil.sendChat(player, "not_leader"); SoundUtil.play(player, "error"); return; }
        player.getScheduler().run(module.getPlugin(), t -> player.openInventory(module.getGuiBuilder().buildDisbandConfirm()), null);
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) { sendUsage(player); return; }
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!requirePermission(player, team, "can-invite")) return;

        if (team.getMemberCount() >= module.getConfig().getInt("team.max-members", 45)) {
            MessageUtil.sendChat(player, "team_full"); SoundUtil.play(player, "error"); return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendChat(player, "player_not_found"); SoundUtil.play(player, "error"); return; }
        if (team.isMember(target.getUniqueId())) { MessageUtil.sendChat(player, "already_member", s -> s.replace("{player}", target.getName())); return; }
        if (module.getTeamManager().hasTeam(target.getUniqueId())) { MessageUtil.sendChat(player, "target_has_team", s -> s.replace("{player}", target.getName())); return; }

        module.getTeamManager().addInvite(target.getUniqueId(), player.getUniqueId());
        MessageUtil.sendChat(player, "invite_sent", s -> s.replace("{player}", target.getName()));
        MessageUtil.sendActionbar(player, "invite_sent_ab");
        SoundUtil.play(player, "invite");
        MessageUtil.sendChat(target, "invite_received", s -> s.replace("{player}", player.getName()).replace("{team}", team.getName()));
        MessageUtil.sendActionbar(target, "invite_received_ab", s -> s.replace("{player}", player.getName()));
        SoundUtil.play(target, "invite");
    }

    private void handleAccept(Player player, String[] args) {
        if (module.getTeamManager().hasTeam(player.getUniqueId())) { MessageUtil.sendChat(player, "already_in_team"); SoundUtil.play(player, "error"); return; }

        String teamName = args.length >= 2 ? args[1] : module.getTeamManager().getInviteTeam(player.getUniqueId());
        if (teamName == null || !module.getTeamManager().hasInvite(player.getUniqueId(), teamName)) {
            MessageUtil.sendChat(player, "no_invite"); SoundUtil.play(player, "error"); return;
        }
        TeamData team = module.getTeamManager().getTeam(teamName);
        if (team == null) { MessageUtil.sendChat(player, "team_not_found"); return; }
        if (team.getMemberCount() >= module.getConfig().getInt("team.max-members", 45)) { MessageUtil.sendChat(player, "team_full"); SoundUtil.play(player, "error"); return; }

        module.getTeamManager().addMember(teamName, player);
        module.getTeamManager().removeInvite(player.getUniqueId());
        MessageUtil.sendChat(player, "join", s -> s.replace("{team}", teamName));
        MessageUtil.sendActionbar(player, "join_ab");
        SoundUtil.play(player, "success");
        for (UUID uuid : team.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && !m.equals(player)) MessageUtil.sendChat(m, "join_notify", s -> s.replace("{player}", player.getName()));
        }
    }

    private void handleDeny(Player player) {
        if (!module.getTeamManager().hasAnyInvite(player.getUniqueId())) { MessageUtil.sendChat(player, "no_invite"); SoundUtil.play(player, "error"); return; }
        module.getTeamManager().removeInvite(player.getUniqueId());
        MessageUtil.sendChat(player, "invite_denied");
        SoundUtil.play(player, "click");
    }

    private void handleLeave(Player player) {
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (team.isLeader(player.getUniqueId())) { MessageUtil.sendChat(player, "leader_must_transfer"); SoundUtil.play(player, "error"); return; }
        player.getScheduler().run(module.getPlugin(), t -> player.openInventory(module.getGuiBuilder().buildLeaveConfirm()), null);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) { sendUsage(player); return; }
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!requirePermission(player, team, "can-kick")) return;

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.isMember(target.getUniqueId())) { MessageUtil.sendChat(player, "player_not_found"); SoundUtil.play(player, "error"); return; }
        if (team.isLeader(target.getUniqueId())) { MessageUtil.sendChat(player, "cannot_kick_leader"); SoundUtil.play(player, "error"); return; }

        module.getTeamManager().removeMember(target.getUniqueId());
        MessageUtil.sendChat(player, "kick", s -> s.replace("{player}", target.getName()));
        MessageUtil.sendActionbar(player, "kick_ab");
        SoundUtil.play(player, "success");
        MessageUtil.sendChat(target, "kick_notify", s -> s.replace("{team}", team.getName()));
        SoundUtil.play(target, "error");
    }

    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) { sendUsage(player); return; }
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!team.isLeader(player.getUniqueId())) { MessageUtil.sendChat(player, "only_leader_can_promote"); SoundUtil.play(player, "error"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.isMember(target.getUniqueId())) { MessageUtil.sendChat(player, "player_not_found"); SoundUtil.play(player, "error"); return; }
        if (team.getRole(target.getUniqueId()) != TeamRole.MEMBER) { MessageUtil.sendChat(player, "already_admin"); SoundUtil.play(player, "error"); return; }

        module.getTeamManager().setRole(team, target.getUniqueId(), TeamRole.ADMIN);
        MessageUtil.sendChat(player, "promote", s -> s.replace("{player}", target.getName()));
        SoundUtil.play(player, "success");
        MessageUtil.sendChat(target, "promote_notify", s -> s.replace("{team}", team.getName()));
        SoundUtil.play(target, "success");
    }

    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) { sendUsage(player); return; }
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!team.isLeader(player.getUniqueId())) { MessageUtil.sendChat(player, "only_leader_can_demote"); SoundUtil.play(player, "error"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.isMember(target.getUniqueId())) { MessageUtil.sendChat(player, "player_not_found"); SoundUtil.play(player, "error"); return; }
        if (team.getRole(target.getUniqueId()) != TeamRole.ADMIN) { MessageUtil.sendChat(player, "already_not_admin"); SoundUtil.play(player, "error"); return; }

        module.getTeamManager().setRole(team, target.getUniqueId(), TeamRole.MEMBER);
        MessageUtil.sendChat(player, "demote", s -> s.replace("{player}", target.getName()));
        SoundUtil.play(player, "click");
        MessageUtil.sendChat(target, "demote_notify", s -> s.replace("{team}", team.getName()));
        SoundUtil.play(target, "error");
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) { sendUsage(player); return; }
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!team.isLeader(player.getUniqueId())) { MessageUtil.sendChat(player, "not_leader"); SoundUtil.play(player, "error"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.isMember(target.getUniqueId())) { MessageUtil.sendChat(player, "player_not_found"); SoundUtil.play(player, "error"); return; }
        if (target.getUniqueId().equals(player.getUniqueId())) { MessageUtil.sendChat(player, "cannot_transfer_self"); SoundUtil.play(player, "error"); return; }

        module.getTeamManager().transferLeadership(team, target.getUniqueId());
        MessageUtil.sendChat(player, "transfer", s -> s.replace("{player}", target.getName()));
        SoundUtil.play(player, "success");
        MessageUtil.sendChat(target, "transfer_notify", s -> s.replace("{team}", team.getName()));
        SoundUtil.play(target, "success");
    }

    private void handleSetHome(Player player) {
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!requirePermission(player, team, "can-sethome")) return;
        if (module.getConfig().getStringList("blocked_worlds").contains(player.getWorld().getName())) {
            MessageUtil.sendChat(player, "world_blocked"); MessageUtil.sendActionbar(player, "world_blocked_ab"); SoundUtil.play(player, "error"); return;
        }
        module.getTeamManager().setHome(team.getName(), player.getLocation());
        MessageUtil.sendChat(player, "sethome"); MessageUtil.sendActionbar(player, "sethome_ab"); SoundUtil.play(player, "success");
    }

    private void handleDelHome(Player player) {
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!requirePermission(player, team, "can-sethome")) return;
        module.getTeamManager().deleteHome(team.getName());
        MessageUtil.sendChat(player, "delhome"); MessageUtil.sendActionbar(player, "delhome_ab"); SoundUtil.play(player, "click");
    }

    private void handleHome(Player player) {
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (team.getHome() == null) { MessageUtil.sendChat(player, "home_not_set"); MessageUtil.sendActionbar(player, "home_not_set_ab"); SoundUtil.play(player, "error"); return; }
        if (!requirePermission(player, team, "can-visit-home")) return;
        module.getWarmupManager().startWarmup(player, team.getHome());
    }

    private void handleChat(Player player) {
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!requirePermission(player, team, "can-team-chat")) return;

        module.getChatListener().toggleChat(player);
        if (module.getChatListener().isChatEnabled(player.getUniqueId())) {
            MessageUtil.sendChat(player, "chat_toggle_on"); MessageUtil.sendActionbar(player, "chat_toggle_on_ab"); SoundUtil.play(player, "success");
        } else {
            MessageUtil.sendChat(player, "chat_toggle_off"); MessageUtil.sendActionbar(player, "chat_toggle_off_ab"); SoundUtil.play(player, "click");
        }
    }

    private void handlePvp(Player player) {
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!requirePermission(player, team, "can-toggle-pvp")) return;
        module.getTeamManager().setPvp(team.getName(), !team.isPvpEnabled());
        MessageUtil.sendChat(player, "pvp_toggled", s -> s.replace("{status}", team.isPvpEnabled() ? "ON" : "OFF"));
        SoundUtil.play(player, "click");
    }

    private void handleTag(Player player, String[] args) {
        if (args.length < 2) { sendUsage(player); return; }
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!team.isLeader(player.getUniqueId())) { MessageUtil.sendChat(player, "not_leader"); SoundUtil.play(player, "error"); return; }

        int maxTagLength = module.getConfig().getInt("team.max-tag-length", 6);
        String tag = args[1];
        if (tag.length() > maxTagLength) { MessageUtil.sendChat(player, "tag_too_long"); SoundUtil.play(player, "error"); return; }

        module.getTeamManager().setTag(team.getName(), tag);
        MessageUtil.sendChat(player, "tag_set", s -> s.replace("{tag}", tag));
        SoundUtil.play(player, "success");
    }

    private void handleInfo(Player player) {
        TeamData team = requireTeam(player);
        if (team == null) return;

        player.sendMessage(ColorUtil.parse("&8--- &6" + team.getName() + " &8---"));
        player.sendMessage(ColorUtil.parse("&7Members: &f" + team.getMemberCount() + "/" + module.getConfig().getInt("team.max-members", 45)));
        player.sendMessage(ColorUtil.parse("&7PVP: &f" + (team.isPvpEnabled() ? "&aON" : "&cOFF")));
        for (UUID uuid : team.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            String name = m != null ? m.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            TeamRole role = team.getRole(uuid);
            player.sendMessage(ColorUtil.parse("&7- &f" + name + " &8[&6" + (role != null ? role.display(module) : "?") + "&8]"));
        }
    }

    private void handleEnderChest(Player player) {
        TeamData team = requireTeam(player);
        if (team == null) return;
        if (!requirePermission(player, team, "can-open-endchest")) return;

        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(module.getTeamEnderChestManager().getInventory(team.getName())), null);
        SoundUtil.play(player, "click");
    }

    private void handleList(Player player) {
        List<TeamData> teams = new ArrayList<>(module.getTeamManager().getAllTeams());
        if (teams.isEmpty()) { MessageUtil.sendChat(player, "no_teams_exist"); return; }
        player.sendMessage(ColorUtil.parse("&8--- &6Teams &8---"));
        for (TeamData team : teams) {
            player.sendMessage(ColorUtil.parse("&7- &f" + team.getName() + " &8(&7" + team.getMemberCount() + "&8)"));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1)
            return Arrays.asList("create", "disband", "invite", "accept", "deny", "leave", "kick", "promote",
                            "demote", "transfer", "sethome", "delhome", "home", "chat", "pvp", "tag", "info", "list",
                            "endchest", "ec")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && List.of("invite", "kick", "promote", "demote", "transfer").contains(args[0].toLowerCase()))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
                }
                
