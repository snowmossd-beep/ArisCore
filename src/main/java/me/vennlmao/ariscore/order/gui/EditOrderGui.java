package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.utils.OrderEntry;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class EditOrderGui {
   private static final TextColor GREEN = TextColor.fromHexString("#08FB7B");
   private static final TextColor LIME = TextColor.fromHexString("#3CFF3C");
   private final GuiManager gui;
   private final YourOrdersGui yourOrdersGui;

   public EditOrderGui(GuiManager gui, YourOrdersGui yourOrdersGui) {
      this.gui = gui;
      this.yourOrdersGui = yourOrdersGui;
   }

   public Inventory create(OrderEntry entry) {
      Component title = this.noItalic(Component.text(this.gui.editOrderGuiTitle(), NamedTextColor.DARK_GRAY));
      Inventory inv = Bukkit.createInventory(new EditOrderHolder(entry), this.gui.editOrderGuiSize(), title);

      for (int slot : this.gui.editOrderGuiFillerSlots()) {
         inv.setItem(slot, this.filler());
      }

      inv.setItem(this.gui.editOrderGuiOrderItemSlot(), this.orderItem(entry));
      int cancelSlot = this.gui.editOrderGuiCancelSlot();
      int collectSlot = this.gui.editOrderGuiCollectSlot();
      if (entry.isCompleted()) {
         inv.setItem(cancelSlot, this.collectItem());
         inv.setItem(collectSlot, this.filler());
      } else {
         inv.setItem(cancelSlot, this.cancelItem());
         inv.setItem(collectSlot, this.collectItem());
      }

      return inv;
   }

   private ItemStack filler() {
      ItemStack it = new ItemStack(this.gui.editOrderGuiFillerMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(" ")));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack cancelItem() {
      ItemStack it = new ItemStack(this.gui.editOrderGuiCancelMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.editOrderGuiCancelName(), GREEN)));
      meta.lore(this.toLore(this.gui.editOrderGuiCancelLore()));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack collectItem() {
      ItemStack it = new ItemStack(this.gui.editOrderGuiCollectMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.editOrderGuiCollectName(), LIME)));
      meta.lore(this.toLore(this.gui.editOrderGuiCollectLore()));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack orderItem(OrderEntry entry) {
      return this.yourOrdersGui.orderItem(entry);
   }

   private List<Component> toLore(List<String> lines) {
      return lines.stream().map(l -> this.noItalic(Component.text(l, NamedTextColor.WHITE))).toList();
   }

   private Component noItalic(Component c) {
      return c.decoration(TextDecoration.ITALIC, false);
   }
}
