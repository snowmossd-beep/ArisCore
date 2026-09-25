package me.vennlmao.ariscore.order.utils;

import me.vennlmao.ariscore.order.OrderModule;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static OrderModule module;

    public static void init(OrderModule m) { module = m; }

    public static void play(Player player, String key) {
        play(player, key, Float.NaN);
    }

    public static void play(Player player, String key, float pitchOverride) {
        String soundName = module.getConfig().getString("sounds." + key + ".sound", "");
        float volume = (float) module.getConfig().getDouble("sounds." + key + ".volume", 1.0);
        float pitch = Float.isNaN(pitchOverride) ? (float) module.getConfig().getDouble("sounds." + key + ".pitch", 1.0) : pitchOverride;
        if (soundName.isEmpty()) return;
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
        } catch (IllegalArgumentException ignored) {}
    }
}
