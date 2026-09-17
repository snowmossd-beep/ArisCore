package me.vennlmao.ariscore.combat.utils;

import me.vennlmao.ariscore.combat.CombatModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.UnaryOperator;

public class MessageUtil {

    private static CombatModule module;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void init(CombatModule mod) {
        module = mod;
    }

    public static Component parse(String raw) {
        String s = raw
                .replaceAll("&#([0-9A-Fa-f]{6})", "<color:#$1>")
                .replace("&a", "<green>").replace("&b", "<aqua>").replace("&c", "<red>")
                .replace("&d", "<light_purple>").replace("&e", "<yellow>").replace("&f", "<white>")
                .replace("&7", "<gray>").replace("&6", "<gold>").replace("&4", "<dark_red>")
                .replace("&2", "<dark_green>").replace("&1", "<dark_blue>").replace("&9", "<blue>")
                .replace("&5", "<dark_purple>").replace("&3", "<dark_aqua>").replace("&0", "<black>")
                .replace("&8", "<dark_gray>").replace("&l", "<bold>").replace("&o", "<italic>")
                .replace("&n", "<underlined>").replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>").replace("&r", "<reset>");
        return MM.deserialize("<!italic>" + s);
    }

    public static void sendChatList(CommandSender sender, String key) {
        sendChatList(sender, key, s -> s);
    }

    public static void sendChatList(CommandSender sender, String key, UnaryOperator<String> replacer) {
        List<String> lines = module.getConfig().getStringList("messages." + key);
        for (String line : lines) {
            sender.sendMessage(parse(replacer.apply(line)));
        }
    }

    public static void sendActionbar(Player player, String key) {
        sendActionbar(player, key, s -> s);
    }

    public static void sendActionbar(Player player, String key, UnaryOperator<String> replacer) {
        String raw = module.getConfig().getString("messages." + key, "");
        if (raw.isEmpty()) return;
        player.sendActionBar(parse(replacer.apply(raw)));
    }
}
