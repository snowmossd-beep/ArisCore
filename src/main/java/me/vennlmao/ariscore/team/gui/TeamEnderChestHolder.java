package me.vennlmao.ariscore.team.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class TeamEnderChestHolder implements InventoryHolder {

    private final String teamName;
    private Inventory inventory;

    public TeamEnderChestHolder(String teamName) { this.teamName = teamName; }

    public String getTeamName() { return teamName; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
}
