package me.vennlmao.ariscore.team.utils;

import me.vennlmao.ariscore.team.TeamModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.UnaryOperator;

public class MessageUtil {

    private static TeamModule module;

    public static void init(TeamModule m) { module = m; }

    public static void sendChat(CommandSender sender, String key) {
        sendChat(sender, key, s -> s);
    }

    public static void sendChat(CommandSender sender, String key, UnaryOperator<String> replacer) {
        String msg = module.getConfig().getString("messages." + key, "");
        if (msg.isEmpty()) return;
        String applied = replacer.apply(msg);
        sender.sendMessage(ColorUtil.parse(applied));
        if (sender instanceof Player player) player.sendActionBar(ColorUtil.parse(applied));
    }

    public static void sendActionbar(Player player, String key) {
        sendActionbar(player, key, s -> s);
    }

    public static void sendActionbar(Player player, String key, UnaryOperator<String> replacer) {
        String msg = module.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) player.sendActionBar(ColorUtil.parse(replacer.apply(msg)));
    }
}
