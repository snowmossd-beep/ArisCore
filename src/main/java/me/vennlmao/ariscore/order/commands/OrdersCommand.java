package me.vennlmao.ariscore.order.commands;

import me.vennlmao.ariscore.order.OrderModule;
import me.vennlmao.ariscore.order.managers.OrdersService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class OrdersCommand implements CommandExecutor {
   private final OrdersService ordersService;
   private final OrderModule module;

   public OrdersCommand(OrdersService ordersService, OrderModule module) {
      this.ordersService = ordersService;
      this.module = module;
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
         if (!sender.hasPermission("ariscore.order.admin")) {
            sender.sendMessage(this.ordersService.noPermissionMessage());
            return true;
         }

         this.module.reload();
         sender.sendMessage(this.ordersService.reloadedMessage());
         return true;
      } else if (sender instanceof Player player) {
         this.ordersService.resetSession(player);
         this.ordersService.openOrders(player);
         return true;
      } else {
         return true;
      }
   }
                 }
