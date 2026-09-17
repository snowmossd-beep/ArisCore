package me.vennlmao.ariscore.combat.commands;

import me.vennlmao.ariscore.combat.CombatModule;
import me.vennlmao.ariscore.combat.utils.MessageUtil;
import me.vennlmao.ariscore.combat.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CombatTagCommand implements CommandExecutor, TabCompleter {

    private final CombatModule module;

    public CombatTagCommand(CombatModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String permission = module.getConfig().getString("combat.command-permission", "ariscore.combat.tag");
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendChatList(sender, "no_permission");
            return true;
        }

        if (args.length < 1) {
            MessageUtil.sendChatList(sender, "player_not_found");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendChatList(sender, "player_not_found");
            if (sender instanceof Player player) SoundUtil.play(player, "error");
            return true;
        }

        int defaultSeconds = module.getCombatManager().getDefaultSeconds();
        int seconds = defaultSeconds;

        if (args.length >= 2) {
            try {
                int parsed = Integer.parseInt(args[1]);
                if (parsed > 0) {
                    seconds = parsed;
                } else {
                    MessageUtil.sendChatList(sender, "invalid_seconds",
                            s -> s.replace("{seconds}", String.valueOf(defaultSeconds)));
                }
            } catch (NumberFormatException e) {
                MessageUtil.sendChatList(sender, "invalid_seconds",
                        s -> s.replace("{seconds}", String.valueOf(defaultSeconds)));
            }
        }

        module.getCombatManager().tag(target, seconds, true, sender);

        int finalSeconds = seconds;
        MessageUtil.sendChatList(sender, "admin_tag_sender",
                s -> s.replace("{player}", target.getName()).replace("{seconds}", String.valueOf(finalSeconds)));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String defaultSeconds = String.valueOf(module.getCombatManager().getDefaultSeconds());
            return Arrays.asList(defaultSeconds, "10", "15", "30").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
                                  }
