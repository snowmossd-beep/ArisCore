package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.utils.OrderEntry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

public final class DeliverItemsGui {
   private final GuiManager gui;

   public DeliverItemsGui(GuiManager gui) {
      this.gui = gui;
   }

   public Inventory create(OrderEntry entry) {
      Component title = ((TextComponent)Component.text(this.gui.deliverItemsGuiTitlePart1(), NamedTextColor.DARK_GRAY)
            .append(Component.text(this.gui.deliverItemsGuiTitlePart2(), NamedTextColor.DARK_GRAY)))
         .decoration(TextDecoration.ITALIC, false);
      return Bukkit.createInventory(new DeliverItemsHolder(entry), 36, title);
   }
}
