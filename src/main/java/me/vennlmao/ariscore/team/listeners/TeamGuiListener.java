package me.vennlmao.ariscore.team.listeners;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.gui.TeamGuiBuilder;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.managers.TeamRole;
import me.vennlmao.ariscore.team.utils.ColorUtil;
import me.vennlmao.ariscore.team.utils.MessageUtil;
import me.vennlmao.ariscore.team.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamGuiListener implements Listener {

    private final TeamModule module;
    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, UUID> pendingTarget = new HashMap<>();

    public TeamGuiListener(TeamModule module) { this.module = module; }

    public void openMain(Player player, int page) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;
        int p = Math.max(0, page);
        playerPage.put(player.getUniqueId(), p);
        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(module.getGuiBuilder().buildMain(player, team, p)), null);
    }

    private void openMemberActions(Player player, TeamData team, UUID target) {
        pendingTarget.put(player.getUniqueId(), target);
        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(module.getGuiBuilder().buildMemberActions(team, target)), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        String mainBase = ColorUtil.strip(module.getConfig().getString("gui.main.title", ""))
                .replaceAll("\\{[^}]*}", "").trim();
        String memberActionsTitle = ColorUtil.strip(module.getConfig().getString("gui.member-actions.title", ""))
                .replaceAll("\\{[^}]*}", "").trim();
        String kickTitle = ColorUtil.strip(module.getConfig().getString("gui.kick-confirm.title", ""))
                .replaceAll("\\{[^}]*}", "").trim();
        String leaveTitle = ColorUtil.strip(module.getConfig().getString("gui.leave-confirm.title", ""));
        String disbandTitle = ColorUtil.strip(module.getConfig().getString("gui.disband-confirm.title", ""));
        String transferTitle = ColorUtil.strip(module.getConfig().getString("gui.transfer-confirm.title", ""))
                .replaceAll("\\{[^}]*}", "").trim();

        if (title.contains(mainBase) && !mainBase.isEmpty()) {
            event.setCancelled(true);
            handleMain(player, event.getSlot(), event.getCurrentItem());
        } else if (title.contains(memberActionsTitle) && !memberActionsTitle.isEmpty()) {
            event.setCancelled(true);
            handleMemberActions(player, event.getSlot());
        } else if (title.contains(kickTitle) && !kickTitle.isEmpty()) {
            event.setCancelled(true);
            handleKickConfirm(player, event.getSlot());
        } else if (title.equals(leaveTitle)) {
            event.setCancelled(true);
            handleLeaveConfirm(player, event.getSlot());
        } else if (title.equals(disbandTitle)) {
            event.setCancelled(true);
            handleDisbandConfirm(player, event.getSlot());
        } else if (title.contains(transferTitle) && !transferTitle.isEmpty()) {
            event.setCancelled(true);
            handleTransferConfirm(player, event.getSlot());
        }
    }

    private void handleMain(Player player, int slot, ItemStack clicked) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) { player.closeInventory(); return; }

        int prevSlot = module.getConfig().getInt("gui.main.items.previous-page.slot", -1);
        int nextSlot = module.getConfig().getInt("gui.main.items.next-page.slot", -1);
        int homeSlot = module.getConfig().getInt("gui.main.items.home.slot", -1);
        int pvpSlot = module.getConfig().getInt("gui.main.items.pvp.slot", -1);
        int inviteSlot = module.getConfig().getInt("gui.main.items.invite.slot", -1);
        int chatSlot = module.getConfig().getInt("gui.main.items.chat.slot", -1);
        int endchestSlot = module.getConfig().getInt("gui.main.items.endchest.slot", -1);
        int disbandSlot = module.getConfig().getInt("gui.main.items.disband.slot", -1);
        int leaveSlot = module.getConfig().getInt("gui.main.items.leave.slot", -1);

        int page = playerPage.getOrDefault(player.getUniqueId(), 0);

        if (slot == prevSlot) {
            SoundUtil.play(player, "page");
            if (page > 0) openMain(player, page - 1);
            return;
        }
        if (slot == nextSlot) {
            SoundUtil.play(player, "page");
            int maxPage = Math.max(0, (team.getMemberCount() - 1) / TeamGuiBuilder.PAGE_SIZE);
            if (page < maxPage) openMain(player, page + 1);
            return;
        }
        if (slot == homeSlot) {
            player.closeInventory();
            if (team.getHome() == null) { MessageUtil.sendChat(player, "home_not_set"); SoundUtil.play(player, "error"); return; }
            TeamRole role = team.getRole(player.getUniqueId());
            if (role == null || !role.has(module, "can-visit-home")) { MessageUtil.sendChat(player, "no_permission"); SoundUtil.play(player, "error"); return; }
            module.getWarmupManager().startWarmup(player, team.getHome());
            return;
        }
        if (slot == pvpSlot) {
            TeamRole role = team.getRole(player.getUniqueId());
            if (role == null || !role.has(module, "can-toggle-pvp")) { SoundUtil.play(player, "error"); return; }
            module.getTeamManager().setPvp(team.getName(), !team.isPvpEnabled());
            SoundUtil.play(player, "click");
            openMain(player, page);
            return;
        }
        if (slot == inviteSlot) {
            player.closeInventory();
            MessageUtil.sendChat(player, "invite_hint");
            SoundUtil.play(player, "click");
            return;
        }
        if (slot == chatSlot) {
            TeamRole role = team.getRole(player.getUniqueId());
            if (role == null || !role.has(module, "can-team-chat")) { SoundUtil.play(player, "error"); return; }
            module.getChatListener().toggleChat(player);
            SoundUtil.play(player, "click");
            openMain(player, page);
            return;
        }
        if (slot == endchestSlot) {
            TeamRole role = team.getRole(player.getUniqueId());
            if (role == null || !role.has(module, "can-open-endchest")) { SoundUtil.play(player, "error"); return; }
            SoundUtil.play(player, "click");
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getTeamEnderChestManager().getInventory(team.getName())), null);
            return;
        }
        if (slot == disbandSlot && team.isLeader(player.getUniqueId())) {
            player.getScheduler().run(module.getPlugin(), t -> player.openInventory(module.getGuiBuilder().buildDisbandConfirm()), null);
            return;
        }
        if (slot == leaveSlot && !team.isLeader(player.getUniqueId())) {
            player.getScheduler().run(module.getPlugin(), t -> player.openInventory(module.getGuiBuilder().buildLeaveConfirm()), null);
            return;
        }

        if (slot >= 0 && slot < TeamGuiBuilder.PAGE_SIZE && clicked.getItemMeta() instanceof SkullMeta skullMeta) {
            OfflinePlayer op = skullMeta.getOwningPlayer();
            if (op == null) return;
            UUID targetUuid = op.getUniqueId();

            if (targetUuid.equals(player.getUniqueId())) {
                if (team.isLeader(player.getUniqueId())) {
                    MessageUtil.sendChat(player, "leader_must_transfer");
                    SoundUtil.play(player, "error");
                } else {
                    player.getScheduler().run(module.getPlugin(), t -> player.openInventory(module.getGuiBuilder().buildLeaveConfirm()), null);
                }
                return;
            }

            TeamRole viewerRole = team.getRole(player.getUniqueId());
            boolean canManage = viewerRole == TeamRole.LEADER || (viewerRole != null && viewerRole.has(module, "can-kick"));
            if (!canManage) {
                MessageUtil.sendChat(player, "member_cannot_manage");
                SoundUtil.play(player, "error");
                return;
            }

            SoundUtil.play(player, "click");
            openMemberActions(player, team, targetUuid);
        }
    }

    private void handleMemberActions(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) { player.closeInventory(); return; }

        UUID target = pendingTarget.get(player.getUniqueId());
        if (target == null) { player.closeInventory(); return; }

        int backSlot = module.getConfig().getInt("gui.member-actions.back.slot", -1);
        int promoteSlot = module.getConfig().getInt("gui.member-actions.promote.slot", -1);
        int demoteSlot = module.getConfig().getInt("gui.member-actions.demote.slot", -1);
        int transferSlot = module.getConfig().getInt("gui.member-actions.transfer.slot", -1);
        int kickSlot = module.getConfig().getInt("gui.member-actions.kick.slot", -1);

        if (slot == backSlot) {
            SoundUtil.play(player, "click");
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        boolean isLeader = team.isLeader(player.getUniqueId());
        TeamRole targetRole = team.getRole(target);

        if (slot == promoteSlot) {
            if (!isLeader || targetRole != TeamRole.MEMBER) { SoundUtil.play(player, "error"); return; }
            module.getTeamManager().setRole(team, target, TeamRole.ADMIN);
            MessageUtil.sendChat(player, "promote", s -> s.replace("{player}", offlineName(target)));
            SoundUtil.play(player, "success");
            openMemberActions(player, team, target);
            return;
        }
        if (slot == demoteSlot) {
            if (!isLeader || targetRole != TeamRole.ADMIN) { SoundUtil.play(player, "error"); return; }
            module.getTeamManager().setRole(team, target, TeamRole.MEMBER);
            MessageUtil.sendChat(player, "demote", s -> s.replace("{player}", offlineName(target)));
            SoundUtil.play(player, "click");
            openMemberActions(player, team, target);
            return;
        }
        if (slot == transferSlot) {
            if (!isLeader) { SoundUtil.play(player, "error"); return; }
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getGuiBuilder().buildTransferConfirm(Bukkit.getOfflinePlayer(target))), null);
            return;
        }
        if (slot == kickSlot) {
            TeamRole viewerRole = team.getRole(player.getUniqueId());
            boolean canKick = isLeader || (viewerRole != null && viewerRole.has(module, "can-kick"));
            if (!canKick || targetRole == TeamRole.LEADER) { SoundUtil.play(player, "error"); return; }
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getGuiBuilder().buildKickConfirm(Bukkit.getOfflinePlayer(target))), null);
        }
    }

    private void handleKickConfirm(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) { player.closeInventory(); return; }

        int confirmSlot = module.getConfig().getInt("gui.kick-confirm.confirm.slot", -1);
        int cancelSlot = module.getConfig().getInt("gui.kick-confirm.cancel.slot", -1);

        if (slot == confirmSlot) {
            UUID target = pendingTarget.remove(player.getUniqueId());
            if (target == null) return;
            String name = offlineName(target);
            module.getTeamManager().removeMember(target);
            player.closeInventory();
            MessageUtil.sendChat(player, "kick", s -> s.replace("{player}", name));
            SoundUtil.play(player, "success");
            Player online = Bukkit.getPlayer(target);
            if (online != null) {
                MessageUtil.sendChat(online, "kick_notify", s -> s.replace("{team}", team.getName()));
                SoundUtil.play(online, "error");
            }
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            UUID target = pendingTarget.get(player.getUniqueId());
            if (target != null) {
                openMemberActions(player, team, target);
            } else {
                openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
            }
        }
    }

    private void handleLeaveConfirm(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) { player.closeInventory(); return; }

        int confirmSlot = module.getConfig().getInt("gui.leave-confirm.confirm.slot", -1);
        int cancelSlot = module.getConfig().getInt("gui.leave-confirm.cancel.slot", -1);

        if (slot == confirmSlot) {
            if (team.isLeader(player.getUniqueId())) { player.closeInventory(); return; }
            String teamName = team.getName();
            module.getTeamManager().removeMember(player.getUniqueId());
            player.closeInventory();
            MessageUtil.sendChat(player, "leave", s -> s.replace("{team}", teamName));
            SoundUtil.play(player, "click");
            for (UUID uuid : team.getMembers().keySet()) {
                Player m = Bukkit.getPlayer(uuid);
                if (m != null) MessageUtil.sendChat(m, "leave_notify", s -> s.replace("{player}", player.getName()));
            }
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
        }
    }

    private void handleDisbandConfirm(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());

        int confirmSlot = module.getConfig().getInt("gui.disband-confirm.confirm.slot", -1);
        int cancelSlot = module.getConfig().getInt("gui.disband-confirm.cancel.slot", -1);

        if (slot == confirmSlot) {
            if (team == null || !team.isLeader(player.getUniqueId())) { player.closeInventory(); return; }
            String teamName = team.getName();
            for (UUID uuid : team.getMembers().keySet()) {
                Player m = Bukkit.getPlayer(uuid);
                if (m != null && !m.equals(player)) MessageUtil.sendChat(m, "disband_notify", s -> s.replace("{team}", teamName));
            }
            module.getTeamManager().disbandTeam(teamName);
            pendingTarget.remove(player.getUniqueId());
            playerPage.remove(player.getUniqueId());
            player.closeInventory();
            MessageUtil.sendChat(player, "disband", s -> s.replace("{team}", teamName));
            SoundUtil.play(player, "error");
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            if (team == null || !team.isLeader(player.getUniqueId())) { player.closeInventory(); return; }
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
        }
    }

    private void handleTransferConfirm(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) { player.closeInventory(); return; }

        int confirmSlot = module.getConfig().getInt("gui.transfer-confirm.confirm.slot", -1);
        int cancelSlot = module.getConfig().getInt("gui.transfer-confirm.cancel.slot", -1);

        if (slot == confirmSlot) {
            UUID target = pendingTarget.remove(player.getUniqueId());
            if (target == null || !team.isLeader(player.getUniqueId())) { player.closeInventory(); return; }
            module.getTeamManager().transferLeadership(team, target);
            player.closeInventory();
            MessageUtil.sendChat(player, "transfer", s -> s.replace("{player}", offlineName(target)));
            SoundUtil.play(player, "success");
            Player online = Bukkit.getPlayer(target);
            if (online != null) {
                MessageUtil.sendChat(online, "transfer_notify", s -> s.replace("{team}", team.getName()));
                SoundUtil.play(online, "success");
            }
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            UUID target = pendingTarget.get(player.getUniqueId());
            if (target != null) {
                openMemberActions(player, team, target);
            } else {
                openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
            }
        }
    }

    private String offlineName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : "?";
    }
                               }
            
