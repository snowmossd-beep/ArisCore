package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.utils.OrderEntry;
import me.vennlmao.ariscore.order.utils.OrdersState;
import me.vennlmao.ariscore.order.utils.TextFormat;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class OrdersGui {
   private static final TextColor GREEN = TextColor.fromHexString("#08FB7B");
   private final GuiManager gui;
   private final YourOrdersGui yourOrdersGui;

   public OrdersGui(GuiManager gui, YourOrdersGui yourOrdersGui) {
      this.gui = gui;
      this.yourOrdersGui = yourOrdersGui;
   }

   public Inventory create(OrdersState state, List<OrderEntry> orders) {
      String rawTitle = this.gui.orderGuiTitle().replace("{page}", String.valueOf(state.getPage()));
      Component title = this.noItalic(Component.text(rawTitle, NamedTextColor.DARK_GRAY));
      int size = this.gui.orderGuiSize();
      int maxSlot = this.gui.orderGuiItemsMaxSlot();
      Inventory inv = Bukkit.createInventory(new OrdersHolder(state), size, title);
      if (orders != null && !orders.isEmpty()) {
         int slot = 0;

         for (OrderEntry entry : orders) {
            if (slot > maxSlot) {
               break;
            }

            inv.setItem(slot, this.orderItem(entry));
            slot++;
         }
      }

      List<String> sorts = this.gui.orderGuiSorts();
      List<String> filters = this.gui.orderGuiFilters();
      inv.setItem(
         this.gui.orderGuiSlot("back-button", 45),
         this.btn(this.gui.orderGuiMat("back-button", Material.ARROW), this.gui.orderGuiName("back-button"), this.gui.orderGuiLore("back-button"))
      );
      inv.setItem(this.gui.orderGuiSlot("sort-button", 47), this.sortItem(state, sorts));
      inv.setItem(this.gui.orderGuiSlot("filter-button", 48), this.filterItem(state, filters));
      inv.setItem(
         this.gui.orderGuiSlot("refresh-button", 49),
         this.btn(
            this.gui.orderGuiMat("refresh-button", Material.MAP), this.gui.orderGuiName("refresh-button"), this.gui.orderGuiLore("refresh-button")
         )
      );
      inv.setItem(
         this.gui.orderGuiSlot("search-button", 50),
         this.btn(
            this.gui.orderGuiMat("search-button", Material.OAK_SIGN), this.gui.orderGuiName("search-button"), this.gui.orderGuiLore("search-button")
         )
      );
      inv.setItem(
         this.gui.orderGuiSlot("your-orders-button", 51),
         this.btn(
            this.gui.orderGuiMat("your-orders-button", Material.CHEST),
            this.gui.orderGuiName("your-orders-button"),
            this.gui.orderGuiLore("your-orders-button")
         )
      );
      inv.setItem(
         this.gui.orderGuiSlot("next-button", 53),
         this.btn(this.gui.orderGuiMat("next-button", Material.ARROW), this.gui.orderGuiName("next-button"), this.gui.orderGuiLore("next-button"))
      );
      return inv;
   }

   private ItemStack orderItem(OrderEntry entry) {
      ItemStack base = this.yourOrdersGui.orderItem(entry);
      ItemMeta meta = base.getItemMeta();
      List<Component> lore = meta.lore();
      if (lore != null && !lore.isEmpty()) {
         int remaining = Math.max(0, entry.getAmount() - entry.getDelivered());
         String itemName = this.pluralize(TextFormat.materialNiceName(entry.getMaterial()), remaining);
         Component clickLine = Component.text(this.gui.ordersGuiDeliverLore(entry.getOwnerName(), itemName), NamedTextColor.WHITE);
         List<Component> newLore = new ArrayList<>(lore);
         int insertAt = Math.max(0, newLore.size() - 1);
         newLore.add(insertAt, clickLine.decoration(TextDecoration.ITALIC, false));
         meta.lore(newLore.stream().map(this::noItalic).toList());
         base.setItemMeta(meta);
         return base;
      } else {
         return base;
      }
   }

   private String pluralize(String name, int count) {
      if (name == null) {
         return "";
      } else if (count == 1) {
         return name;
      } else {
         String n = name.trim();
         if (n.isEmpty()) {
            return n;
         } else {
            return !n.endsWith("s") && !n.endsWith("x") && !n.endsWith("z") && !n.endsWith("ch") && !n.endsWith("sh") ? n + "s" : n + "es";
         }
      }
   }

   private ItemStack sortItem(OrdersState state, List<String> sorts) {
      List<Component> lore = new ArrayList<>();

      for (int i = 0; i < sorts.size(); i++) {
         boolean selected = i == this.normalizeIndex(state.getSortIndex(), sorts.size());
         lore.add(
            Component.text(this.gui.ordersGuiBullet(), (TextColor)(selected ? GREEN : NamedTextColor.WHITE))
               .append(Component.text(sorts.get(i), (TextColor)(selected ? GREEN : NamedTextColor.WHITE)))
         );
      }

      return this.btnC(this.gui.orderGuiMat("sort-button", Material.CAULDRON), this.gui.orderGuiName("sort-button"), lore);
   }

   private ItemStack filterItem(OrdersState state, List<String> filters) {
      List<Component> lore = new ArrayList<>();

      for (int i = 0; i < filters.size(); i++) {
         boolean selected = i == this.normalizeIndex(state.getFilterIndex(), filters.size());
         lore.add(
            Component.text(this.gui.ordersGuiBullet(), (TextColor)(selected ? GREEN : NamedTextColor.WHITE))
               .append(Component.text(filters.get(i), (TextColor)(selected ? GREEN : NamedTextColor.WHITE)))
         );
      }

      return this.btnC(this.gui.orderGuiMat("filter-button", Material.HOPPER), this.gui.orderGuiName("filter-button"), lore);
   }

   private ItemStack btn(Material mat, String name, List<String> loreTxt) {
      List<Component> lore = loreTxt.stream().map(l -> (Component) Component.text(l, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)).toList();
      return this.btnC(mat, name, lore);
   }

   private ItemStack btnC(Material mat, String name, List<Component> lore) {
      ItemStack it = new ItemStack(mat);
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(name, GREEN)));
      meta.lore(lore.stream().map(this::noItalic).toList());
      it.setItemMeta(meta);
      return it;
   }

   private Component noItalic(Component c) {
      return c.decoration(TextDecoration.ITALIC, false);
   }

   private int normalizeIndex(int index, int size) {
      if (size <= 0) {
         return 0;
      } else {
         int mod = index % size;
         return mod < 0 ? mod + size : mod;
      }
   }
         }
         
