package me.vennlmao.ariscore.team.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TeamMenuHolder implements InventoryHolder {

    public enum Screen { MAIN, MEMBER_ACTIONS, KICK_CONFIRM, LEAVE_CONFIRM, DISBAND_CONFIRM, TRANSFER_CONFIRM }

    private final Screen screen;
    private final String teamName;
    private final int page;
    private final UUID target;
    private Inventory inventory;

    public TeamMenuHolder(Screen screen, String teamName, int page, UUID target) {
        this.screen = screen;
        this.teamName = teamName;
        this.page = page;
        this.target = target;
    }

    public Screen getScreen() { return screen; }
    public String getTeamName() { return teamName; }
    public int getPage() { return page; }
    public UUID getTarget() { return target; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
  }
