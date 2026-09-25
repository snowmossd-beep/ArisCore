package me.vennlmao.ariscore.order.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public final class NewOrderState {
   private Material material;
   private int amount;
   private double pricePerItem;
   private Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();

   public NewOrderState() {
      this.material = Material.STONE;
      this.amount = 1;
      this.pricePerItem = 0.0;
   }

   public Material getMaterial() {
      return this.material;
   }

   public void setMaterial(Material material) {
      this.material = material;
   }

   public int getAmount() {
      return this.amount;
   }

   public void setAmount(int amount) {
      this.amount = amount;
   }

   public double getPricePerItem() {
      return this.pricePerItem;
   }

   public void setPricePerItem(double pricePerItem) {
      this.pricePerItem = pricePerItem;
   }

   public Map<Enchantment, Integer> getEnchantments() {
      return this.enchantments;
   }

   public void setEnchantments(Map<Enchantment, Integer> enchantments) {
      this.enchantments = enchantments;
   }

   public void clearEnchantments() {
      this.enchantments.clear();
   }
}
