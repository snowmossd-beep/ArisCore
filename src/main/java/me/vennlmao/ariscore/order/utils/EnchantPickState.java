package me.vennlmao.ariscore.order.utils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public final class EnchantPickState {
   private static final Set<Set<Enchantment>> INCOMPATIBLE_GROUPS = buildIncompatibleGroups();
   private final Map<Enchantment, Integer> selected = new LinkedHashMap<>();
   private int page = 0;

   public Map<Enchantment, Integer> getSelected() {
      return this.selected;
   }

   public void toggle(Enchantment ench, int level) {
      if (ench != null && level > 0) {
         Integer current = this.selected.get(ench);
         if (current != null && current == level) {
            this.selected.remove(ench);
         } else {
            if (!this.canSelect(ench)) {
               return;
            }

            this.selected.put(ench, level);
         }
      }
   }

   public boolean canSelect(Enchantment ench) {
      if (this.selected.containsKey(ench)) {
         return true;
      } else {
         for (Set<Enchantment> group : INCOMPATIBLE_GROUPS) {
            if (group.contains(ench)) {
               for (Enchantment chosen : this.selected.keySet()) {
                  if (group.contains(chosen)) {
                     return false;
                  }
               }
            }
         }

         return true;
      }
   }

   public boolean isSelected(Enchantment ench, int level) {
      return this.selected.getOrDefault(ench, 0) == level;
   }

   public int getPage() {
      return this.page;
   }

   public void setPage(int page) {
      this.page = Math.max(0, page);
   }

   private static Set<Set<Enchantment>> buildIncompatibleGroups() {
      Enchantment density = byName("DENSITY");
      return density == null
         ? Set.of(
            group(byName("SHARPNESS"), byName("SMITE"), byName("BANE_OF_ARTHROPODS")),
            group(byName("INFINITY"), byName("MENDING")),
            group(byName("PROTECTION"), byName("FIRE_PROTECTION"), byName("BLAST_PROTECTION"), byName("PROJECTILE_PROTECTION")),
            group(byName("DEPTH_STRIDER"), byName("FROST_WALKER")),
            group(byName("SOUL_SPEED"), byName("FROST_WALKER")),
            group(byName("PIERCING"), byName("MULTISHOT")),
            group(byName("LOOTING"), byName("SILK_TOUCH")),
            group(byName("RIPTIDE"), byName("CHANNELING"))
         )
         : Set.of(
            group(byName("SHARPNESS"), byName("SMITE"), byName("BANE_OF_ARTHROPODS"), density, byName("BREACH")),
            group(byName("INFINITY"), byName("MENDING")),
            group(byName("PROTECTION"), byName("FIRE_PROTECTION"), byName("BLAST_PROTECTION"), byName("PROJECTILE_PROTECTION")),
            group(byName("DEPTH_STRIDER"), byName("FROST_WALKER")),
            group(byName("SOUL_SPEED"), byName("FROST_WALKER")),
            group(byName("PIERCING"), byName("MULTISHOT")),
            group(byName("LOOTING"), byName("SILK_TOUCH")),
            group(byName("RIPTIDE"), byName("CHANNELING"))
         );
   }

   private static Enchantment byName(String name) {
      return name == null ? null : Enchantment.getByKey(NamespacedKey.fromString(name.toLowerCase()));
   }

   private static Set<Enchantment> group(Enchantment... enchantments) {
      LinkedHashSet<Enchantment> result = new LinkedHashSet<>();

      for (Enchantment enchantment : enchantments) {
         if (enchantment != null) {
            result.add(enchantment);
         }
      }

      return Set.copyOf(result);
   }
}
