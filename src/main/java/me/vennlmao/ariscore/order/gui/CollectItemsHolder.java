package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.utils.OrderEntry;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class CollectItemsHolder implements InventoryHolder {
   private final OrderEntry entry;
   private int page;

   public CollectItemsHolder(OrderEntry entry, int page) {
      this.entry = entry;
      this.page = page;
   }

   public OrderEntry getEntry() {
      return this.entry;
   }

   public int getPage() {
      return this.page;
   }

   public void setPage(int page) {
      this.page = page;
   }

   @NotNull
   public Inventory getInventory() {
      throw new UnsupportedOperationException("CollectItemsHolder does not store an inventory instance");
   }
}
