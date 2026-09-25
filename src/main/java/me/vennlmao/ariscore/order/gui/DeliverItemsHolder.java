package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.utils.OrderEntry;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class DeliverItemsHolder implements InventoryHolder {
   private final OrderEntry entry;

   public DeliverItemsHolder(OrderEntry entry) {
      this.entry = entry;
   }

   public OrderEntry getEntry() {
      return this.entry;
   }

   @NotNull
   public Inventory getInventory() {
      throw new UnsupportedOperationException("DeliverItemsHolder does not store an inventory instance");
   }
}
