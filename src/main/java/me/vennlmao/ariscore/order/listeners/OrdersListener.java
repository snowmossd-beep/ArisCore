package me.vennlmao.ariscore.order.listeners;

import me.vennlmao.ariscore.order.gui.CancelOrderHolder;
import me.vennlmao.ariscore.order.gui.CollectItemsHolder;
import me.vennlmao.ariscore.order.gui.ConfirmDeliveryHolder;
import me.vennlmao.ariscore.order.gui.DeliverItemsHolder;
import me.vennlmao.ariscore.order.gui.EditOrderHolder;
import me.vennlmao.ariscore.order.gui.EnchantPickGui;
import me.vennlmao.ariscore.order.gui.EnchantPickHolder;
import me.vennlmao.ariscore.order.utils.EnchantPickState;
import me.vennlmao.ariscore.order.utils.SoundUtil;
import me.vennlmao.ariscore.order.managers.GuiManager;
import me.vennlmao.ariscore.order.gui.NewOrderHolder;
import me.vennlmao.ariscore.order.utils.NewOrderState;
import me.vennlmao.ariscore.order.utils.OrderEntry;
import me.vennlmao.ariscore.order.gui.OrdersHolder;
import me.vennlmao.ariscore.order.managers.OrdersService;
import me.vennlmao.ariscore.order.utils.OrdersState;
import me.vennlmao.ariscore.order.gui.SelectItemHolder;
import me.vennlmao.ariscore.order.utils.SelectItemState;
import me.vennlmao.ariscore.order.utils.ServerScheduler;
import me.vennlmao.ariscore.order.utils.TextFormat;
import me.vennlmao.ariscore.order.gui.YourOrdersHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.Plugin;

public final class OrdersListener implements Listener {
   private final OrdersService ordersService;
   private final SignInputListener signInputListener;
   private final Plugin plugin;
   private final GuiManager guiManager;
   private final Set<UUID> deliveringPlayers = ConcurrentHashMap.newKeySet();

   public OrdersListener(OrdersService ordersService, SignInputListener signInputListener, Plugin plugin) {
      this.ordersService = ordersService;
      this.signInputListener = signInputListener;
      this.plugin = plugin;
      this.guiManager = ordersService.getGuiManager();
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      InventoryHolder holder = event.getInventory().getHolder();
      if (event.getWhoClicked() instanceof Player player) {
         if (!(holder instanceof DeliverItemsHolder)) {
            boolean isPluginGui = holder instanceof OrdersHolder
               || holder instanceof YourOrdersHolder
               || holder instanceof NewOrderHolder
               || holder instanceof SelectItemHolder
               || holder instanceof EditOrderHolder
               || holder instanceof CancelOrderHolder
               || holder instanceof CollectItemsHolder
               || holder instanceof ConfirmDeliveryHolder
               || holder instanceof EnchantPickHolder;
            if (isPluginGui && event.getRawSlot() >= event.getInventory().getSize()) {
               event.setCancelled(true);
               return;
            }
         }

         int slot = event.getRawSlot();
         if (slot >= 0 && slot < event.getInventory().getSize()) {
            if (holder instanceof OrdersHolder ordersHolder) {
               this.handleOrdersClick(event, player, slot, ordersHolder.getState());
            } else if (holder instanceof YourOrdersHolder yourOrdersHolder) {
               this.handleYourOrdersClick(event, player, slot, yourOrdersHolder.getState());
            } else if (holder instanceof NewOrderHolder newOrderHolder) {
               this.handleNewOrderClick(event, player, slot, newOrderHolder.getOrdersState());
            } else if (holder instanceof SelectItemHolder selectItemHolder) {
               this.handleSelectItemClick(event, player, slot, selectItemHolder);
            } else if (holder instanceof EditOrderHolder editOrderHolder) {
               this.handleEditOrderClick(event, player, slot, editOrderHolder.getEntry());
            } else if (holder instanceof CancelOrderHolder cancelOrderHolder) {
               this.handleCancelOrderClick(event, player, slot, cancelOrderHolder.getEntry());
            } else if (holder instanceof CollectItemsHolder collectItemsHolder) {
               this.handleCollectItemsClick(event, player, slot, collectItemsHolder);
            } else if (!(holder instanceof DeliverItemsHolder)) {
               if (holder instanceof ConfirmDeliveryHolder confirmDeliveryHolder) {
                  this.handleConfirmDeliveryClick(event, player, slot, confirmDeliveryHolder);
               } else {
                  if (holder instanceof EnchantPickHolder enchantPickHolder) {
                     this.handleEnchantPickClick(event, player, slot, enchantPickHolder);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onClose(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player player) {
         if (event.getInventory().getHolder() instanceof DeliverItemsHolder deliverItemsHolder) {
            if (this.deliveringPlayers.contains(player.getUniqueId())) {
               return;
            }

            Inventory inv = event.getInventory();
            OrderEntry entry = deliverItemsHolder.getEntry();
            int count = this.ordersService.countDeliverable(inv, entry);
            int remaining = entry.getAmount() - entry.getDelivered();
            count = Math.min(count, remaining);
            if (count <= 0) {
               this.returnAllItems(player, inv);
               return;
            }

            this.ordersService.savePendingDeliveryItems(player, inv.getContents());
            int finalCount = count;
            ServerScheduler.runPlayerTask(this.plugin, player, () -> this.ordersService.openConfirmDelivery(player, entry, finalCount));
         }
      }
   }

   private void returnAllItems(Player player, Inventory inv) {
      for (ItemStack item : inv.getContents()) {
         if (item != null && !item.getType().isAir()) {
            player.getInventory()
               .addItem(new ItemStack[]{item})
               .values()
               .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
         }
      }

      inv.clear();
   }

   private void handleOrdersClick(InventoryClickEvent event, Player player, int slot, OrdersState state) {
      event.setCancelled(true);
      int backSlot = this.guiManager.orderGuiSlot("back-button", 45);
      int nextSlot = this.guiManager.orderGuiSlot("next-button", 53);
      int refreshSlot = this.guiManager.orderGuiSlot("refresh-button", 49);
      int searchSlot = this.guiManager.orderGuiSlot("search-button", 50);
      int yourOrdersSlot = this.guiManager.orderGuiSlot("your-orders-button", 51);
      int sortSlot = this.guiManager.orderGuiSlot("sort-button", 47);
      int filterSlot = this.guiManager.orderGuiSlot("filter-button", 48);
      int itemsMaxSlot = this.guiManager.orderGuiItemsMaxSlot();
      if (slot == backSlot) {
         if (this.ordersService.hasPreviousOrdersPage(state)) {
            state.setPage(Math.max(1, state.getPage() - 1));
            SoundUtil.play(player, "page-turn");
            this.ordersService.refresh(player, state);
         }
      } else if (slot == nextSlot) {
         if (this.ordersService.hasNextOrdersPage(state)) {
            state.setPage(state.getPage() + 1);
            SoundUtil.play(player, "page-turn");
            this.ordersService.refresh(player, state);
         }
      } else if (slot == refreshSlot) {
         SoundUtil.play(player, "refresh");
         this.ordersService.refresh(player, state);
      } else if (slot == searchSlot) {
         SoundUtil.play(player, "button-click");
         this.signInputListener.open(player, input -> {
            state.setSearchQuery(input);
            this.ordersService.refresh(player, state);
         }, () -> this.ordersService.refresh(player, state));
      } else if (slot == yourOrdersSlot) {
         SoundUtil.play(player, "button-click");
         this.ordersService.openYourOrders(player);
      } else if (slot == sortSlot) {
         int delta = this.clickDelta(event.getClick());
         if (delta != 0) {
            state.setSortIndex(state.getSortIndex() + delta);
            SoundUtil.play(player, "button-click");
            this.ordersService.refresh(player, state);
         }
      } else if (slot == filterSlot) {
         int delta = this.clickDelta(event.getClick());
         if (delta != 0) {
            state.setFilterIndex(state.getFilterIndex() + delta);
            SoundUtil.play(player, "button-click");
            this.ordersService.refresh(player, state);
         }
      } else {
         if (slot >= 0 && slot <= itemsMaxSlot) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) {
               return;
            }

            OrderEntry entry = this.ordersService.getOrderAtSlot(state, slot);
            if (entry == null) {
               return;
            }

            if (entry.getOwner().equals(player.getUniqueId())) {
               player.sendActionBar(this.ordersService.cannotDeliverOwnOrderMessage());
               return;
            }

            SoundUtil.play(player, "button-click");
            this.ordersService.openDeliverItems(player, entry);
         }
      }
   }

   private void handleYourOrdersClick(InventoryClickEvent event, Player player, int slot, OrdersState state) {
      event.setCancelled(true);
      List<OrderEntry> orders = this.ordersService.getYourOrders(player);
      boolean hasOrders = orders != null && !orders.isEmpty();
      int newOrderSlot = hasOrders ? Math.min(this.guiManager.yourOrdersGuiMaxNewOrderSlot(), orders.size()) : 0;
      ItemStack clicked = event.getCurrentItem();
      boolean clickedNewOrderButton = clicked != null && clicked.getType() == Material.PAPER && slot == newOrderSlot;
      if (clickedNewOrderButton) {
         SoundUtil.play(player, "button-click");
         this.ordersService.openNewOrder(player);
      } else {
         OrderEntry entry = this.ordersService.getYourOrderAtSlot(player, slot);
         if (entry == null && hasOrders && slot == newOrderSlot && slot < orders.size()) {
            entry = orders.get(slot);
         }

         if (entry != null) {
            SoundUtil.play(player, "button-click");
            this.ordersService.openEditOrder(player, entry);
         }
      }
   }

   private void handleEditOrderClick(InventoryClickEvent event, Player player, int slot, OrderEntry entry) {
      event.setCancelled(true);
      int cancelSlot = this.guiManager.editOrderGuiCancelSlot();
      int collectSlot = this.guiManager.editOrderGuiCollectSlot();
      if (slot == cancelSlot) {
         SoundUtil.play(player, "button-click");
         if (entry.isCompleted()) {
            if (this.ordersService.getCollectableCount(entry) <= 0) {
               player.sendActionBar(this.ordersService.noItemsToCollectMessage());
               player.sendMessage(this.ordersService.noItemsToCollectMessage());
               SoundUtil.play(player, "deny");
               return;
            }

            this.ordersService.openCollectItems(player, entry, 1);
         } else {
            this.ordersService.openCancelOrder(player, entry);
         }
      } else {
         if (slot == collectSlot) {
            if (entry.isCompleted()) {
               return;
            }

            if (this.ordersService.getCollectableCount(entry) <= 0) {
               player.sendActionBar(this.ordersService.noItemsToCollectMessage());
               player.sendMessage(this.ordersService.noItemsToCollectMessage());
               SoundUtil.play(player, "deny");
               return;
            }

            SoundUtil.play(player, "button-click");
            this.ordersService.openCollectItems(player, entry, 1);
         }
      }
   }

   private void handleCancelOrderClick(InventoryClickEvent event, Player player, int slot, OrderEntry entry) {
      event.setCancelled(true);
      int backSlot = this.guiManager.cancelOrderGuiBackSlot();
      int confirmSlot = this.guiManager.cancelOrderGuiConfirmSlot();
      if (slot == backSlot) {
         SoundUtil.play(player, "button-click");
         this.ordersService.openEditOrder(player, entry);
      } else {
         if (slot == confirmSlot) {
            SoundUtil.play(player, "button-click");
            int undelivered = Math.max(0, entry.getAmount() - entry.getDelivered());
            String itemName = TextFormat.materialNiceName(entry.getMaterial());
            double refund = this.ordersService.removeYourOrder(player, entry);
            if (refund >= 0.0) {
               Component msg = this.ordersService.cancelRefundMessage(undelivered, itemName, refund);
               player.sendMessage(msg);
               player.sendActionBar(msg);
            }

            this.ordersService.openYourOrders(player);
         }
      }
   }

   private void handleCollectItemsClick(InventoryClickEvent event, Player player, int slot, CollectItemsHolder holder) {
      event.setCancelled(true);
      int pageSize = this.guiManager.collectItemsGuiPageSize();
      int backSlot = this.guiManager.collectItemsGuiBackSlot();
      int dropSlot = this.guiManager.collectItemsGuiDropSlot();
      int nextSlot = this.guiManager.collectItemsGuiNextSlot();
      if (slot >= 0 && slot < pageSize) {
         ItemStack clicked = event.getCurrentItem();
         if (clicked != null && !clicked.getType().isAir()) {
            int amount = clicked.getAmount();
            player.getInventory()
               .addItem(new ItemStack[]{clicked.clone()})
               .values()
               .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            event.getInventory().setItem(slot, null);
            OrderEntry entry = holder.getEntry();
            entry.setCollected(entry.getCollected() + amount);
            this.ordersService.persistDelivery(entry);
            if (this.ordersService.shouldAutoDeleteCollectedOrder(entry)) {
               this.ordersService.deleteOrderCompletely(entry);
               SoundUtil.play(player, "collect-item");
               this.ordersService.openYourOrders(player);
            } else {
               SoundUtil.play(player, "collect-item");
            }
         }
      } else if (slot == backSlot) {
         if (holder.getPage() > 1) {
            SoundUtil.play(player, "page-turn");
            this.ordersService.openCollectItems(player, holder.getEntry(), holder.getPage() - 1);
         }
      } else if (slot == nextSlot) {
         OrderEntry e = holder.getEntry();
         int maxStack = e.getMaterial().getMaxStackSize();
         int from = (holder.getPage() - 1) * pageSize * maxStack;
         if (this.ordersService.getCollectableCount(e) - from > pageSize * maxStack) {
            SoundUtil.play(player, "page-turn");
            this.ordersService.openCollectItems(player, e, holder.getPage() + 1);
         }
      } else {
         if (slot == dropSlot) {
            SoundUtil.play(player, "drop-loot");
            this.ordersService.dropLootPage(player, holder.getEntry(), holder.getPage());
            if (this.ordersService.shouldAutoDeleteCollectedOrder(holder.getEntry())) {
               this.ordersService.openYourOrders(player);
               return;
            }

            this.ordersService.openCollectItems(player, holder.getEntry(), holder.getPage());
         }
      }
   }

   private void handleConfirmDeliveryClick(InventoryClickEvent event, Player player, int slot, ConfirmDeliveryHolder holder) {
      event.setCancelled(true);
      OrderEntry entry = holder.getEntry();
      int deliverAmount = holder.getDeliverAmount();
      int cancelSlot = this.guiManager.confirmDeliveryGuiCancelSlot();
      int confirmSlot = this.guiManager.confirmDeliveryGuiConfirmSlot();
      if (slot == cancelSlot) {
         SoundUtil.play(player, "button-click");
         this.ordersService.openDeliverItems(player, entry);
      } else {
         if (slot == confirmSlot) {
            SoundUtil.play(player, "button-click");
            this.returnNonMatchingPendingItems(player, entry, deliverAmount);
            this.ordersService.clearPendingDeliveryItems(player);
            this.startDeliveryAnimation(player, entry, deliverAmount);
         }
      }
   }

   private void returnNonMatchingPendingItems(Player player, OrderEntry entry, int deliverAmount) {
      ItemStack[] pending = this.ordersService.getPendingDeliveryItems(player);
      if (pending != null) {
         int leftToConsume = deliverAmount;

         for (ItemStack item : pending) {
            if (item != null && !item.getType().isAir()) {
               if (this.ordersService.matchesOrderItem(item, entry) && leftToConsume > 0) {
                  int consume = Math.min(item.getAmount(), leftToConsume);
                  leftToConsume -= consume;
                  int leftover = item.getAmount() - consume;
                  if (leftover > 0) {
                     ItemStack ret = item.clone();
                     ret.setAmount(leftover);
                     this.returnToPlayer(player, ret);
                  }
               } else if (this.isShulkerBox(item.getType()) && leftToConsume > 0) {
                  int before = this.ordersService.countMatchingInShulker(item, entry);
                  ItemStack processed = this.consumeFromShulker(item, entry, leftToConsume);
                  int after = this.ordersService.countMatchingInShulker(processed, entry);
                  leftToConsume -= before - after;
                  this.returnToPlayer(player, processed);
               } else {
                  this.returnToPlayer(player, item.clone());
               }
            }
         }
      }
   }

   private ItemStack consumeFromShulker(ItemStack shulker, OrderEntry entry, int toConsume) {
      ItemStack result = shulker.clone();
      if (!(result.getItemMeta() instanceof BlockStateMeta bsm)) {
         return result;
      } else if (!(bsm.getBlockState() instanceof ShulkerBox box)) {
         return result;
      } else {
         Inventory inv = box.getInventory();
         ItemStack[] contents = inv.getContents();
         int left = toConsume;

         for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack inner = contents[i];
            if (this.ordersService.matchesOrderItem(inner, entry)) {
               int take = Math.min(inner.getAmount(), left);
               inner.setAmount(inner.getAmount() - take);
               if (inner.getAmount() <= 0) {
                  contents[i] = null;
               }

               left -= take;
            }
         }

         inv.setContents(contents);
         bsm.setBlockState(box);
         result.setItemMeta(bsm);
         return result;
      }
   }

   private boolean isShulkerBox(Material mat) {
      return mat.name().endsWith("_SHULKER_BOX") || mat == Material.SHULKER_BOX;
   }

   private void returnToPlayer(Player player, ItemStack item) {
      player.getInventory().addItem(new ItemStack[]{item}).values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
   }

   private void startDeliveryAnimation(Player player, OrderEntry entry, int deliverAmount) {
      this.deliveringPlayers.add(player.getUniqueId());
      player.closeInventory();
      String itemName = TextFormat.materialNiceName(entry.getMaterial());
      double totalEarned = deliverAmount * entry.getPricePerItem();
      this.runDeliveryAnimationStep(player, entry, deliverAmount, itemName, totalEarned, 0);
   }

   private void runDeliveryAnimationStep(Player player, OrderEntry entry, int deliverAmount, String itemName, double totalEarned, int tick) {
      if (!player.isOnline()) {
         this.deliveringPlayers.remove(player.getUniqueId());
      } else {
         String[] dots = new String[]{".", "..", "..."};
         if (tick < 6) {
            String dotPhase = dots[tick / 2];
            player.sendActionBar(this.ordersService.deliveryProgressMessage(dotPhase));
            if (tick < 4) {
               SoundUtil.play(player, "delivery-progress", 1.0F + tick * 0.05F);
            }

            ServerScheduler.runPlayerTaskLater(
               this.plugin, player, () -> this.runDeliveryAnimationStep(player, entry, deliverAmount, itemName, totalEarned, tick + 1), 10L
            );
         } else {
            this.deliveringPlayers.remove(player.getUniqueId());
            if (!this.ordersService.isOrderActive(entry)) {
               ItemStack returned = new ItemStack(entry.getMaterial(), deliverAmount);
               player.getInventory()
                  .addItem(new ItemStack[]{returned})
                  .values()
                  .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
               player.sendActionBar(this.ordersService.orderCancelledDuringDeliveryMessage());
               player.sendMessage(this.ordersService.orderCancelledDuringDeliveryMessage());
            } else {
               int applied = entry.applyDelivery(deliverAmount, totalEarned, 7776000000L);
               if (applied <= 0) {
                  ItemStack returned = new ItemStack(entry.getMaterial(), deliverAmount);
                  player.getInventory()
                     .addItem(new ItemStack[]{returned})
                     .values()
                     .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
                  player.sendActionBar(this.ordersService.orderCancelledDuringDeliveryMessage());
                  player.sendMessage(this.ordersService.orderCancelledDuringDeliveryMessage());
               } else {
                  double earned = applied * entry.getPricePerItem();
                  if (applied < deliverAmount) {
                     int leftover = deliverAmount - applied;
                     ItemStack returned = new ItemStack(entry.getMaterial(), leftover);
                     player.getInventory()
                        .addItem(new ItemStack[]{returned})
                        .values()
                        .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
                  }

                  this.ordersService.persistDelivery(entry);
                  this.ordersService.notifyOwner(entry, player.getName(), applied);
                  this.ordersService.payDeliverer(player, earned);
                  player.sendActionBar(this.ordersService.deliverySuccessMessage(applied, itemName, earned));
                  player.sendMessage(this.ordersService.deliverySuccessMessage(applied, itemName, earned));
                  SoundUtil.play(player, "delivery-success");
               }
            }
         }
      }
   }

   private void handleNewOrderClick(InventoryClickEvent event, Player player, int slot, OrdersState ordersState) {
      event.setCancelled(true);
      NewOrderState state = this.ordersService.getOrCreateNewOrder(player);
      int cancelSlot = this.guiManager.newOrderGuiCancelSlot();
      int itemSlot = this.guiManager.newOrderGuiItemSlot();
      int amountSlot = this.guiManager.newOrderGuiAmountSlot();
      int priceSlot = this.guiManager.newOrderGuiPriceSlot();
      int confirmSlot = this.guiManager.newOrderGuiConfirmSlot();
      if (slot == cancelSlot) {
         SoundUtil.play(player, "button-click");
         this.ordersService.openYourOrders(player);
      } else if (slot == itemSlot) {
         SoundUtil.play(player, "button-click");
         this.ordersService.openSelectItem(player);
      } else if (slot == amountSlot) {
         SoundUtil.play(player, "button-click");
         this.signInputListener.open(player, input -> {
            Integer amount = this.parseIntStrict(input);
            if (amount != null && amount > 0) {
               state.setAmount(amount);
            }

            this.ordersService.refreshNewOrder(player);
         }, () -> this.ordersService.refreshNewOrder(player));
      } else if (slot == priceSlot) {
         SoundUtil.play(player, "button-click");
         this.signInputListener.open(player, input -> {
            Double price = TextFormat.parseCompactNumber(input);
            if (price != null && price >= 0.0) {
               state.setPricePerItem(price);
            }

            this.ordersService.refreshNewOrder(player);
         }, () -> this.ordersService.refreshNewOrder(player));
      } else {
         if (slot == confirmSlot) {
            SoundUtil.play(player, "button-click");
            boolean canAfford = this.ordersService.canAffordOrder(player);
            boolean created = this.ordersService.createOrder(player);
            if (created) {
               this.ordersService.openYourOrders(player);
            } else {
               if (!canAfford) {
                  Component msg = this.ordersService.notEnoughMoneyMessage();
                  player.sendActionBar(msg);
                  player.sendMessage(msg);
               }

               this.ordersService.refreshNewOrder(player);
            }
         }
      }
   }

   private void handleSelectItemClick(InventoryClickEvent event, Player player, int slot, SelectItemHolder holder) {
      event.setCancelled(true);
      SelectItemState state = holder.getSelectItemState();
      int itemsMaxSlot = this.guiManager.selectItemGuiItemsMaxSlot();
      int backSlot = this.guiManager.selectItemGuiSlot("back-button", 45);
      int nextSlot = this.guiManager.selectItemGuiSlot("next-button", 53);
      int sortSlot = this.guiManager.selectItemGuiSlot("sort-button", 48);
      int filterSlot = this.guiManager.selectItemGuiSlot("filter-button", 49);
      int searchSlot = this.guiManager.selectItemGuiSlot("search-button", 50);
      if (slot >= 0 && slot <= itemsMaxSlot) {
         if (event.getCurrentItem() != null) {
            Material mat = event.getCurrentItem().getType();
            if (!mat.isAir()) {
               SoundUtil.play(player, "button-click");
               this.ordersService.selectMaterial(player, mat);
            }
         }
      } else if (slot == backSlot) {
         state.setPage(Math.max(1, state.getPage() - 1));
         SoundUtil.play(player, "page-turn");
         this.ordersService.refreshSelectItem(player);
      } else if (slot == nextSlot) {
         state.setPage(state.getPage() + 1);
         SoundUtil.play(player, "page-turn");
         this.ordersService.refreshSelectItem(player);
      } else if (slot == sortSlot) {
         state.setSortIndex(state.getSortIndex() + this.clickDelta(event.getClick()));
         SoundUtil.play(player, "button-click");
         this.ordersService.refreshSelectItem(player);
      } else if (slot == filterSlot) {
         state.setFilterIndex(state.getFilterIndex() + this.clickDelta(event.getClick()));
         SoundUtil.play(player, "button-click");
         this.ordersService.refreshSelectItem(player);
      } else {
         if (slot == searchSlot) {
            SoundUtil.play(player, "button-click");
            this.signInputListener.open(player, input -> {
               state.setSearchQuery(input);
               state.setPage(1);
               this.ordersService.refreshSelectItem(player);
            }, () -> this.ordersService.refreshSelectItem(player));
         }
      }
   }

   private void handleEnchantPickClick(InventoryClickEvent event, Player player, int slot, EnchantPickHolder holder) {
      event.setCancelled(true);
      EnchantPickState state = holder.getState();
      Material material = holder.getMaterial();
      List<EnchantPickGui.EnchantEntry> enchants = this.ordersService.computeEnchants(material);
      if (slot == 45 && state.getPage() > 0) {
         state.setPage(state.getPage() - 1);
         SoundUtil.play(player, "page-turn");
         this.ordersService.refreshEnchantPick(player, material, state);
      } else if (slot == 53 && state.getPage() < this.enchantPickGuiTotalPages(enchants.size())) {
         state.setPage(state.getPage() + 1);
         SoundUtil.play(player, "page-turn");
         this.ordersService.refreshEnchantPick(player, material, state);
      } else if (slot == 46) {
         SoundUtil.play(player, "button-click");
         this.ordersService.openSelectItem(player);
      } else if (slot == 52) {
         SoundUtil.play(player, "button-click");
         this.ordersService.confirmEnchants(player, state);
      } else {
         OrdersListener.EnchantmentClicked clicked = this.clickedEnchantment(enchants, state.getPage(), slot);
         if (clicked != null) {
            if (!state.isSelected(clicked.enchantment(), clicked.level()) && !state.canSelect(clicked.enchantment())) {
               player.sendActionBar(this.ordersService.enchantConflictMessage());
               SoundUtil.play(player, "deny");
            } else {
               state.toggle(clicked.enchantment(), clicked.level());
               SoundUtil.play(player, "button-click");
               this.ordersService.refreshEnchantPick(player, material, state);
            }
         }
      }
   }

   private int enchantPickGuiTotalPages(int enchantCount) {
      return Math.max(0, (enchantCount - 2) / 5);
   }

   private OrdersListener.EnchantmentClicked clickedEnchantment(List<EnchantPickGui.EnchantEntry> enchants, int page, int slot) {
      int count;
      int level;
      if (slot >= 3 && slot <= 7) {
         count = 2;
         level = slot - 2;
      } else if (slot >= 12 && slot <= 16) {
         count = 3;
         level = slot - 11;
      } else if (slot >= 21 && slot <= 25) {
         count = 4;
         level = slot - 20;
      } else if (slot >= 30 && slot <= 34) {
         count = 5;
         level = slot - 29;
      } else if (slot >= 39 && slot <= 43) {
         count = 6;
         level = slot - 38;
      } else if (slot != 18 && slot != 27 && slot != 36) {
         if (slot != 19 && slot != 28 && slot != 37) {
            return null;
         }

         count = 1;
         level = (slot - 19) / 9 + 1;
      } else {
         count = 0;
         level = (slot - 18) / 9 + 1;
      }

      int index = count > 1 ? page * 5 + count : count;
      if (index >= 0 && index < enchants.size()) {
         Enchantment enchantment = enchants.get(index).enchantment();
         return enchantment != null && level <= enchantment.getMaxLevel() ? new OrdersListener.EnchantmentClicked(enchantment, level) : null;
      } else {
         return null;
      }
   }

   private Integer parseIntStrict(String input) {
      Double parsed = TextFormat.parseCompactNumber(input);
      if (parsed == null || parsed < 0.0 || parsed > 2.147483647E9) {
         return null;
      } else {
         return Math.floor(parsed) != parsed ? null : parsed.intValue();
      }
   }

   private int clickDelta(ClickType clickType) {
      if (clickType == ClickType.LEFT) {
         return 1;
      } else {
         return clickType == ClickType.RIGHT ? 1 : 0;
      }
   }

   private record EnchantmentClicked(Enchantment enchantment, int level) {
   }
}
