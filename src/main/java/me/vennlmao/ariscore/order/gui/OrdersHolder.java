package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.utils.OrdersState;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class OrdersHolder implements InventoryHolder {
   private final OrdersState state;

   public OrdersHolder(OrdersState state) {
      this.state = state;
   }

   public OrdersState getState() {
      return this.state;
   }

   @NotNull
   public Inventory getInventory() {
      throw new UnsupportedOperationException("OrdersHolder does not store an inventory instance");
   }
}
