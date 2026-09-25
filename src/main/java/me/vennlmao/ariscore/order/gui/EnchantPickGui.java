package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.utils.EnchantPickState;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public final class EnchantPickGui {
   private final GuiManager gui;

   public EnchantPickGui(GuiManager gui) {
      this.gui = gui;
   }

   public Inventory create(Material material, EnchantPickState state, List<EnchantPickGui.EnchantEntry> enchants) {
      Component title = Component.text(this.gui.enchantPickGuiTitle(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
      Inventory inv = Bukkit.createInventory(new EnchantPickHolder(material, state), 54, title);
      inv.setItem(0, this.previewItem(material, state.getSelected()));
      ItemStack panel = this.fillerPanel();
      inv.setItem(1, panel);
      inv.setItem(9, panel);
      inv.setItem(10, panel);
      inv.setItem(45, this.arrow(this.gui.enchantPickGuiBackName()));
      inv.setItem(53, this.arrow(this.gui.enchantPickGuiNextName()));
      inv.setItem(46, this.cancelItem());
      inv.setItem(52, this.confirmItem());
      int totalPages = this.getTotalPages(enchants);
      if (state.getPage() < 0) {
         state.setPage(0);
      } else if (state.getPage() > totalPages) {
         state.setPage(totalPages);
      }

      ItemStack bookTemplate = new ItemStack(Material.ENCHANTED_BOOK);
      EnchantmentStorageMeta templateMeta = (EnchantmentStorageMeta)bookTemplate.getItemMeta();
      templateMeta.lore(List.of((TextComponent)Component.text(this.gui.enchantPickGuiBookSelectLore(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
      bookTemplate.setItemMeta(templateMeta);
      int count = 0;

      for (int j = 0; j < enchants.size() && j < 7; j++) {
         int index = j > 1 ? state.getPage() * 5 + j : j;
         if (index >= enchants.size()) {
            break;
         }

         Enchantment enchantment = enchants.get(index).enchantment();
         if (enchantment != null) {
            boolean canSelect = state.canSelect(enchantment);

            for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
               ItemStack book = bookTemplate.clone();
               EnchantmentStorageMeta meta = (EnchantmentStorageMeta)book.getItemMeta();
               meta.addStoredEnchant(enchantment, level, false);
               if (state.isSelected(enchantment, level)) {
                  meta.lore(List.of((TextComponent)Component.text(this.gui.enchantPickGuiBookSelectedLore(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
               } else if (!canSelect) {
                  meta.lore(List.of((TextComponent)Component.text(this.gui.enchantPickGuiBookCannotAddLore(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
               }

               book.setItemMeta(meta);
               int slot = this.enchantSlot(count, level);
               if (slot >= 0 && slot < 54) {
                  inv.setItem(slot, book);
               }
            }

            count++;
         }
      }

      return inv;
   }

   public int getTotalPages(List<EnchantPickGui.EnchantEntry> enchants) {
      return Math.max(0, (enchants.size() - 2) / 5);
   }

   private int enchantSlot(int count, int level) {
      return switch (count) {
         case 0 -> 9 + level * 9;
         case 1 -> 10 + level * 9;
         case 2 -> 2 + level;
         case 3 -> 11 + level;
         case 4 -> 20 + level;
         case 5 -> 29 + level;
         case 6 -> 38 + level;
         default -> -1;
      };
   }

   private ItemStack previewItem(Material material, Map<Enchantment, Integer> selected) {
      ItemStack it = new ItemStack(material);
      ItemMeta meta = it.getItemMeta();

      for (Entry<Enchantment, Integer> e : selected.entrySet()) {
         meta.addEnchant(e.getKey(), e.getValue(), false);
      }

      it.setItemMeta(meta);
      return it;
   }

   private ItemStack fillerPanel() {
      ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta meta = it.getItemMeta();
      meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack arrow(String name) {
      ItemStack it = new ItemStack(Material.ARROW);
      ItemMeta meta = it.getItemMeta();
      meta.displayName(Component.text(name, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack cancelItem() {
      ItemStack it = new ItemStack(Material.RED_STAINED_GLASS_PANE);
      ItemMeta meta = it.getItemMeta();
      meta.displayName(Component.text(this.gui.enchantPickGuiCancelName(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
      meta.lore(List.of((TextComponent)Component.text(this.gui.enchantPickGuiCancelLore(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack confirmItem() {
      ItemStack it = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
      ItemMeta meta = it.getItemMeta();
      meta.displayName(Component.text(this.gui.enchantPickGuiConfirmName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
      meta.lore(List.of((TextComponent)Component.text(this.gui.enchantPickGuiConfirmLore(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
      it.setItemMeta(meta);
      return it;
   }

   public record EnchantEntry(Enchantment enchantment, int level) {
   }
   }
            
