package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.utils.OrdersState;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class NewOrderHolder implements InventoryHolder {
   private final OrdersState ordersState;

   public NewOrderHolder(OrdersState ordersState) {
      this.ordersState = ordersState;
   }

   public OrdersState getOrdersState() {
      return this.ordersState;
   }

   @NotNull
   public Inventory getInventory() {
      throw new UnsupportedOperationException("NewOrderHolder does not store an inventory instance");
   }
}
