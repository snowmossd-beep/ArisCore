package me.vennlmao.ariscore.order.utils;

import java.util.Locale;
import org.bukkit.Material;

public final class MaterialFilters {
   private MaterialFilters() {
   }

   public static boolean matchesFilter(Material material, int filterIndex) {
      if (material == null) {
         return false;
      } else {
         return switch (normalize(filterIndex, 9)) {
            case 0 -> true;
            case 1 -> material.isBlock();
            case 2 -> isTool(material);
            case 3 -> material.isEdible();
            case 4 -> isCombat(material);
            case 5 -> isPotion(material);
            case 6 -> isBook(material);
            case 7 -> isIngredient(material);
            case 8 -> !material.isAir();
            default -> true;
         };
      }
   }

   public static boolean matchesQuery(Material material, String query) {
      if (query != null && !query.trim().isEmpty()) {
         String q = query.trim().toLowerCase(Locale.ROOT);
         return material.name().toLowerCase(Locale.ROOT).contains(q);
      } else {
         return true;
      }
   }

   public static int normalize(int index, int size) {
      if (size <= 0) {
         return 0;
      } else {
         int mod = index % size;
         return mod < 0 ? mod + size : mod;
      }
   }

   private static boolean isTool(Material m) {
      String n = m.name();
      return n.endsWith("_PICKAXE")
         || n.endsWith("_AXE")
         || n.endsWith("_SHOVEL")
         || n.endsWith("_HOE")
         || n.endsWith("_SHEARS")
         || n.endsWith("_FISHING_ROD")
         || n.equals("FLINT_AND_STEEL")
         || n.equals("BUCKET")
         || n.endsWith("_BUCKET")
         || n.equals("COMPASS")
         || n.equals("CLOCK");
   }

   private static boolean isCombat(Material m) {
      String n = m.name();
      return n.endsWith("_SWORD")
         || n.endsWith("_AXE")
         || n.equals("BOW")
         || n.equals("CROSSBOW")
         || n.equals("TRIDENT")
         || n.equals("SHIELD")
         || n.endsWith("_HELMET")
         || n.endsWith("_CHESTPLATE")
         || n.endsWith("_LEGGINGS")
         || n.endsWith("_BOOTS")
         || n.equals("ARROW")
         || n.equals("SPECTRAL_ARROW")
         || n.equals("TIPPED_ARROW");
   }

   private static boolean isPotion(Material m) {
      String n = m.name();
      return n.equals("POTION") || n.equals("SPLASH_POTION") || n.equals("LINGERING_POTION") || n.equals("TIPPED_ARROW");
   }

   private static boolean isBook(Material m) {
      String n = m.name();
      return n.equals("BOOK") || n.equals("ENCHANTED_BOOK") || n.equals("WRITTEN_BOOK") || n.equals("WRITABLE_BOOK");
   }

   private static boolean isIngredient(Material m) {
      String n = m.name();
      return n.contains("_INGOT")
         || n.contains("_NUGGET")
         || n.contains("_GEM")
         || n.contains("_DUST")
         || n.contains("_SEED")
         || n.contains("_SEEDS")
         || n.contains("_ESSENCE")
         || n.contains("_POWDER")
         || n.contains("_SHARD")
         || n.equals("BLAZE_ROD")
         || n.equals("BLAZE_POWDER")
         || n.equals("GUNPOWDER")
         || n.equals("STRING")
         || n.equals("FEATHER")
         || n.equals("SLIME_BALL")
         || n.equals("ENDER_PEARL")
         || n.equals("NETHER_WART")
         || n.equals("SUGAR")
         || n.equals("WHEAT")
         || n.equals("EGG")
         || n.equals("MILK_BUCKET")
         || n.equals("LEATHER")
         || n.equals("RABBIT_FOOT")
         || n.equals("SPIDER_EYE")
         || n.equals("GHAST_TEAR")
         || n.equals("MAGMA_CREAM")
         || n.equals("GOLDEN_CARROT")
         || n.equals("GLISTERING_MELON_SLICE")
         || n.equals("FERMENTED_SPIDER_EYE");
   }
}
