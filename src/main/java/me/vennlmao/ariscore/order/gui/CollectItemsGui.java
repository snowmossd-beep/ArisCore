package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.utils.OrderEntry;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CollectItemsGui {
   private static final TextColor GREEN = TextColor.fromHexString("#08FB7B");
   private final GuiManager gui;

   public CollectItemsGui(GuiManager gui) {
      this.gui = gui;
   }

   public Inventory create(OrderEntry entry, int page) {
      Component title = this.noItalic(Component.text(this.gui.collectItemsGuiTitle(), NamedTextColor.DARK_GRAY));
      int size = this.gui.collectItemsGuiSize();
      int pageSize = this.gui.collectItemsGuiPageSize();
      Inventory inv = Bukkit.createInventory(new CollectItemsHolder(entry, page), size, title);

      for (int i = pageSize; i < size; i++) {
         inv.setItem(i, this.filler());
      }

      int collectable = Math.max(0, entry.getDelivered() - entry.getCollected());
      int maxStack = entry.getMaterial().getMaxStackSize();
      int from = (page - 1) * pageSize * maxStack;
      int remaining = collectable - from;
      if (remaining > 0) {
         int slot = 0;

         for (int left = Math.min(remaining, pageSize * maxStack); left > 0 && slot < pageSize; slot++) {
            int stack = Math.min(left, maxStack);
            inv.setItem(slot, new ItemStack(entry.getMaterial(), stack));
            left -= stack;
         }
      }

      boolean hasPrev = page > 1;
      boolean hasNext = remaining > pageSize * maxStack;
      int backSlot = this.gui.collectItemsGuiBackSlot();
      int dropSlot = this.gui.collectItemsGuiDropSlot();
      int nextSlot = this.gui.collectItemsGuiNextSlot();
      inv.setItem(backSlot, hasPrev ? this.backItem() : this.filler());
      inv.setItem(dropSlot, this.dropLootItem());
      inv.setItem(nextSlot, hasNext ? this.nextItem() : this.filler());
      return inv;
   }

   public int getPageSize() {
      return this.gui.collectItemsGuiPageSize();
   }

   private ItemStack filler() {
      ItemStack it = new ItemStack(this.gui.collectItemsGuiFillerMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(" ")));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack backItem() {
      return this.btn(this.gui.collectItemsGuiBackMat(), this.gui.collectItemsGuiBackName(), this.gui.collectItemsGuiBackLore());
   }

   private ItemStack nextItem() {
      return this.btn(this.gui.collectItemsGuiNextMat(), this.gui.collectItemsGuiNextName(), this.gui.collectItemsGuiNextLore());
   }

   private ItemStack dropLootItem() {
      return this.btn(this.gui.collectItemsGuiDropMat(), this.gui.collectItemsGuiDropName(), this.gui.collectItemsGuiDropLore());
   }

   private ItemStack btn(Material mat, String name, List<String> loreTxt) {
      ItemStack it = new ItemStack(mat);
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(name, GREEN)));
      meta.lore(loreTxt.stream().map(l -> this.noItalic(Component.text(l, NamedTextColor.WHITE))).toList());
      it.setItemMeta(meta);
      return it;
   }

   private Component noItalic(Component c) {
      return c.decoration(TextDecoration.ITALIC, false);
   }
}
