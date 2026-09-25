package me.vennlmao.ariscore.order.commands;

import me.vennlmao.ariscore.order.managers.OrdersService;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class OrdersCommand implements CommandExecutor {
   private final OrdersService ordersService;

   public OrdersCommand(OrdersService ordersService) {
      this.ordersService = ordersService;
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (sender instanceof Player player) {
         this.ordersService.resetSession(player);
         this.ordersService.openOrders(player);
         return true;
      } else {
         return true;
      }
   }
}
