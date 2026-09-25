package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.utils.OrderEntry;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class ConfirmDeliveryHolder implements InventoryHolder {
   private final OrderEntry entry;
   private final int deliverAmount;

   public ConfirmDeliveryHolder(OrderEntry entry, int deliverAmount) {
      this.entry = entry;
      this.deliverAmount = deliverAmount;
   }

   public OrderEntry getEntry() {
      return this.entry;
   }

   public int getDeliverAmount() {
      return this.deliverAmount;
   }

   @NotNull
   public Inventory getInventory() {
      throw new UnsupportedOperationException("ConfirmDeliveryHolder does not store an inventory instance");
   }
}
