package me.vennlmao.ariscore.order.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.bukkit.Material;

public final class TextFormat {
   private static final DecimalFormat MONEY = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.US));

   private TextFormat() {
   }

   public static String materialNiceName(Material material) {
      if (material == null) {
         return "";
      } else {
         String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
         StringBuilder sb = new StringBuilder();

         for (String part : parts) {
            if (!part.isEmpty()) {
               if (sb.length() > 0) {
                  sb.append(' ');
               }

               sb.append(Character.toUpperCase(part.charAt(0)));
               if (part.length() > 1) {
                  sb.append(part.substring(1));
               }
            }
         }

         return sb.toString();
      }
   }

   public static String money(double value) {
      if (!Double.isNaN(value) && !Double.isInfinite(value)) {
         double abs = Math.abs(value);
         if (abs >= 1.0E12) {
            return MONEY.format(value / 1.0E12) + "t";
         } else if (abs >= 1.0E9) {
            return MONEY.format(value / 1.0E9) + "b";
         } else if (abs >= 1000000.0) {
            return MONEY.format(value / 1000000.0) + "m";
         } else {
            return abs >= 1000.0 ? MONEY.format(value / 1000.0) + "k" : MONEY.format(value);
         }
      } else {
         return "0";
      }
   }

   public static Double parseCompactNumber(String input) {
      if (input == null) {
         return null;
      } else {
         String s = input.trim().toLowerCase(Locale.ROOT);
         if (s.isEmpty()) {
            return null;
         } else {
            double multiplier = 1.0;
            char last = s.charAt(s.length() - 1);
            if (last == 'k' || last == 'm' || last == 'b' || last == 't') {
               s = s.substring(0, s.length() - 1).trim();

               multiplier = switch (last) {
                  case 'b' -> 1.0E9;
                  case 'k' -> 1000.0;
                  case 'm' -> 1000000.0;
                  case 't' -> 1.0E12;
                  default -> 1.0;
               };
            }

            if (!s.matches("\\d+(\\.\\d+)?")) {
               return null;
            } else {
               try {
                  return Double.parseDouble(s) * multiplier;
               } catch (NumberFormatException var6) {
                  return null;
               }
            }
         }
      }
   }

   public static String timeLeft(long millisLeft) {
      long totalMinutes = Math.max(0L, millisLeft) / 60000L;
      long days = totalMinutes / 1440L;
      long hours = totalMinutes % 1440L / 60L;
      long minutes = totalMinutes % 60L;
      return days + "d " + hours + "h " + minutes + "m";
   }
}
