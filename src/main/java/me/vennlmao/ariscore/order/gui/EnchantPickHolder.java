package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.utils.EnchantPickState;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class EnchantPickHolder implements InventoryHolder {
   private final Material material;
   private final EnchantPickState state;

   public EnchantPickHolder(Material material, EnchantPickState state) {
      this.material = material;
      this.state = state;
   }

   public Material getMaterial() {
      return this.material;
   }

   public EnchantPickState getState() {
      return this.state;
   }

   @NotNull
   public Inventory getInventory() {
      throw new UnsupportedOperationException();
   }
}
