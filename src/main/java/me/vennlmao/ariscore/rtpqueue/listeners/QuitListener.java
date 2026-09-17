package me.vennlmao.ariscore.rtpqueue.listeners;

import me.vennlmao.ariscore.rtpqueue.RtpQueueModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    private final RtpQueueModule module;

    public QuitListener(RtpQueueModule module) {
        this.module = module;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        module.getQueueManager().removeSilently(event.getPlayer().getUniqueId());
    }
}
