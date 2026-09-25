package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.utils.OrderEntry;
import me.vennlmao.ariscore.order.utils.OrdersState;
import me.vennlmao.ariscore.order.utils.TextFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class YourOrdersGui {
   private static final TextColor GREEN = TextColor.fromHexString("#08FB7B");
   private static final TextColor DARK_YELLOW = TextColor.fromHexString("#C9A200");
   private final GuiManager gui;

   public YourOrdersGui(GuiManager gui) {
      this.gui = gui;
   }

   public Inventory create(OrdersState state, List<OrderEntry> orders) {
      Component title = this.noItalic(Component.text(this.gui.yourOrdersGuiTitle(), NamedTextColor.DARK_GRAY));
      int size = this.gui.yourOrdersGuiSize();
      int maxNewSlot = this.gui.yourOrdersGuiMaxNewOrderSlot();
      Inventory inv = Bukkit.createInventory(new YourOrdersHolder(state), size, title);
      boolean hasOrders = orders != null && !orders.isEmpty();
      int newOrderSlot = hasOrders ? Math.min(maxNewSlot, orders.size()) : 0;
      inv.setItem(newOrderSlot, this.newOrderItem());
      if (hasOrders) {
         int slot = 0;

         for (OrderEntry entry : orders) {
            if (slot >= inv.getSize()) {
               break;
            }

            if (slot == newOrderSlot) {
               if (++slot >= inv.getSize()) {
                  break;
               }
            }

            inv.setItem(slot, this.orderItem(entry));
            slot++;
         }
      }

      return inv;
   }

   private ItemStack newOrderItem() {
      Material mat = this.gui.yourOrdersGuiNewOrderMat();
      ItemStack it = new ItemStack(mat);
      ItemMeta meta = it.getItemMeta();
      meta.displayName(this.noItalic(Component.text(this.gui.yourOrdersGuiNewOrderName(), GREEN)));
      List<Component> lore = this.gui
         .yourOrdersGuiNewOrderLore()
         .stream()
         .map(l -> (Component) Component.text(l, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
         .toList();
      meta.lore(lore);
      it.setItemMeta(meta);
      return it;
   }

   public ItemStack orderItem(OrderEntry entry) {
      ItemStack it = new ItemStack(entry.getMaterial() == null ? Material.STONE : entry.getMaterial());
      ItemMeta meta = it.getItemMeta();
      Map<Enchantment, Integer> enchants = entry.getEnchantments();
      if (!enchants.isEmpty()) {
         for (Entry<Enchantment, Integer> e : enchants.entrySet()) {
            meta.addEnchant(e.getKey(), e.getValue(), true);
         }

         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
      }

      meta.displayName(this.noItalic(Component.text(entry.getOwnerName() + "'s Order", GREEN)));
      long left = entry.getExpiresAtMillis() - System.currentTimeMillis();
      List<Component> lore = new ArrayList<>();
      lore.add(
         this.noItalic(
            ((TextComponent)Component.text(entry.getAmount(), GREEN).append(Component.text(" ", NamedTextColor.WHITE)))
               .append(Component.text(TextFormat.materialNiceName(entry.getMaterial()), NamedTextColor.WHITE))
         )
      );
      if (!enchants.isEmpty()) {
         for (Entry<Enchantment, Integer> e : enchants.entrySet()) {
            lore.add(this.noItalic(e.getKey().displayName(e.getValue()).color(TextColor.fromHexString("#3CFF3C"))));
         }
      }

      lore.add(this.noItalic(Component.text("$" + TextFormat.money(entry.getPricePerItem()), GREEN).append(Component.text(this.gui.yourOrdersGuiEachSuffix(), NamedTextColor.WHITE))));
      lore.add(this.noItalic(Component.text(" ")));
      lore.add(
         this.noItalic(
            ((TextComponent)((TextComponent)Component.text(entry.getDelivered(), DARK_YELLOW).append(Component.text("/", GREEN)))
                  .append(Component.text(entry.getAmount(), GREEN)))
               .append(Component.text(this.gui.yourOrdersGuiDeliveredSuffix(), NamedTextColor.WHITE))
         )
      );
      lore.add(
         this.noItalic(
            ((TextComponent)((TextComponent)Component.text("$" + TextFormat.money(entry.getPaid()), DARK_YELLOW).append(Component.text("/", GREEN)))
                  .append(Component.text("$" + TextFormat.money(entry.getPaidByCreator()), GREEN)))
               .append(Component.text(this.gui.yourOrdersGuiPaidSuffix(), NamedTextColor.WHITE))
         )
      );
      lore.add(this.noItalic(Component.text(" ")));
      if (entry.isCompleted()) {
         long removalLeft = Math.max(0L, entry.getExpiresAtMillis() - System.currentTimeMillis());
         lore.add(this.noItalic(Component.text(TextFormat.timeLeft(removalLeft) + " Until your order is removed", NamedTextColor.RED)));
      } else {
         lore.add(this.noItalic(Component.text(TextFormat.timeLeft(left) + " Until Order expieres", NamedTextColor.DARK_GRAY)));
      }

      meta.lore(lore);
      it.setItemMeta(meta);
      return it;
   }

   private Component noItalic(Component c) {
      return c.decoration(TextDecoration.ITALIC, false);
   }
         }
         
