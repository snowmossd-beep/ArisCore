package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.utils.OrdersState;
import me.vennlmao.ariscore.order.utils.SelectItemState;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class SelectItemHolder implements InventoryHolder {
   private final OrdersState ordersState;
   private final SelectItemState selectItemState;

   public SelectItemHolder(OrdersState ordersState, SelectItemState selectItemState) {
      this.ordersState = ordersState;
      this.selectItemState = selectItemState;
   }

   public OrdersState getOrdersState() {
      return this.ordersState;
   }

   public SelectItemState getSelectItemState() {
      return this.selectItemState;
   }

   @NotNull
   public Inventory getInventory() {
      throw new UnsupportedOperationException("SelectItemHolder does not store an inventory instance");
   }
}
