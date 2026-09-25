package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.utils.OrdersState;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class YourOrdersHolder implements InventoryHolder {
   private final OrdersState state;

   public YourOrdersHolder(OrdersState state) {
      this.state = state;
   }

   public OrdersState getState() {
      return this.state;
   }

   @NotNull
   public Inventory getInventory() {
      throw new UnsupportedOperationException("YourOrdersHolder does not store an inventory instance");
   }
}
