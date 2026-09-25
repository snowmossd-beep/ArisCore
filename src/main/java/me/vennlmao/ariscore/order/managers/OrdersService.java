package me.vennlmao.ariscore.order.managers;

import me.vennlmao.ariscore.order.gui.CancelOrderGui;
import me.vennlmao.ariscore.order.gui.CollectItemsGui;
import me.vennlmao.ariscore.order.gui.ConfirmDeliveryGui;
import me.vennlmao.ariscore.order.gui.DeliverItemsGui;
import me.vennlmao.ariscore.order.gui.EditOrderGui;
import me.vennlmao.ariscore.order.gui.EnchantPickGui;
import me.vennlmao.ariscore.order.utils.EnchantPickState;
import me.vennlmao.ariscore.order.utils.MaterialFilters;
import me.vennlmao.ariscore.order.gui.NewOrderGui;
import me.vennlmao.ariscore.order.utils.NewOrderState;
import me.vennlmao.ariscore.order.utils.OrderEntry;
import me.vennlmao.ariscore.order.gui.OrdersGui;
import me.vennlmao.ariscore.order.utils.OrdersState;
import me.vennlmao.ariscore.order.gui.SelectItemGui;
import me.vennlmao.ariscore.order.utils.SelectItemState;
import me.vennlmao.ariscore.order.utils.ServerScheduler;
import me.vennlmao.ariscore.order.utils.TextFormat;
import me.vennlmao.ariscore.order.gui.YourOrdersGui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrdersService {
   public static final long COMPLETED_RETENTION_MILLIS = 7776000000L;
   private final Map<UUID, OrdersState> states = new ConcurrentHashMap<>();
   private final Map<UUID, NewOrderState> newOrderStates = new ConcurrentHashMap<>();
   private final Map<UUID, SelectItemState> selectItemStates = new ConcurrentHashMap<>();
   private final Map<UUID, EnchantPickState> enchantPickStates = new ConcurrentHashMap<>();
   private final Map<UUID, List<OrderEntry>> yourOrders = new ConcurrentHashMap<>();
   private final Object ordersLock = new Object();
   private final Map<UUID, ItemStack[]> pendingDeliveryItems = new ConcurrentHashMap<>();
   private DatabaseManager db;
   private EconomyBridge economy;
   private JavaPlugin plugin;
   private FileConfiguration messagesCfg;
   private final GuiManager guiManager;
   private final YourOrdersGui yourOrdersGui;
   private final OrdersGui gui;
   private final NewOrderGui newOrderGui;
   private final SelectItemGui selectItemGui;
   private final EditOrderGui editOrderGui;
   private final CancelOrderGui cancelOrderGui;
   private final CollectItemsGui collectItemsGui;
   private final DeliverItemsGui deliverItemsGui;
   private final ConfirmDeliveryGui confirmDeliveryGui;
   private final EnchantPickGui enchantPickGui;

   public OrdersService(GuiManager guiManager) {
      this.guiManager = guiManager;
      this.yourOrdersGui = new YourOrdersGui(guiManager);
      this.gui = new OrdersGui(guiManager, this.yourOrdersGui);
      this.newOrderGui = new NewOrderGui(guiManager);
      this.selectItemGui = new SelectItemGui(guiManager);
      this.editOrderGui = new EditOrderGui(guiManager, this.yourOrdersGui);
      this.cancelOrderGui = new CancelOrderGui(guiManager, this.yourOrdersGui);
      this.collectItemsGui = new CollectItemsGui(guiManager);
      this.deliverItemsGui = new DeliverItemsGui(guiManager);
      this.confirmDeliveryGui = new ConfirmDeliveryGui(guiManager, this.yourOrdersGui);
      this.enchantPickGui = new EnchantPickGui(guiManager);
   }

   public void setMessages(FileConfiguration cfg) {
      this.messagesCfg = cfg;
   }

   public GuiManager getGuiManager() {
      return this.guiManager;
   }

   public void setPlugin(JavaPlugin plugin) {
      this.plugin = plugin;
   }

   public void persistDelivery(OrderEntry entry) {
      if (this.db != null && entry != null) {
         long dbId = entry.getDbId();
         int delivered = entry.getDelivered();
         int collected = entry.getCollected();
         double paid = entry.getPaid();
         boolean completed = entry.isCompleted();
         long expiresAt = entry.getExpiresAtMillis();
         this.runDbAsync(() -> this.db.updateDelivery(dbId, delivered, collected, paid, completed, expiresAt));
      }
   }

   public void notifyOwner(OrderEntry entry, String delivererName, int amount) {
      Player owner = Bukkit.getPlayer(entry.getOwner());
      if (owner != null && owner.isOnline()) {
         String itemName = TextFormat.materialNiceName(entry.getMaterial());
         String raw = this.format(
            this.message("messages.owner_delivery_notification"),
            "{deliverer}",
            delivererName,
            "{amount}",
            String.valueOf(amount),
            "{item}",
            itemName
         );
         Component msg = this.mm(raw);
         if (this.plugin != null) {
            ServerScheduler.runPlayerTask(this.plugin, owner, () -> {
               if (owner.isOnline()) {
                  owner.sendActionBar(msg);
                  owner.sendMessage(msg);
               }
            });
         } else {
            owner.sendActionBar(msg);
            owner.sendMessage(msg);
         }
      }
   }

   public boolean payDeliverer(Player player, double amount) {
      return this.economy == null ? false : this.economy.deposit(player, amount);
   }

   public void setDatabase(DatabaseManager db) {
      this.db = db;
      synchronized (this.ordersLock) {
         for (OrderEntry entry : db.loadAllOrders()) {
            this.yourOrders.computeIfAbsent(entry.getOwner(), k -> new ArrayList<>()).add(entry);
         }
      }
   }

   private void runDbAsync(Runnable work) {
      if (work != null) {
         if (this.plugin != null) {
            ServerScheduler.runAsync(this.plugin, work);
         } else {
            work.run();
         }
      }
   }

   public void setEconomy(EconomyBridge economy) {
      this.economy = economy;
   }

   public OrdersState getOrCreate(Player player) {
      return this.states.computeIfAbsent(player.getUniqueId(), OrdersState::new);
   }

   public OrderEntry getYourOrderAtSlot(Player player, int slot) {
      List<OrderEntry> orders = this.getYourOrders(player);
      if (orders.isEmpty()) {
         return null;
      } else {
         int newOrderSlot = Math.min(this.guiManager.yourOrdersGuiMaxNewOrderSlot(), orders.size());
         if (slot == newOrderSlot) {
            return null;
         } else {
            int index = slot;
            if (slot > newOrderSlot) {
               index = slot - 1;
            }

            return index >= 0 && index < orders.size() ? orders.get(index) : null;
         }
      }
   }

   public double removeYourOrder(Player player, OrderEntry entry) {
      if (player != null && entry != null) {
         synchronized (this.ordersLock) {
            List<OrderEntry> list = this.yourOrders.get(player.getUniqueId());
            if (list == null) {
               return -1.0;
            } else {
               boolean removed = list.remove(entry);
               if (!removed) {
                  return -1.0;
               } else {
                  int undelivered = Math.max(0, entry.getAmount() - entry.getDelivered());
                  double refund = undelivered * entry.getPricePerItem();
                  if (this.economy != null && refund > 0.0) {
                     this.economy.deposit(player, refund);
                  }

                  if (this.getCollectableCount(entry) > 0) {
                     entry.setCompleted(true);
                     entry.setExpiresAtMillis(System.currentTimeMillis() + 7776000000L);
                     list.add(entry);
                     this.persistDelivery(entry);
                     return refund;
                  } else {
                     if (this.db != null) {
                        long dbId = entry.getDbId();
                        this.runDbAsync(() -> this.db.deleteOrder(dbId));
                     }

                     return refund;
                  }
               }
            }
         }
      } else {
         return -1.0;
      }
   }

   public Component cannotDeliverOwnOrderMessage() {
      return this.mm(this.message("messages.cannot_deliver_own_order"));
   }

   public Component noPermissionMessage() {
      return this.mm(this.message("messages.no_permission"));
   }

   public Component reloadedMessage() {
      return this.mm(this.message("messages.order_reloaded"));
   }

   public Component cancelRefundMessage(int undelivered, String itemName, double refund) {
      if (undelivered > 0 && !(refund <= 0.0)) {
         String raw = this.format(
            this.message("messages.order_cancelled_refund"),
            "{refund}",
            TextFormat.money(refund),
            "{undelivered}",
            String.valueOf(undelivered),
            "{item}",
            itemName
         );
         return this.mm(raw);
      } else {
         return this.mm(this.message("messages.order_cancelled_no_refund"));
      }
   }

   public Component orderCancelledDuringDeliveryMessage() {
      return this.mm(this.message("messages.order_cancelled_during_delivery"));
   }

   public Component noItemsToCollectMessage() {
      return this.mm(this.message("messages.no_items_to_collect"));
   }

   public Component enchantConflictMessage() {
      return this.mm(this.message("messages.enchants_conflict"));
   }

   public Component notEnoughMoneyMessage() {
      return this.mm(this.message("messages.not_enough_money"));
   }

   public Component deliveryProgressMessage(String dots) {
      String raw = this.format(this.message("messages.delivery_progress_actionbar"), "{dots}", dots);
      return this.mm(raw);
   }

   public Component deliverySuccessMessage(int amount, String itemName, double totalEarned) {
      String raw = this.format(
         this.message("messages.delivery_success"),
         "{amount}",
         String.valueOf(amount),
         "{item}",
         itemName,
         "{money}",
         TextFormat.money(totalEarned)
      );
      return this.mm(raw);
   }

   public boolean isOrderActive(OrderEntry entry) {
      if (entry == null) {
         return false;
      } else {
         synchronized (this.ordersLock) {
            List<OrderEntry> list = this.yourOrders.get(entry.getOwner());
            return list != null && list.contains(entry);
         }
      }
   }

   public boolean deleteOrderCompletely(OrderEntry entry) {
      if (entry == null) {
         return false;
      } else {
         synchronized (this.ordersLock) {
            List<OrderEntry> list = this.yourOrders.get(entry.getOwner());
            if (list == null) {
               return false;
            } else {
               boolean removed = list.remove(entry);
               if (removed && this.db != null) {
                  long dbId = entry.getDbId();
                  this.runDbAsync(() -> this.db.deleteOrder(dbId));
               }

               return removed;
            }
         }
      }
   }

   public boolean shouldAutoDeleteCollectedOrder(OrderEntry entry) {
      return entry == null ? false : entry.isCompleted() && this.getCollectableCount(entry) <= 0;
   }

   public NewOrderState getOrCreateNewOrder(Player player) {
      return this.newOrderStates.computeIfAbsent(player.getUniqueId(), k -> new NewOrderState());
   }

   public SelectItemState getOrCreateSelectItem(Player player) {
      return this.selectItemStates.computeIfAbsent(player.getUniqueId(), k -> new SelectItemState());
   }

   public void openOrders(Player player) {
      OrdersState state = this.states.computeIfAbsent(player.getUniqueId(), OrdersState::new);
      player.openInventory(this.gui.create(state, this.getVisibleOrders(state)));
   }

   public void openYourOrders(Player player) {
      OrdersState state = this.getOrCreate(player);
      player.openInventory(this.yourOrdersGui.create(state, this.getYourOrders(player)));
   }

   public void openNewOrder(Player player) {
      OrdersState ordersState = this.getOrCreate(player);
      NewOrderState state = this.getOrCreateNewOrder(player);
      player.openInventory(this.newOrderGui.create(ordersState, state));
   }

   public void openEditOrder(Player player, OrderEntry entry) {
      if (player != null && entry != null) {
         player.openInventory(this.editOrderGui.create(entry));
      }
   }

   public void openCancelOrder(Player player, OrderEntry entry) {
      if (player != null && entry != null) {
         player.openInventory(this.cancelOrderGui.create(entry));
      }
   }

   public void openCollectItems(Player player, OrderEntry entry, int page) {
      if (player != null && entry != null) {
         player.openInventory(this.collectItemsGui.create(entry, page));
      }
   }

   public void openDeliverItems(Player player, OrderEntry entry) {
      if (player != null && entry != null) {
         Inventory inv = this.deliverItemsGui.create(entry);
         ItemStack[] pending = this.pendingDeliveryItems.remove(player.getUniqueId());
         if (pending != null) {
            inv.setContents(pending);
         }

         player.openInventory(inv);
      }
   }

   public void savePendingDeliveryItems(Player player, ItemStack[] items) {
      if (player != null && items != null) {
         this.pendingDeliveryItems.put(player.getUniqueId(), Arrays.copyOf(items, items.length));
      }
   }

   public void clearPendingDeliveryItems(Player player) {
      if (player != null) {
         this.pendingDeliveryItems.remove(player.getUniqueId());
      }
   }

   public ItemStack[] getPendingDeliveryItems(Player player) {
      return player == null ? null : this.pendingDeliveryItems.get(player.getUniqueId());
   }

   public void openConfirmDelivery(Player player, OrderEntry entry, int deliverAmount) {
      if (player != null && entry != null) {
         player.openInventory(this.confirmDeliveryGui.create(entry, deliverAmount));
      }
   }

   public int countDeliverable(Inventory deliverInv, OrderEntry entry) {
      if (deliverInv != null && entry != null) {
         int count = 0;

         for (ItemStack item : deliverInv.getContents()) {
            if (item != null && !item.getType().isAir()) {
               if (this.matchesOrderItem(item, entry)) {
                  count += item.getAmount();
               } else if (this.isShulkerBox(item.getType())) {
                  count += this.countMatchingInShulker(item, entry);
               }
            }
         }

         return count;
      } else {
         return 0;
      }
   }

   private boolean isShulkerBox(Material mat) {
      return mat.name().endsWith("_SHULKER_BOX") || mat == Material.SHULKER_BOX;
   }

   public int countMatchingInShulker(ItemStack shulker, OrderEntry entry) {
      if (shulker.getItemMeta() instanceof BlockStateMeta bsm) {
         if (bsm.getBlockState() instanceof ShulkerBox box) {
            int var11 = 0;

            for (ItemStack inner : box.getInventory().getContents()) {
               if (this.matchesOrderItem(inner, entry)) {
                  var11 += inner.getAmount();
               }
            }

            return var11;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public boolean matchesOrderItem(ItemStack item, OrderEntry entry) {
      if (item != null && !item.getType().isAir() && entry != null) {
         if (item.getType() != entry.getMaterial()) {
            return false;
         } else {
            Map<Enchantment, Integer> required = entry.getEnchantments();
            if (required != null && !required.isEmpty()) {
               Map<Enchantment, Integer> present = item.getEnchantments();
               return present.equals(required);
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public int executeDelivery(Player player, Inventory deliverInv, OrderEntry entry) {
      if (player != null && deliverInv != null && entry != null) {
         int remaining = entry.getAmount() - entry.getDelivered();
         if (remaining <= 0) {
            this.returnItems(player, deliverInv);
            return 0;
         } else {
            int toDeliver = Math.min(this.countDeliverable(deliverInv, entry), remaining);
            if (toDeliver <= 0) {
               this.returnItems(player, deliverInv);
               return 0;
            } else {
               int leftToRemove = toDeliver;
               ItemStack[] contents = deliverInv.getContents();

               for (int i = 0; i < contents.length && leftToRemove > 0; i++) {
                  ItemStack item = contents[i];
                  if (this.matchesOrderItem(item, entry)) {
                     int take = Math.min(item.getAmount(), leftToRemove);
                     item.setAmount(item.getAmount() - take);
                     if (item.getAmount() <= 0) {
                        contents[i] = null;
                     }

                     leftToRemove -= take;
                  }
               }

               deliverInv.setContents(contents);
               this.returnItems(player, deliverInv);
               int applied = entry.applyDelivery(toDeliver, toDeliver * entry.getPricePerItem(), 7776000000L);
               if (applied > 0) {
                  this.persistDelivery(entry);
               }

               return applied;
            }
         }
      } else {
         return 0;
      }
   }

   private void returnItems(Player player, Inventory inv) {
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

   public void openSelectItem(Player player) {
      OrdersState ordersState = this.getOrCreate(player);
      SelectItemState state = this.getOrCreateSelectItem(player);
      player.openInventory(this.selectItemGui.create(ordersState, state, this.computeSelectMaterials(state)));
   }

   public void refreshSelectItem(Player player) {
      OrdersState ordersState = this.getOrCreate(player);
      SelectItemState state = this.getOrCreateSelectItem(player);
      player.openInventory(this.selectItemGui.create(ordersState, state, this.computeSelectMaterials(state)));
   }

   public void selectMaterial(Player player, Material material) {
      if (player != null && material != null) {
         NewOrderState state = this.getOrCreateNewOrder(player);
         state.setMaterial(material);
         state.clearEnchantments();
         if (this.isEnchantable(material)) {
            EnchantPickState enchState = new EnchantPickState();
            this.enchantPickStates.put(player.getUniqueId(), enchState);
            this.openEnchantPick(player, material, enchState);
         } else {
            this.openNewOrder(player);
         }
      }
   }

   public boolean isEnchantable(Material material) {
      if (material == null) {
         return false;
      } else {
         ItemStack test = new ItemStack(material);
         return !test.getType().isAir()
            && (
               test.getItemMeta() instanceof Damageable
                  || material == Material.ENCHANTED_BOOK
                  || material == Material.BOOK
                  || material == Material.FISHING_ROD
                  || material == Material.CARROT_ON_A_STICK
                  || material == Material.WARPED_FUNGUS_ON_A_STICK
            )
            && !this.computeEnchants(material).isEmpty();
      }
   }

   public void openEnchantPick(Player player, Material material, EnchantPickState state) {
      player.openInventory(this.enchantPickGui.create(material, state, this.computeEnchants(material)));
   }

   public void refreshEnchantPick(Player player, Material material, EnchantPickState state) {
      player.openInventory(this.enchantPickGui.create(material, state, this.computeEnchants(material)));
   }

   public void confirmEnchants(Player player, EnchantPickState enchState) {
      NewOrderState orderState = this.getOrCreateNewOrder(player);
      orderState.setEnchantments(new LinkedHashMap<>(enchState.getSelected()));
      this.openNewOrder(player);
   }

   public List<EnchantPickGui.EnchantEntry> computeEnchants(Material material) {
      List<EnchantPickGui.EnchantEntry> result = new ArrayList<>();
      ItemStack test = new ItemStack(material);

      for (Enchantment ench : Enchantment.values()) {
         if (ench.canEnchantItem(test) || material == Material.ENCHANTED_BOOK) {
            result.add(new EnchantPickGui.EnchantEntry(ench, 1));
         }
      }

      result.sort(Comparator.comparing(e -> e.enchantment().getKey().getKey()));
      return result;
   }

   public void refresh(Player player, OrdersState state) {
      player.openInventory(this.gui.create(state, this.getVisibleOrders(state)));
   }

   public boolean hasPreviousOrdersPage(OrdersState state) {
      return state != null && state.getPage() > 1;
   }

   public boolean hasNextOrdersPage(OrdersState state) {
      if (state == null) {
         return false;
      } else {
         int total = this.countVisibleOrders(state);
         return total <= 0 ? false : state.getPage() * (this.guiManager.orderGuiItemsMaxSlot() + 1) < total;
      }
   }

   public OrderEntry getOrderAtSlot(OrdersState state, int slot) {
      List<OrderEntry> visible = this.getVisibleOrders(state);
      return slot >= 0 && slot < visible.size() ? visible.get(slot) : null;
   }

   private List<OrderEntry> getVisibleOrders(OrdersState state) {
      if (state == null) {
         return List.of();
      } else {
         List<OrderEntry> all = new ArrayList<>();
         synchronized (this.ordersLock) {
            for (List<OrderEntry> list : this.yourOrders.values()) {
               if (list != null && !list.isEmpty()) {
                  all.addAll(list);
               }
            }
         }

         if (all.isEmpty()) {
            state.setPage(1);
            return List.of();
         } else {
            all.removeIf(
               e -> e == null
                  || e.getMaterial() == null
                  || e.isCompleted()
                  || !MaterialFilters.matchesFilter(e.getMaterial(), state.getFilterIndex())
                  || !MaterialFilters.matchesQuery(e.getMaterial(), state.getSearchQuery())
            );
            if (all.isEmpty()) {
               state.setPage(1);
               return List.of();
            } else {
               int sort = MaterialFilters.normalize(state.getSortIndex(), 4);

               Comparator<OrderEntry> comparator = switch (sort) {
                  case 0 -> Comparator.comparingDouble(OrderEntry::getPaidByCreator).reversed();
                  case 1 -> Comparator.comparingInt(OrderEntry::getDelivered).reversed();
                  case 2 -> Comparator.comparingLong(OrderEntry::getExpiresAtMillis).reversed();
                  case 3 -> Comparator.comparingDouble(OrderEntry::getPricePerItem).reversed();
                  default -> Comparator.comparingLong(OrderEntry::getExpiresAtMillis).reversed();
               };
               all.sort(comparator);
               int pageSize = this.guiManager.orderGuiItemsMaxSlot() + 1;
               int totalPages = Math.max(1, (int)Math.ceil((double)all.size() / pageSize));
               int page = Math.max(1, Math.min(state.getPage(), totalPages));
               state.setPage(page);
               int from = (page - 1) * pageSize;
               int to = Math.min(all.size(), from + pageSize);
               return all.subList(from, to);
            }
         }
      }
   }

   private int countVisibleOrders(OrdersState state) {
      if (state == null) {
         return 0;
      } else {
         int count = 0;
         synchronized (this.ordersLock) {
            for (List<OrderEntry> list : this.yourOrders.values()) {
               if (list != null && !list.isEmpty()) {
                  for (OrderEntry entry : list) {
                     if (entry != null
                        && entry.getMaterial() != null
                        && !entry.isCompleted()
                        && MaterialFilters.matchesFilter(entry.getMaterial(), state.getFilterIndex())
                        && MaterialFilters.matchesQuery(entry.getMaterial(), state.getSearchQuery())) {
                        count++;
                     }
                  }
               }
            }

            return count;
         }
      }
   }

   private String message(String path) {
      return this.messagesCfg == null ? "" : this.messagesCfg.getString(path, "");
   }

   private Component mm(String text) {
      return MiniMessage.miniMessage().deserialize(text == null ? "" : text);
   }

   private String format(String template, String... replacements) {
      String result = template == null ? "" : template;

      for (int i = 0; i + 1 < replacements.length; i += 2) {
         result = result.replace(replacements[i], replacements[i + 1]);
      }

      return result;
   }

   public void refreshYourOrders(Player player, OrdersState state) {
      player.openInventory(this.yourOrdersGui.create(state, this.getYourOrders(player)));
   }

   public void refreshNewOrder(Player player) {
      OrdersState ordersState = this.getOrCreate(player);
      NewOrderState state = this.getOrCreateNewOrder(player);
      player.openInventory(this.newOrderGui.create(ordersState, state));
   }

   public void resetSession(Player player) {
      if (player != null) {
         this.states.put(player.getUniqueId(), new OrdersState(player.getUniqueId()));
         this.newOrderStates.put(player.getUniqueId(), new NewOrderState());
         this.selectItemStates.put(player.getUniqueId(), new SelectItemState());
      }
   }

   public List<OrderEntry> getYourOrders(Player player) {
      if (player == null) {
         return List.of();
      } else {
         synchronized (this.ordersLock) {
            List<OrderEntry> list = this.yourOrders.get(player.getUniqueId());
            return list != null && !list.isEmpty() ? Collections.unmodifiableList(new ArrayList<>(list)) : List.of();
         }
      }
   }

   public int getCollectableCount(OrderEntry entry) {
      return entry == null ? 0 : Math.max(0, entry.getDelivered() - entry.getCollected());
   }

   public void dropLootPage(Player player, OrderEntry entry, int page) {
      if (player != null && entry != null) {
         int collectable = this.getCollectableCount(entry);
         if (collectable > 0) {
            Material mat = entry.getMaterial();
            int maxStack = mat.getMaxStackSize();
            int pageSize = this.collectItemsGui.getPageSize();
            int from = (page - 1) * pageSize * maxStack;
            int available = collectable - from;
            if (available > 0) {
               int toGive = Math.min(available, pageSize * maxStack);
               entry.setCollected(entry.getCollected() + toGive);
               this.persistDelivery(entry);
               if (this.shouldAutoDeleteCollectedOrder(entry)) {
                  this.deleteOrderCompletely(entry);
               }

               int left = toGive;

               while (left > 0) {
                  int stack = Math.min(left, maxStack);
                  ItemStack item = new ItemStack(mat, stack);
                  player.getWorld().dropItemNaturally(player.getLocation(), item);
                  left -= stack;
               }
            }
         }
      }
   }

   public boolean canAffordOrder(Player player) {
      if (player != null && this.economy != null) {
         NewOrderState state = this.getOrCreateNewOrder(player);
         double totalCost = Math.max(1, state.getAmount()) * state.getPricePerItem();
         return this.economy.has(player, totalCost);
      } else {
         return false;
      }
   }

   public boolean createOrder(Player player) {
      if (player == null) {
         return false;
      } else {
         NewOrderState state = this.getOrCreateNewOrder(player);
         int amount = Math.max(1, state.getAmount());
         double price = state.getPricePerItem();
         if (price < 1.0) {
            return false;
         } else {
            Material material = state.getMaterial() == null ? Material.STONE : state.getMaterial();
            long expires = System.currentTimeMillis() + 604800000L;
            double totalCost = amount * price;
            if (this.economy != null && this.economy.has(player, totalCost) && this.economy.withdraw(player, totalCost)) {
               OrderEntry entry = new OrderEntry(
                  player.getUniqueId(), player.getName(), material, amount, price, 0, 0, 0.0, totalCost, expires, -1L, state.getEnchantments()
               );
               if (this.db != null) {
                  long id = this.db.insertOrder(entry);
                  entry.setDbId(id);
               }

               synchronized (this.ordersLock) {
                  this.yourOrders.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(0, entry);
                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }

   private List<Material> computeSelectMaterials(SelectItemState state) {
      List<Material> all = new ArrayList<>();

      for (Material m : Material.values()) {
         if (m.isItem() && !m.isAir() && MaterialFilters.matchesFilter(m, state.getFilterIndex()) && MaterialFilters.matchesQuery(m, state.getSearchQuery())) {
            all.add(m);
         }
      }

      boolean za = MaterialFilters.normalize(state.getSortIndex(), 2) == 1;
      Comparator<Material> byName = Comparator.comparing(Material::name);
      all.sort(za ? byName.reversed() : byName);
      int selectPageSize = this.guiManager.selectItemGuiItemsMaxSlot() + 1;
      int page = Math.max(1, state.getPage());
      int from = (page - 1) * selectPageSize;
      if (from >= all.size()) {
         state.setPage(1);
         from = 0;
      }

      int to = Math.min(all.size(), from + selectPageSize);
      return all.subList(from, to);
   }
}
