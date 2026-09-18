package me.vennlmao.ariscore.spawners.listeners;

import me.vennlmao.ariscore.spawners.SpawnersModule;
import me.vennlmao.ariscore.spawners.gui.SpawnerGuiBuilder;
import me.vennlmao.ariscore.spawners.gui.SpawnerGuiHolder;
import me.vennlmao.ariscore.spawners.managers.SpawnerData;
import me.vennlmao.ariscore.spawners.utils.MessageUtil;
import me.vennlmao.ariscore.spawners.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class SpawnerGuiListener implements Listener {

    private final SpawnersModule module;

    public SpawnerGuiListener(SpawnersModule module) {
        this.module = module;
    }

    public void openInfo(Player player, SpawnerData data) {
        Inventory inv = SpawnerGuiBuilder.buildInfo(module, data);
        player.getScheduler().run(module.getPlugin(), t -> player.openInventory(inv), null);
    }

    public void openStorage(Player player, SpawnerData data, int page) {
        Inventory inv = SpawnerGuiBuilder.buildStorage(module, data, page);
        player.getScheduler().run(module.getPlugin(), t -> player.openInventory(inv), null);
    }

    public void openConfirm(Player player, SpawnerData data, SpawnerGuiHolder.Screen returnTo) {
        Inventory inv = SpawnerGuiBuilder.buildConfirm(module, data, returnTo);
        player.getScheduler().run(module.getPlugin(), t -> player.openInventory(inv), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SpawnerGuiHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        SpawnerData data = holder.getData();
        FileConfiguration gui = module.getGuiConfig();
        int slot = event.getSlot();

        switch (holder.getScreen()) {
            case INFO -> handleInfoClick(player, data, gui, slot);
            case STORAGE -> handleStorageClick(player, data, holder, gui, slot, event.getCurrentItem());
            case CONFIRM -> handleConfirmClick(player, data, holder, gui, slot);
        }
    }

    private void handleInfoClick(Player player, SpawnerData data, FileConfiguration gui, int slot) {
        if (slot == gui.getInt("gui.info.items.open-storage.slot", 11)) {
            SoundUtil.play(player, "open");
            openStorage(player, data, 0);
        } else if (slot == gui.getInt("gui.info.items.fullness.slot", 13)) {
            if (data.totalStoredItems() <= 0 && data.getStoredXp() <= 0) {
                MessageUtil.sendChat(player, "storage_empty");
                return;
            }
            openConfirm(player, data, SpawnerGuiHolder.Screen.INFO);
        } else if (slot == gui.getInt("gui.info.items.collect-xp.slot", 15)) {
            if (data.getStoredXp() <= 0) {
                MessageUtil.sendChat(player, "no_xp");
                return;
            }
            long collected = data.getStoredXp();
            module.getSpawnerManager().collectXp(player, data);
            MessageUtil.sendChat(player, "collected_xp", s -> s.replace("{amount}", String.valueOf(collected)));
            SoundUtil.play(player, "xp");
            openInfo(player, data);
        }
    }

    private void handleStorageClick(Player player, SpawnerData data, SpawnerGuiHolder holder, FileConfiguration gui, int slot, ItemStack clicked) {
        if (slot == gui.getInt("storage.back.slot", 45)) {
            openInfo(player, data);
        } else if (slot == gui.getInt("storage.previous-page.slot", 48)) {
            SoundUtil.play(player, "page");
            openStorage(player, data, holder.getPage() - 1);
        } else if (slot == gui.getInt("storage.next-page.slot", 50)) {
            SoundUtil.play(player, "page");
            openStorage(player, data, holder.getPage() + 1);
        } else if (slot == gui.getInt("storage.drop-all.slot", 52)) {
            dropPage(player, data, holder);
        } else if (slot == gui.getInt("storage.sell-all.slot", 53)) {
            if (data.totalStoredItems() <= 0) {
                MessageUtil.sendChat(player, "storage_empty");
                return;
            }
            openConfirm(player, data, SpawnerGuiHolder.Screen.STORAGE);
        } else if (slot >= 0 && slot < SpawnerGuiBuilder.STORAGE_PAGE_SIZE) {
            collectItem(player, data, holder, clicked);
        }
    }

    private void collectItem(Player player, SpawnerData data, SpawnerGuiHolder holder, ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir()) return;

        long inStorage = data.getStoredCount(clicked.getType());
        if (inStorage <= 0) return;

        int take = (int) Math.min(Math.min(inStorage, clicked.getAmount()), 64);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(clicked.getType(), take));
        int leftoverAmount = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
        int given = take - leftoverAmount;

        if (given <= 0) {
            MessageUtil.sendChat(player, "inventory_full");
            return;
        }

        data.removeItem(clicked.getType(), given);
        MessageUtil.sendChat(player, "collected_items",
                s -> s.replace("{amount}", String.valueOf(given)).replace("{item}", SpawnerGuiBuilder.niceName(clicked.getType())));
        SoundUtil.play(player, "collect");
        openStorage(player, data, holder.getPage());
    }

    private void dropPage(Player player, SpawnerData data, SpawnerGuiHolder holder) {
        List<ItemStack> pageItems = SpawnerGuiBuilder.pageItems(data, holder.getPage());
        if (pageItems.isEmpty()) {
            MessageUtil.sendChat(player, "storage_empty");
            return;
        }

        int count = 0;
        for (ItemStack stack : pageItems) {
            long inStorage = data.getStoredCount(stack.getType());
            int take = (int) Math.min(inStorage, stack.getAmount());
            if (take <= 0) continue;
            data.removeItem(stack.getType(), take);
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(stack.getType(), take));
            count += take;
        }

        if (count <= 0) {
            MessageUtil.sendChat(player, "storage_empty");
            return;
        }

        MessageUtil.sendChat(player, "dropped_all");
        SoundUtil.play(player, "drop");
        openStorage(player, data, holder.getPage());
    }

    private void handleConfirmClick(Player player, SpawnerData data, SpawnerGuiHolder holder, FileConfiguration gui, int slot) {
        SpawnerGuiHolder.Screen returnTo = holder.getReturnTo();
        if (slot == gui.getInt("confirm-gui.items.decline.slot", 10)) {
            MessageUtil.sendChat(player, "sell_cancelled");
            returnTo(player, data, returnTo);
        } else if (slot == gui.getInt("confirm-gui.items.confirm.slot", 16)) {
            double earned = module.getSpawnerManager().sellAll(player, data);
            if (data.getStoredXp() > 0) {
                module.getSpawnerManager().collectXp(player, data);
            }
            if (earned > 0) {
                MessageUtil.sendChat(player, "sold_all", s -> s.replace("{amount}", SpawnerGuiBuilder.formatValue(module, earned)));
                SoundUtil.play(player, "sell");
            } else {
                MessageUtil.sendChat(player, "storage_empty");
            }
            returnTo(player, data, returnTo);
        }
    }

    private void returnTo(Player player, SpawnerData data, SpawnerGuiHolder.Screen screen) {
        if (screen == SpawnerGuiHolder.Screen.STORAGE) {
            openStorage(player, data, 0);
        } else {
            openInfo(player, data);
        }
    }
            }
                                 
