package me.vennlmao.ariscore.combat.commands;

import me.vennlmao.ariscore.combat.CombatModule;
import me.vennlmao.ariscore.combat.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CombatReloadCommand implements CommandExecutor {

    private final CombatModule module;

    public CombatReloadCommand(CombatModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String permission = module.getConfig().getString("combat.reload-permission", "ariscore.combat.reload");
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendChatList(sender, "no_permission");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            module.reload();
            MessageUtil.sendChatList(sender, "reload_success");
            return true;
        }

        module.reload();
        MessageUtil.sendChatList(sender, "reload_success");
        return true;
    }
}
