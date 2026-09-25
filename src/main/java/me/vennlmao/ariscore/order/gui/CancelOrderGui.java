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

public final class CancelOrderGui {
   private static final TextColor CONFIRM_GREEN = TextColor.fromHexString("#3CFF3C");
   private static final TextColor CANCEL_RED = TextColor.fromHexString("#B00000");
   private final GuiManager gui;
   private final YourOrdersGui yourOrdersGui;

   public CancelOrderGui(GuiManager gui, YourOrdersGui yourOrdersGui) {
      this.gui = gui;
      this.yourOrdersGui = yourOrdersGui;
   }

   public Inventory create(OrderEntry entry) {
      Component title = this.noItalic(Component.text(this.gui.cancelOrderGuiTitle(), NamedTextColor.DARK_GRAY));
      Inventory inv = Bukkit.createInventory(new CancelOrderHolder(entry), this.gui.cancelOrderGuiSize(), title);
      inv.setItem(this.gui.cancelOrderGuiBackSlot(), this.backItem());
      inv.setItem(this.gui.cancelOrderGuiOrderItemSlot(), this.orderItem(entry));
      inv.setItem(this.gui.cancelOrderGuiConfirmSlot(), this.confirmItem());
      return inv;
   }

   private ItemStack backItem() {
      ItemStack it = new ItemStack(this.gui.cancelOrderGuiBackMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.cancelOrderGuiBackName(), CANCEL_RED)));
      meta.lore(this.toLore(this.gui.cancelOrderGuiBackLore()));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack confirmItem() {
      ItemStack it = new ItemStack(this.gui.cancelOrderGuiConfirmMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.cancelOrderGuiConfirmName(), CONFIRM_GREEN)));
      meta.lore(this.toLore(this.gui.cancelOrderGuiConfirmLore()));
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
