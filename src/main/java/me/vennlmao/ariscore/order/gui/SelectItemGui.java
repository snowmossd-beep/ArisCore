package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.utils.OrdersState;
import me.vennlmao.ariscore.order.utils.SelectItemState;

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

public final class SelectItemGui {
   private static final TextColor GREEN = TextColor.fromHexString("#08FB7B");
   private final GuiManager gui;

   public SelectItemGui(GuiManager gui) {
      this.gui = gui;
   }

   public Inventory create(OrdersState ordersState, SelectItemState state, List<Material> materials) {
      Component title = this.noItalic(Component.text(this.gui.selectItemGuiTitle(), NamedTextColor.DARK_GRAY));
      int size = this.gui.selectItemGuiSize();
      int maxSlot = this.gui.selectItemGuiItemsMaxSlot();
      Inventory inv = Bukkit.createInventory(new SelectItemHolder(ordersState, state), size, title);

      for (int i = 0; i < Math.min(maxSlot + 1, materials.size()); i++) {
         inv.setItem(i, new ItemStack(materials.get(i)));
      }

      inv.setItem(this.gui.selectItemGuiFillerSlot(), this.filler());
      List<String> sorts = this.gui.selectItemGuiSorts();
      List<String> filters = this.gui.selectItemGuiFilters();
      inv.setItem(
         this.gui.selectItemGuiSlot("back-button", 45),
         this.btn(
            this.gui.selectItemGuiMat("back-button", Material.ARROW),
            this.gui.selectItemGuiName("back-button"),
            this.gui.selectItemGuiLore("back-button")
         )
      );
      inv.setItem(this.gui.selectItemGuiSlot("sort-button", 48), this.sortItem(state, sorts));
      inv.setItem(this.gui.selectItemGuiSlot("filter-button", 49), this.filterItem(state, filters));
      inv.setItem(
         this.gui.selectItemGuiSlot("search-button", 50),
         this.btn(
            this.gui.selectItemGuiMat("search-button", Material.OAK_SIGN),
            this.gui.selectItemGuiName("search-button"),
            this.gui.selectItemGuiLore("search-button")
         )
      );
      inv.setItem(
         this.gui.selectItemGuiSlot("next-button", 53),
         this.btn(
            this.gui.selectItemGuiMat("next-button", Material.ARROW),
            this.gui.selectItemGuiName("next-button"),
            this.gui.selectItemGuiLore("next-button")
         )
      );
      return inv;
   }

   public int getItemsMaxSlot() {
      return this.gui.selectItemGuiItemsMaxSlot();
   }

   private ItemStack filler() {
      ItemStack it = new ItemStack(this.gui.selectItemGuiFillerMat());
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(" ")));
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack sortItem(SelectItemState state, List<String> sorts) {
      List<Component> lore = new ArrayList<>();

      for (int i = 0; i < sorts.size(); i++) {
         boolean selected = i == this.normalizeIndex(state.getSortIndex(), sorts.size());
         lore.add(
            Component.text(this.gui.selectItemGuiBullet(), (TextColor)(selected ? GREEN : NamedTextColor.WHITE))
               .append(Component.text(sorts.get(i), (TextColor)(selected ? GREEN : NamedTextColor.WHITE)))
         );
      }

      return this.btnC(this.gui.selectItemGuiMat("sort-button", Material.CAULDRON), this.gui.selectItemGuiName("sort-button"), lore);
   }

   private ItemStack filterItem(SelectItemState state, List<String> filters) {
      List<Component> lore = new ArrayList<>();

      for (int i = 0; i < filters.size(); i++) {
         boolean selected = i == this.normalizeIndex(state.getFilterIndex(), filters.size());
         lore.add(
            Component.text(this.gui.selectItemGuiBullet(), (TextColor)(selected ? GREEN : NamedTextColor.WHITE))
               .append(Component.text(filters.get(i), (TextColor)(selected ? GREEN : NamedTextColor.WHITE)))
         );
      }

      return this.btnC(this.gui.selectItemGuiMat("filter-button", Material.HOPPER), this.gui.selectItemGuiName("filter-button"), lore);
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
