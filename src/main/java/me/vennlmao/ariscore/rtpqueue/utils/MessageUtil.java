package me.vennlmao.ariscore.rtpqueue.utils;

import me.vennlmao.ariscore.rtpqueue.RtpQueueModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.function.UnaryOperator;

public class MessageUtil {

    private static RtpQueueModule module;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void init(RtpQueueModule mod) {
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

    public static void sendChatList(Player player, String key) {
        sendChatList(player, key, s -> s);
    }

    public static void sendChatList(Player player, String key, UnaryOperator<String> replacer) {
        List<String> lines = module.getConfig().getStringList("messages." + key);
        for (String line : lines) {
            player.sendMessage(parse(replacer.apply(line)));
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

    public static String getString(String key, String def) {
        return module.getConfig().getString(key, def);
    }

    public static void sendTitle(Player player, String titleKey, String subtitleKey) {
        sendTitle(player, titleKey, subtitleKey, s -> s);
    }

    public static void sendTitle(Player player, String titleKey, String subtitleKey, UnaryOperator<String> replacer) {
        String titleRaw = module.getConfig().getString("titles." + titleKey, "");
        String subtitleRaw = module.getConfig().getString("titles." + subtitleKey, "");
        if (titleRaw.isEmpty() && subtitleRaw.isEmpty()) return;

        Component title = parse(replacer.apply(titleRaw));
        Component subtitle = parse(replacer.apply(subtitleRaw));

        int fadeIn = module.getConfig().getInt("title-defaults.fade-in", 5);
        int stay = module.getConfig().getInt("title-defaults.stay", 40);
        int fadeOut = module.getConfig().getInt("title-defaults.fade-out", 10);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L));

        player.showTitle(Title.title(title, subtitle, times));
    }
}
