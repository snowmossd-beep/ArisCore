package me.vennlmao.ariscore.order.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public final class OrderEntry {
   private final UUID owner;
   private final String ownerName;
   private final Material material;
   private final int amount;
   private final double pricePerItem;
   private int delivered;
   private int collected;
   private double paid;
   private final double paidByCreator;
   private long expiresAtMillis;
   private long dbId;
   private boolean completed;
   private final Map<Enchantment, Integer> enchantments;

   public OrderEntry(
      UUID owner,
      String ownerName,
      Material material,
      int amount,
      double pricePerItem,
      int delivered,
      int collected,
      double paid,
      double paidByCreator,
      long expiresAtMillis
   ) {
      this(owner, ownerName, material, amount, pricePerItem, delivered, collected, paid, paidByCreator, expiresAtMillis, -1L, Collections.emptyMap());
   }

   public OrderEntry(
      UUID owner,
      String ownerName,
      Material material,
      int amount,
      double pricePerItem,
      int delivered,
      int collected,
      double paid,
      double paidByCreator,
      long expiresAtMillis,
      long dbId
   ) {
      this(owner, ownerName, material, amount, pricePerItem, delivered, collected, paid, paidByCreator, expiresAtMillis, dbId, Collections.emptyMap());
   }

   public OrderEntry(
      UUID owner,
      String ownerName,
      Material material,
      int amount,
      double pricePerItem,
      int delivered,
      int collected,
      double paid,
      double paidByCreator,
      long expiresAtMillis,
      long dbId,
      Map<Enchantment, Integer> enchantments
   ) {
      this.owner = owner;
      this.ownerName = ownerName;
      this.material = material;
      this.amount = amount;
      this.pricePerItem = pricePerItem;
      this.delivered = delivered;
      this.collected = collected;
      this.paid = paid;
      this.paidByCreator = paidByCreator;
      this.expiresAtMillis = expiresAtMillis;
      this.dbId = dbId;
      this.enchantments = enchantments != null ? new LinkedHashMap<>(enchantments) : new LinkedHashMap<>();
   }

   public UUID getOwner() {
      return this.owner;
   }

   public String getOwnerName() {
      return this.ownerName;
   }

   public Material getMaterial() {
      return this.material;
   }

   public int getAmount() {
      return this.amount;
   }

   public double getPricePerItem() {
      return this.pricePerItem;
   }

   public synchronized int getDelivered() {
      return this.delivered;
   }

   public synchronized void setDelivered(int delivered) {
      this.delivered = delivered;
   }

   public synchronized double getPaid() {
      return this.paid;
   }

   public synchronized void setPaid(double paid) {
      this.paid = paid;
   }

   public double getPaidByCreator() {
      return this.paidByCreator;
   }

   public synchronized int getCollected() {
      return this.collected;
   }

   public synchronized void setCollected(int collected) {
      this.collected = Math.max(0, collected);
   }

   public synchronized long getExpiresAtMillis() {
      return this.expiresAtMillis;
   }

   public synchronized void setExpiresAtMillis(long expiresAtMillis) {
      this.expiresAtMillis = expiresAtMillis;
   }

   public synchronized long getDbId() {
      return this.dbId;
   }

   public synchronized void setDbId(long dbId) {
      this.dbId = dbId;
   }

   public synchronized boolean isCompleted() {
      return this.completed;
   }

   public synchronized void setCompleted(boolean completed) {
      this.completed = completed;
   }

   public synchronized int applyDelivery(int amount, double payForApplied, long completedRetentionMillis) {
      if (amount > 0 && !this.completed) {
         int remaining = this.amount - this.delivered;
         if (remaining <= 0) {
            return 0;
         } else {
            int applied = Math.min(amount, remaining);
            double payRatio = amount == applied ? 1.0 : (double)applied / amount;
            this.delivered += applied;
            this.paid += payForApplied * payRatio;
            if (this.delivered >= this.amount) {
               this.completed = true;
               this.expiresAtMillis = System.currentTimeMillis() + completedRetentionMillis;
            }

            return applied;
         }
      } else {
         return 0;
      }
   }

   public Map<Enchantment, Integer> getEnchantments() {
      return Collections.unmodifiableMap(this.enchantments);
   }
}
