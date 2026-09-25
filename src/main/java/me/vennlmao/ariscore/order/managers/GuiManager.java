package me.vennlmao.ariscore.order.managers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuiManager {
   private final JavaPlugin plugin;
   private FileConfiguration orderCfg;
   private FileConfiguration editOrderCfg;
   private FileConfiguration myOrderCfg;
   private FileConfiguration labelsCfg;

   public GuiManager(JavaPlugin plugin) {
      this.plugin = plugin;
      File guiFolder = new File(plugin.getDataFolder(), "order/gui");
      if (!guiFolder.exists()) {
         guiFolder.mkdirs();
      }

      this.saveDefault("order/gui/order.yml");
      this.saveDefault("order/gui/editorder.yml");
      this.saveDefault("order/gui/myorder.yml");
      this.saveDefault("order/gui/labels.yml");
      this.reload();
   }

   private void saveDefault(String resourcePath) {
      File target = new File(this.plugin.getDataFolder(), resourcePath);
      if (!target.exists()) {
         this.plugin.saveResource(resourcePath, false);
      }
   }

   public void reload() {
      this.orderCfg = this.load("order/gui/order.yml");
      this.editOrderCfg = this.load("order/gui/editorder.yml");
      this.myOrderCfg = this.load("order/gui/myorder.yml");
      this.labelsCfg = this.load("order/gui/labels.yml");
   }

   private FileConfiguration load(String resourcePath) {
      File file = new File(this.plugin.getDataFolder(), resourcePath);
      YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
      InputStream defStream = this.plugin.getResource(resourcePath);
      if (defStream != null) {
         YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
         cfg.setDefaults(defaults);
         cfg.options().copyDefaults(true);

         try {
            cfg.save(file);
         } catch (IOException var7) {
         }
      }

      return cfg;
   }

   public String orderGuiTitle() {
      return this.orderCfg.getString("order-gui.title", "");
   }

   public int orderGuiSize() {
      return this.orderCfg.getInt("order-gui.size", 54);
   }

   public int orderGuiItemsMaxSlot() {
      return this.orderCfg.getInt("order-gui.items-max-slot", 44);
   }

   public int orderGuiSlot(String button, int def) {
      return this.orderCfg.getInt("order-gui." + button + ".slot", def);
   }

   public Material orderGuiMat(String button, Material def) {
      return this.mat(this.orderCfg, "order-gui." + button + ".material", def);
   }

   public String orderGuiName(String button) {
      return this.orderCfg.getString("order-gui." + button + ".name", "");
   }

   public List<String> orderGuiLore(String button) {
      return this.orderCfg.getStringList("order-gui." + button + ".lore");
   }

   public List<String> orderGuiSorts() {
      return this.orderCfg.getStringList("order-gui.sorts");
   }

   public List<String> orderGuiFilters() {
      return this.orderCfg.getStringList("order-gui.filters");
   }

   public String selectItemGuiTitle() {
      return this.orderCfg.getString("select-item-gui.title", "");
   }

   public int selectItemGuiSize() {
      return this.orderCfg.getInt("select-item-gui.size", 54);
   }

   public int selectItemGuiItemsMaxSlot() {
      return this.orderCfg.getInt("select-item-gui.items-max-slot", 43);
   }

   public int selectItemGuiFillerSlot() {
      return this.orderCfg.getInt("select-item-gui.filler-slot", 44);
   }

   public Material selectItemGuiFillerMat() {
      return this.mat(this.orderCfg, "select-item-gui.filler-material", Material.BLACK_STAINED_GLASS_PANE);
   }

   public int selectItemGuiSlot(String button, int def) {
      return this.orderCfg.getInt("select-item-gui." + button + ".slot", def);
   }

   public Material selectItemGuiMat(String button, Material def) {
      return this.mat(this.orderCfg, "select-item-gui." + button + ".material", def);
   }

   public String selectItemGuiName(String button) {
      return this.orderCfg.getString("select-item-gui." + button + ".name", "");
   }

   public List<String> selectItemGuiLore(String button) {
      return this.orderCfg.getStringList("select-item-gui." + button + ".lore");
   }

   public List<String> selectItemGuiSorts() {
      return this.orderCfg.getStringList("select-item-gui.sorts");
   }

   public List<String> selectItemGuiFilters() {
      return this.orderCfg.getStringList("select-item-gui.filters");
   }

   public String editOrderGuiTitle() {
      return this.editOrderCfg.getString("edit-order-gui.title", "");
   }

   public int editOrderGuiSize() {
      return this.editOrderCfg.getInt("edit-order-gui.size", 27);
   }

   public int editOrderGuiOrderItemSlot() {
      return this.editOrderCfg.getInt("edit-order-gui.order-item-slot", 10);
   }

   public int editOrderGuiCancelSlot() {
      return this.editOrderCfg.getInt("edit-order-gui.cancel-button-slot", 13);
   }

   public int editOrderGuiCollectSlot() {
      return this.editOrderCfg.getInt("edit-order-gui.collect-button-slot", 15);
   }

   public List<Integer> editOrderGuiFillerSlots() {
      return this.editOrderCfg.getIntegerList("edit-order-gui.filler-slots");
   }

   public Material editOrderGuiFillerMat() {
      return this.mat(this.editOrderCfg, "edit-order-gui.filler-material", Material.BLACK_STAINED_GLASS_PANE);
   }

   public Material editOrderGuiCancelMat() {
      return this.mat(this.editOrderCfg, "edit-order-gui.cancel-button.material", Material.RED_CONCRETE);
   }

   public String editOrderGuiCancelName() {
      return this.editOrderCfg.getString("edit-order-gui.cancel-button.name", "");
   }

   public List<String> editOrderGuiCancelLore() {
      return this.editOrderCfg.getStringList("edit-order-gui.cancel-button.lore");
   }

   public Material editOrderGuiCollectMat() {
      return this.mat(this.editOrderCfg, "edit-order-gui.collect-button.material", Material.CHEST);
   }

   public String editOrderGuiCollectName() {
      return this.editOrderCfg.getString("edit-order-gui.collect-button.name", "");
   }

   public List<String> editOrderGuiCollectLore() {
      return this.editOrderCfg.getStringList("edit-order-gui.collect-button.lore");
   }

   public String cancelOrderGuiTitle() {
      return this.editOrderCfg.getString("cancel-order-gui.title", "");
   }

   public int cancelOrderGuiSize() {
      return this.editOrderCfg.getInt("cancel-order-gui.size", 27);
   }

   public int cancelOrderGuiBackSlot() {
      return this.editOrderCfg.getInt("cancel-order-gui.back-button-slot", 10);
   }

   public int cancelOrderGuiOrderItemSlot() {
      return this.editOrderCfg.getInt("cancel-order-gui.order-item-slot", 13);
   }

   public int cancelOrderGuiConfirmSlot() {
      return this.editOrderCfg.getInt("cancel-order-gui.confirm-button-slot", 16);
   }

   public Material cancelOrderGuiBackMat() {
      return this.mat(this.editOrderCfg, "cancel-order-gui.back-button.material", Material.RED_STAINED_GLASS_PANE);
   }

   public String cancelOrderGuiBackName() {
      return this.editOrderCfg.getString("cancel-order-gui.back-button.name", "");
   }

   public List<String> cancelOrderGuiBackLore() {
      return this.editOrderCfg.getStringList("cancel-order-gui.back-button.lore");
   }

   public Material cancelOrderGuiConfirmMat() {
      return this.mat(this.editOrderCfg, "cancel-order-gui.confirm-button.material", Material.LIME_STAINED_GLASS_PANE);
   }

   public String cancelOrderGuiConfirmName() {
      return this.editOrderCfg.getString("cancel-order-gui.confirm-button.name", "");
   }

   public List<String> cancelOrderGuiConfirmLore() {
      return this.editOrderCfg.getStringList("cancel-order-gui.confirm-button.lore");
   }

   public String confirmDeliveryGuiTitle() {
      return this.editOrderCfg.getString("confirm-delivery-gui.title", "");
   }

   public int confirmDeliveryGuiSize() {
      return this.editOrderCfg.getInt("confirm-delivery-gui.size", 27);
   }

   public int confirmDeliveryGuiCancelSlot() {
      return this.editOrderCfg.getInt("confirm-delivery-gui.cancel-button-slot", 11);
   }

   public int confirmDeliveryGuiPreviewSlot() {
      return this.editOrderCfg.getInt("confirm-delivery-gui.preview-item-slot", 13);
   }

   public int confirmDeliveryGuiConfirmSlot() {
      return this.editOrderCfg.getInt("confirm-delivery-gui.confirm-button-slot", 15);
   }

   public Material confirmDeliveryGuiCancelMat() {
      return this.mat(this.editOrderCfg, "confirm-delivery-gui.cancel-button.material", Material.RED_STAINED_GLASS_PANE);
   }

   public String confirmDeliveryGuiCancelName() {
      return this.editOrderCfg.getString("confirm-delivery-gui.cancel-button.name", "");
   }

   public List<String> confirmDeliveryGuiCancelLore() {
      return this.editOrderCfg.getStringList("confirm-delivery-gui.cancel-button.lore");
   }

   public Material confirmDeliveryGuiConfirmMat() {
      return this.mat(this.editOrderCfg, "confirm-delivery-gui.confirm-button.material", Material.LIME_STAINED_GLASS_PANE);
   }

   public String confirmDeliveryGuiConfirmName() {
      return this.editOrderCfg.getString("confirm-delivery-gui.confirm-button.name", "");
   }

   public List<String> confirmDeliveryGuiConfirmLore() {
      return this.editOrderCfg.getStringList("confirm-delivery-gui.confirm-button.lore");
   }

   public String collectItemsGuiTitle() {
      return this.editOrderCfg.getString("collect-items-gui.title", "");
   }

   public int collectItemsGuiSize() {
      return this.editOrderCfg.getInt("collect-items-gui.size", 54);
   }

   public int collectItemsGuiPageSize() {
      return this.editOrderCfg.getInt("collect-items-gui.page-size", 45);
   }

   public Material collectItemsGuiFillerMat() {
      return this.mat(this.editOrderCfg, "collect-items-gui.filler-material", Material.GRAY_STAINED_GLASS_PANE);
   }

   public int collectItemsGuiBackSlot() {
      return this.editOrderCfg.getInt("collect-items-gui.back-button.slot", 45);
   }

   public Material collectItemsGuiBackMat() {
      return this.mat(this.editOrderCfg, "collect-items-gui.back-button.material", Material.ARROW);
   }

   public String collectItemsGuiBackName() {
      return this.editOrderCfg.getString("collect-items-gui.back-button.name", "");
   }

   public List<String> collectItemsGuiBackLore() {
      return this.editOrderCfg.getStringList("collect-items-gui.back-button.lore");
   }

   public int collectItemsGuiDropSlot() {
      return this.editOrderCfg.getInt("collect-items-gui.drop-loot-button.slot", 49);
   }

   public Material collectItemsGuiDropMat() {
      return this.mat(this.editOrderCfg, "collect-items-gui.drop-loot-button.material", Material.DISPENSER);
   }

   public String collectItemsGuiDropName() {
      return this.editOrderCfg.getString("collect-items-gui.drop-loot-button.name", "");
   }

   public List<String> collectItemsGuiDropLore() {
      return this.editOrderCfg.getStringList("collect-items-gui.drop-loot-button.lore");
   }

   public int collectItemsGuiNextSlot() {
      return this.editOrderCfg.getInt("collect-items-gui.next-button.slot", 53);
   }

   public Material collectItemsGuiNextMat() {
      return this.mat(this.editOrderCfg, "collect-items-gui.next-button.material", Material.ARROW);
   }

   public String collectItemsGuiNextName() {
      return this.editOrderCfg.getString("collect-items-gui.next-button.name", "");
   }

   public List<String> collectItemsGuiNextLore() {
      return this.editOrderCfg.getStringList("collect-items-gui.next-button.lore");
   }

   public String yourOrdersGuiTitle() {
      return this.myOrderCfg.getString("your-orders-gui.title", "");
   }

   public int yourOrdersGuiSize() {
      return this.myOrderCfg.getInt("your-orders-gui.size", 27);
   }

   public int yourOrdersGuiMaxNewOrderSlot() {
      return this.myOrderCfg.getInt("your-orders-gui.max-new-order-slot", 26);
   }

   public Material yourOrdersGuiNewOrderMat() {
      return this.mat(this.myOrderCfg, "your-orders-gui.new-order-button.material", Material.PAPER);
   }

   public String yourOrdersGuiNewOrderName() {
      return this.myOrderCfg.getString("your-orders-gui.new-order-button.name", "");
   }

   public List<String> yourOrdersGuiNewOrderLore() {
      return this.myOrderCfg.getStringList("your-orders-gui.new-order-button.lore");
   }

   public String newOrderGuiTitle() {
      return this.myOrderCfg.getString("new-order-gui.title", "");
   }

   public int newOrderGuiSize() {
      return this.myOrderCfg.getInt("new-order-gui.size", 27);
   }

   public int newOrderGuiCancelSlot() {
      return this.myOrderCfg.getInt("new-order-gui.cancel-button.slot", 10);
   }

   public Material newOrderGuiCancelMat() {
      return this.mat(this.myOrderCfg, "new-order-gui.cancel-button.material", Material.RED_STAINED_GLASS_PANE);
   }

   public String newOrderGuiCancelName() {
      return this.myOrderCfg.getString("new-order-gui.cancel-button.name", "");
   }

   public List<String> newOrderGuiCancelLore() {
      return this.myOrderCfg.getStringList("new-order-gui.cancel-button.lore");
   }

   public int newOrderGuiItemSlot() {
      return this.myOrderCfg.getInt("new-order-gui.item-button.slot", 12);
   }

   public Material newOrderGuiItemFallbackMat() {
      return this.mat(this.myOrderCfg, "new-order-gui.item-button.fallback-material", Material.STONE);
   }

   public String newOrderGuiItemName() {
      return this.myOrderCfg.getString("new-order-gui.item-button.name", "");
   }

   public List<String> newOrderGuiItemLore() {
      return this.myOrderCfg.getStringList("new-order-gui.item-button.lore");
   }

   public int newOrderGuiAmountSlot() {
      return this.myOrderCfg.getInt("new-order-gui.amount-button.slot", 13);
   }

   public Material newOrderGuiAmountMat() {
      return this.mat(this.myOrderCfg, "new-order-gui.amount-button.material", Material.CHEST);
   }

   public String newOrderGuiAmountName() {
      return this.myOrderCfg.getString("new-order-gui.amount-button.name", "");
   }

   public List<String> newOrderGuiAmountLore() {
      return this.myOrderCfg.getStringList("new-order-gui.amount-button.lore");
   }

   public int newOrderGuiPriceSlot() {
      return this.myOrderCfg.getInt("new-order-gui.price-button.slot", 14);
   }

   public Material newOrderGuiPriceMat() {
      return this.mat(this.myOrderCfg, "new-order-gui.price-button.material", Material.EMERALD);
   }

   public String newOrderGuiPriceName() {
      return this.myOrderCfg.getString("new-order-gui.price-button.name", "");
   }

   public List<String> newOrderGuiPriceLore() {
      return this.myOrderCfg.getStringList("new-order-gui.price-button.lore");
   }

   public int newOrderGuiConfirmSlot() {
      return this.myOrderCfg.getInt("new-order-gui.confirm-button.slot", 16);
   }

   public Material newOrderGuiConfirmMat() {
      return this.mat(this.myOrderCfg, "new-order-gui.confirm-button.material", Material.LIME_STAINED_GLASS_PANE);
   }

   public String newOrderGuiConfirmName() {
      return this.myOrderCfg.getString("new-order-gui.confirm-button.name", "");
   }

   public List<String> newOrderGuiConfirmLore() {
      return this.myOrderCfg.getStringList("new-order-gui.confirm-button.lore");
   }

   private Material mat(FileConfiguration cfg, String path, Material fallback) {
      String val = cfg.getString(path);
      if (val == null) {
         return fallback;
      } else {
         try {
            return Material.valueOf(val.toUpperCase());
         } catch (IllegalArgumentException var6) {
            return fallback;
         }
      }
   }
   public String enchantPickGuiTitle() {
      return this.labelsCfg.getString("enchant-pick-gui.title", "");
   }

   public String enchantPickGuiBackName() {
      return this.labelsCfg.getString("enchant-pick-gui.back-button-name", "");
   }

   public String enchantPickGuiNextName() {
      return this.labelsCfg.getString("enchant-pick-gui.next-button-name", "");
   }

   public String enchantPickGuiBookSelectLore() {
      return this.labelsCfg.getString("enchant-pick-gui.book-click-to-select-lore", "");
   }

   public String enchantPickGuiBookSelectedLore() {
      return this.labelsCfg.getString("enchant-pick-gui.book-selected-lore", "");
   }

   public String enchantPickGuiBookCannotAddLore() {
      return this.labelsCfg.getString("enchant-pick-gui.book-cannot-add-lore", "");
   }

   public String enchantPickGuiCancelName() {
      return this.labelsCfg.getString("enchant-pick-gui.cancel-button-name", "");
   }

   public String enchantPickGuiCancelLore() {
      return this.labelsCfg.getString("enchant-pick-gui.cancel-button-lore", "");
   }

   public String enchantPickGuiConfirmName() {
      return this.labelsCfg.getString("enchant-pick-gui.confirm-button-name", "");
   }

   public String enchantPickGuiConfirmLore() {
      return this.labelsCfg.getString("enchant-pick-gui.confirm-button-lore", "");
   }

   public String deliverItemsGuiTitlePart1() {
      return this.labelsCfg.getString("deliver-items-gui.title-part1", "");
   }

   public String deliverItemsGuiTitlePart2() {
      return this.labelsCfg.getString("deliver-items-gui.title-part2", "");
   }

   public String ordersGuiDeliverLore(String player, String item) {
      return this.labelsCfg.getString("orders-gui.deliver-lore", "")
         .replace("{player}", player)
         .replace("{item}", item);
   }

   public String ordersGuiBullet() {
      return this.labelsCfg.getString("orders-gui.bullet", "");
   }

   public String selectItemGuiBullet() {
      return this.labelsCfg.getString("select-item-gui.bullet", "");
   }

   public String confirmDeliveryGuiPriceLore(String amount) {
      return this.labelsCfg.getString("confirm-delivery-gui.price-lore", "").replace("{amount}", amount);
   }

   public String confirmDeliveryGuiDeliveringLore(String amount, String item) {
      return this.labelsCfg.getString("confirm-delivery-gui.delivering-lore", "")
         .replace("{amount}", amount)
         .replace("{item}", item);
   }

   public String newOrderGuiPriceLore(String amount) {
      return this.labelsCfg.getString("new-order-gui.price-lore", "").replace("{amount}", amount);
   }

   public String newOrderGuiTotalLore(String amount) {
      return this.labelsCfg.getString("new-order-gui.total-lore", "").replace("{amount}", amount);
   }

   public String yourOrdersGuiEachSuffix() {
      return this.labelsCfg.getString("your-orders-gui.each-suffix", "");
   }

   public String yourOrdersGuiDeliveredSuffix() {
      return this.labelsCfg.getString("your-orders-gui.delivered-suffix", "");
   }

   public String yourOrdersGuiPaidSuffix() {
      return this.labelsCfg.getString("your-orders-gui.paid-suffix", "");
   }
}
