package me.vennlmao.ariscore.team.gui;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.managers.TeamRole;
import me.vennlmao.ariscore.team.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class TeamGuiBuilder {

    public static final int PAGE_SIZE = 45;

    private final TeamModule module;

    public TeamGuiBuilder(TeamModule module) { this.module = module; }

    public Inventory buildMain(Player viewer, TeamData team, int page) {
        String title = module.getConfig().getString("gui.main.title", "")
                .replace("{team-name}", team.getName())
                .replace("{page}", String.valueOf(page + 1));
        int size = module.getConfig().getInt("gui.main.size", 54);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        applyBorder(inv, "gui.main.border");

        List<UUID> sorted = new ArrayList<>(team.getMembers().keySet());
        sorted.sort(Comparator.comparing((UUID u) -> team.getRole(u).ordinal())
                .thenComparingLong(team::getJoinDate));

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sorted.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildMemberHead(sorted.get(i), team));
        }

        int maxMembers = module.getConfig().getInt("team.max-members", 45);
        for (int i = end - start; i < Math.min(PAGE_SIZE, maxMembers); i++) {
            inv.setItem(i, buildEmptySlot());
        }

        int maxPage = Math.max(0, (sorted.size() - 1) / PAGE_SIZE);

        buildTeamInfoIcon(inv, team, page, maxPage, maxMembers);

        placeControl(inv, "gui.main.items.home", s -> s);

        placeControl(inv, "gui.main.items.pvp", s -> s.replace("{pvp-status}", team.isPvpEnabled()
                ? module.getConfig().getString("gui.main.items.pvp.status.enabled", "")
                : module.getConfig().getString("gui.main.items.pvp.status.disabled", "")));

        placeControl(inv, "gui.main.items.invite", s -> s);
        placeControl(inv, "gui.main.items.chat", s -> s);
        placeControl(inv, "gui.main.items.endchest", s -> s);

        boolean leader = team.isLeader(viewer.getUniqueId());
        placeControl(inv, leader ? "gui.main.items.disband" : "gui.main.items.leave", s -> s);

        placeControl(inv, "gui.main.items.previous-page", s -> s);
        placeControl(inv, "gui.main.items.next-page", s -> s);

        return inv;
    }

    private void buildTeamInfoIcon(Inventory inv, TeamData team, int page, int maxPage, int maxMembers) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection("gui.main.items.team-info");
        if (sec == null) return;
        int slot = sec.getInt("slot", -1);
        if (slot < 0) return;

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return;
        if (team.getLeader() != null) meta.setOwningPlayer(Bukkit.getOfflinePlayer(team.getLeader()));

        UnaryOperator<String> replacer = s -> s
                .replace("{team-name}", team.getName())
                .replace("{tag}", team.getTag() != null ? team.getTag() : "")
                .replace("{leader}", offlineName(team.getLeader()))
                .replace("{count}", String.valueOf(team.getMemberCount()))
                .replace("{max}", String.valueOf(maxMembers))
                .replace("{page}", String.valueOf(page + 1))
                .replace("{max-page}", String.valueOf(maxPage + 1));

        meta.displayName(ColorUtil.parse(replacer.apply(sec.getString("display-name", ""))));
        List<Component> lore = new ArrayList<>();
        for (String l : sec.getStringList("lore")) lore.add(ColorUtil.parse(replacer.apply(l)));
        meta.lore(lore);

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private String offlineName(UUID uuid) {
        if (uuid == null) return "?";
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : "?";
    }

    private ItemStack buildMemberHead(UUID uuid, TeamData team) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        boolean online = Bukkit.getPlayer(uuid) != null;
        TeamRole role = team.getRole(uuid);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;
        meta.setOwningPlayer(op);

        String name = role.icon(module) + " " + (op.getName() != null ? op.getName() : "Unknown");
        meta.displayName(ColorUtil.parse(name));

        List<Component> lore = new ArrayList<>();
        String onlineLine = module.getConfig().getString(online
                ? "gui.main.member-display.online-line"
                : "gui.main.member-display.offline-line", "");
        lore.add(ColorUtil.parse(onlineLine));
        lore.add(ColorUtil.parse(module.getConfig().getString("gui.main.member-display.role-line", "")
                .replace("{role}", role.display(module))));
        lore.add(ColorUtil.parse(module.getConfig().getString("gui.main.member-display.click-line", "")));
        meta.lore(lore);

        head.setItemMeta(meta);
        return head;
    }

    private ItemStack buildEmptySlot() {
        String path = "gui.main.empty-slot";
        Material mat = Material.matchMaterial(module.getConfig().getString(path + ".material", "GRAY_STAINED_GLASS_PANE"));
        if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;
        return buildItem(mat, module.getConfig().getString(path + ".display-name", ""),
                module.getConfig().getStringList(path + ".lore"), s -> s);
    }

    public Inventory buildMemberActions(TeamData team, UUID targetUuid) {
        String title = module.getConfig().getString("gui.member-actions.title", "")
                .replace("{player}", offlineName(targetUuid));
        int size = module.getConfig().getInt("gui.member-actions.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        applyBorder(inv, "gui.member-actions.border");

        TeamRole role = team.getRole(targetUuid);
        final String pName = offlineName(targetUuid);

        ConfigurationSection headSec = module.getConfig().getConfigurationSection("gui.member-actions.head");
        if (headSec != null) {
            int slot = headSec.getInt("slot", 13);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
                meta.displayName(ColorUtil.parse(headSec.getString("display-name", "").replace("{player}", pName)));
                List<Component> lore = new ArrayList<>();
                for (String l : headSec.getStringList("lore")) {
                    lore.add(ColorUtil.parse(l.replace("{player}", pName).replace("{role}", role != null ? role.display(module) : "?")));
                }
                meta.lore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
        }

        placeControl(inv, "gui.member-actions.promote", s -> s.replace("{player}", pName));
        placeControl(inv, "gui.member-actions.demote", s -> s.replace("{player}", pName));
        placeControl(inv, "gui.member-actions.transfer", s -> s.replace("{player}", pName));
        placeControl(inv, "gui.member-actions.kick", s -> s.replace("{player}", pName));
        placeControl(inv, "gui.member-actions.back", s -> s);

        return inv;
    }

    public Inventory buildKickConfirm(OfflinePlayer target) {
        return buildConfirmGui("gui.kick-confirm", target.getName());
    }

    public Inventory buildLeaveConfirm() {
        return buildConfirmGui("gui.leave-confirm", null);
    }

    public Inventory buildDisbandConfirm() {
        return buildConfirmGui("gui.disband-confirm", null);
    }

    public Inventory buildTransferConfirm(OfflinePlayer target) {
        return buildConfirmGui("gui.transfer-confirm", target.getName());
    }

    private Inventory buildConfirmGui(String base, String playerName) {
        String title = module.getConfig().getString(base + ".title", "")
                .replace("{player}", playerName != null ? playerName : "");
        int size = module.getConfig().getInt(base + ".size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        applyBorder(inv, base + ".border");
        placeControl(inv, base + ".cancel", s -> s.replace("{player}", playerName != null ? playerName : ""));
        placeControl(inv, base + ".confirm", s -> s.replace("{player}", playerName != null ? playerName : ""));

        return inv;
    }

    private void applyBorder(Inventory inv, String path) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection(path);
        if (sec == null || !sec.getBoolean("enabled", false)) return;

        Material mat = Material.matchMaterial(sec.getString("material", "GRAY_STAINED_GLASS_PANE"));
        if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;
        String name = sec.getString("display-name", " ");

        ItemStack filler = buildItem(mat, name, List.of(), s -> s);
        for (int slot : sec.getIntegerList("slots")) {
            if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, filler);
        }
    }

    private void placeControl(Inventory inv, String path, UnaryOperator<String> replacer) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection(path);
        if (sec == null) return;
        int slot = sec.getInt("slot", -1);
        if (slot < 0) return;
        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        String name = replacer.apply(sec.getString("display-name", ""));
        inv.setItem(slot, buildItem(mat, name, sec.getStringList("lore"), replacer));
    }

    private ItemStack buildItem(Material mat, String name, List<String> lore, UnaryOperator<String> replacer) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.parse(name));
        List<Component> loreComp = new ArrayList<>();
        for (String l : lore) loreComp.add(ColorUtil.parse(replacer.apply(l)));
        meta.lore(loreComp);
        item.setItemMeta(meta);
        return item;
    }
            }
                         
