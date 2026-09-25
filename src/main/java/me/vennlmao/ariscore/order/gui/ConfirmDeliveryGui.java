package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.utils.OrderEntry;
import me.vennlmao.ariscore.order.utils.TextFormat;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ConfirmDeliveryGui {
   private static final TextColor CONFIRM_GREEN = TextColor.fromHexString("#3CFF3C");
   private static final TextColor CANCEL_RED = TextColor.fromHexString("#B00000");
   private static final TextColor ITEM_GREEN = TextColor.fromHexString("#08FB7B");
   private final GuiManager gui;
   private final YourOrdersGui yourOrdersGui;

   public ConfirmDeliveryGui(GuiManager gui, YourOrdersGui yourOrdersGui) {
      this.gui = gui;
      this.yourOrdersGui = yourOrdersGui;
   }

   public Inventory create(OrderEntry entry, int deliverAmount) {
      Component title = this.noItalic(Component.text(this.gui.confirmDeliveryGuiTitle(), NamedTextColor.DARK_GRAY));
      Inventory inv = Bukkit.createInventory(new ConfirmDeliveryHolder(entry, deliverAmount), this.gui.confirmDeliveryGuiSize(), title);
      inv.setItem(this.gui.confirmDeliveryGuiCancelSlot(), this.cancelItem());
      inv.setItem(this.gui.confirmDeliveryGuiPreviewSlot(), this.previewItem(entry, deliverAmount));
      inv.setItem(this.gui.confirmDeliveryGuiConfirmSlot(), this.confirmItem(entry, deliverAmount));
      return inv;
   }

   private ItemStack cancelItem() {
      ItemStack it = new ItemStack(this.gui.confirmDeliveryGuiCancelMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.confirmDeliveryGuiCancelName(), CANCEL_RED)));
      meta.lore(this.toLore(this.gui.confirmDeliveryGuiCancelLore()));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack previewItem(OrderEntry entry, int deliverAmount) {
      ItemStack base = this.yourOrdersGui.orderItem(entry);
      ItemMeta meta = base.getItemMeta();
      List<Component> lore = meta.lore();
      List<Component> newLore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
      if (!newLore.isEmpty()) {
         newLore.remove(newLore.size() - 1);
      }

      String itemName = TextFormat.materialNiceName(entry.getMaterial());
      Component deliveryLine = this.noItalic(
         Component.text(this.gui.confirmDeliveryGuiDeliveringLore(String.valueOf(deliverAmount), itemName), NamedTextColor.GRAY)
      );
      newLore.add(deliveryLine);
      meta.lore(newLore);
      base.setItemMeta(meta);
      return base;
   }

   private ItemStack confirmItem(OrderEntry entry, int deliverAmount) {
      double totalEarned = deliverAmount * entry.getPricePerItem();
      ItemStack it = new ItemStack(this.gui.confirmDeliveryGuiConfirmMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.confirmDeliveryGuiConfirmName(), CONFIRM_GREEN)));
      List<Component> lore = new ArrayList<>(this.toLore(this.gui.confirmDeliveryGuiConfirmLore()));
      lore.add(this.noItalic(Component.text(this.gui.confirmDeliveryGuiPriceLore(TextFormat.money(totalEarned)), NamedTextColor.GRAY)));
      meta.lore(lore);
      it.setItemMeta(meta);
      return it;
   }

   private List<Component> toLore(List<String> lines) {
      return lines.stream().map(l -> this.noItalic(Component.text(l, NamedTextColor.WHITE))).toList();
   }

   private Component noItalic(Component c) {
      return c.decoration(TextDecoration.ITALIC, false);
   }
}
