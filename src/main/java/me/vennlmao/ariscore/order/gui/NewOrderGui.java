package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.utils.NewOrderState;
import me.vennlmao.ariscore.order.utils.OrdersState;
import me.vennlmao.ariscore.order.utils.TextFormat;

import java.util.ArrayList;
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

public final class NewOrderGui {
   private static final TextColor GREEN = TextColor.fromHexString("#08FB7B");
   private static final TextColor CONFIRM_GREEN = TextColor.fromHexString("#3CFF3C");
   private static final TextColor CANCEL_RED = TextColor.fromHexString("#B00000");
   private final GuiManager gui;

   public NewOrderGui(GuiManager gui) {
      this.gui = gui;
   }

   public Inventory create(OrdersState ordersState, NewOrderState state) {
      Component title = this.noItalic(Component.text(this.gui.newOrderGuiTitle(), NamedTextColor.DARK_GRAY));
      Inventory inv = Bukkit.createInventory(new NewOrderHolder(ordersState), this.gui.newOrderGuiSize(), title);
      inv.setItem(this.gui.newOrderGuiCancelSlot(), this.cancelItem());
      inv.setItem(this.gui.newOrderGuiItemSlot(), this.itemItem(state));
      inv.setItem(this.gui.newOrderGuiAmountSlot(), this.amountItem(state));
      inv.setItem(this.gui.newOrderGuiPriceSlot(), this.priceItem(state));
      inv.setItem(this.gui.newOrderGuiConfirmSlot(), this.confirmItem(state));
      return inv;
   }

   private ItemStack cancelItem() {
      ItemStack it = new ItemStack(this.gui.newOrderGuiCancelMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.newOrderGuiCancelName(), CANCEL_RED)));
      meta.lore(this.toLore(this.gui.newOrderGuiCancelLore()));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack itemItem(NewOrderState state) {
      Material selected = state.getMaterial() == null ? this.gui.newOrderGuiItemFallbackMat() : state.getMaterial();
      ItemStack it = new ItemStack(selected);
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.newOrderGuiItemName(), GREEN)));
      List<String> loreBase = this.gui.newOrderGuiItemLore();
      List<Component> lore = new ArrayList<>(this.toLore(loreBase));
      lore.add(this.noItalic(Component.text("(" + TextFormat.materialNiceName(selected) + ")", NamedTextColor.GRAY)));
      meta.lore(lore);
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack amountItem(NewOrderState state) {
      ItemStack it = new ItemStack(this.gui.newOrderGuiAmountMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.newOrderGuiAmountName(), GREEN)));
      List<Component> lore = new ArrayList<>(this.toLore(this.gui.newOrderGuiAmountLore()));
      lore.add(this.noItalic(Component.text("(" + state.getAmount() + ")", NamedTextColor.GRAY)));
      meta.lore(lore);
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack priceItem(NewOrderState state) {
      ItemStack it = new ItemStack(this.gui.newOrderGuiPriceMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.newOrderGuiPriceName(), GREEN)));
      List<Component> lore = new ArrayList<>(this.toLore(this.gui.newOrderGuiPriceLore()));
      lore.add(this.noItalic(Component.text(this.gui.newOrderGuiPriceLore(TextFormat.money(state.getPricePerItem())), NamedTextColor.GRAY)));
      meta.lore(lore);
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack confirmItem(NewOrderState state) {
      ItemStack it = new ItemStack(this.gui.newOrderGuiConfirmMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.newOrderGuiConfirmName(), CONFIRM_GREEN)));
      double total = state.getAmount() * state.getPricePerItem();
      List<Component> lore = new ArrayList<>(this.toLore(this.gui.newOrderGuiConfirmLore()));
      lore.add(this.noItalic(Component.text(this.gui.newOrderGuiTotalLore(TextFormat.money(total)), NamedTextColor.GRAY)));
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
