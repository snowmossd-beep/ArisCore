package me.vennlmao.ariscore.rtpqueue.commands;

import me.vennlmao.ariscore.rtpqueue.RtpQueueModule;
import me.vennlmao.ariscore.rtpqueue.gui.GuiUtil;
import me.vennlmao.ariscore.rtpqueue.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RtpQueueCommand implements CommandExecutor {

    private final RtpQueueModule module;

    public RtpQueueCommand(RtpQueueModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        player.getScheduler().run(module.getPlugin(), t -> {
            var inv = GuiUtil.buildMainGui(module, player);
            player.openInventory(inv);
            module.getQueueManager().addMainViewer(player.getUniqueId(), inv);
            SoundUtil.play(player, "open_gui");
        }, null);

        return true;
    }
}
