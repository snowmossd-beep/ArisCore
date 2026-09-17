package me.vennlmao.ariscore.rtpqueue.listeners;

import me.vennlmao.ariscore.rtpqueue.RtpQueueModule;
import me.vennlmao.ariscore.rtpqueue.gui.GuiUtil;
import me.vennlmao.ariscore.rtpqueue.utils.MessageUtil;
import me.vennlmao.ariscore.rtpqueue.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiListener implements Listener {

    private final RtpQueueModule module;

    public GuiListener(RtpQueueModule module) {
        this.module = module;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String mainTitle = GuiUtil.stripColor(module.getConfig().getString("gui.main.title", ""));

        if (title.equals(mainTitle)) {
            module.getQueueManager().addMainViewer(player.getUniqueId(), event.getInventory());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        module.getQueueManager().removeMainViewer(player.getUniqueId());
        module.getQueueManager().removeWorldViewer(player.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String clickedTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        String mainTitle = GuiUtil.stripColor(module.getConfig().getString("gui.main.title", ""));
        String worldTitle = GuiUtil.stripColor(module.getConfig().getString("gui.world_select.title", ""));

        boolean isMain = clickedTitle.equals(mainTitle);
        boolean isWorld = clickedTitle.equals(worldTitle);

        if (!isMain && !isWorld) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.displayName() == null) return;

        int slot = event.getSlot();

        if (isMain) {
            handleMainClick(player, slot);
        } else {
            handleWorldClick(player, slot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        int cancelSlot = module.getConfig().getInt("gui.main.slots.cancel.slot", -1);
        int worldSlot = module.getConfig().getInt("gui.main.slots.world.slot", -1);
        int confirmSlot = module.getConfig().getInt("gui.main.slots.confirm.slot", -1);

        if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            if (module.getQueueManager().isQueued(player.getUniqueId())) {
                module.getQueueManager().leaveQueue(player);
            } else {
                MessageUtil.sendChatList(player, "not_queued");
                SoundUtil.play(player, "error");
            }
            refreshMain(player);
            return;
        }

        if (slot == worldSlot) {
            SoundUtil.play(player, "click");
            player.getScheduler().run(module.getPlugin(), t -> {
                module.getQueueManager().addWorldViewer(player.getUniqueId());
                player.openInventory(GuiUtil.buildWorldSelectGui(module, player));
                SoundUtil.play(player, "open_gui");
            }, null);
            return;
        }

        if (slot == confirmSlot) {
            if (module.getQueueManager().isQueued(player.getUniqueId())) {
                MessageUtil.sendChatList(player, "already_queued");
                SoundUtil.play(player, "error");
                return;
            }

            SoundUtil.play(player, "confirm");
            module.getQueueManager().joinQueue(player);
            refreshMain(player);
        }
    }

    private void handleWorldClick(Player player, int slot) {
        ConfigurationSection worlds = module.getConfig().getConfigurationSection("worlds");
        if (worlds == null) return;

        for (String key : worlds.getKeys(false)) {
            ConfigurationSection sec = worlds.getConfigurationSection(key);
            if (sec == null || !sec.getBoolean("enabled", true)) continue;
            if (sec.getInt("slot", -1) != slot) continue;

            module.getQueueManager().setSelectedWorld(player.getUniqueId(), key);
            SoundUtil.play(player, "click");

            player.getScheduler().run(module.getPlugin(), t -> {
                module.getQueueManager().removeWorldViewer(player.getUniqueId());
                player.openInventory(GuiUtil.buildMainGui(module, player));
                module.getQueueManager().addMainViewer(player.getUniqueId(), player.getOpenInventory().getTopInventory());
            }, null);
            return;
        }
    }

    private void refreshMain(Player player) {
        player.getScheduler().run(module.getPlugin(), t -> {
            var open = player.getOpenInventory().getTopInventory();
            String title = PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().title());
            String mainTitle = GuiUtil.stripColor(module.getConfig().getString("gui.main.title", ""));
            if (title.equals(mainTitle)) {
                GuiUtil.updateDynamicSlots(module, open, player);
            }
        }, null);
    }
}
