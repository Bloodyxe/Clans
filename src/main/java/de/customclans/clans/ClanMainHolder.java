package de.customclans.clans;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an Inventory as the clan main menu (54 slots) opened by /clan info on your own clan.
 */
public class ClanMainHolder implements InventoryHolder {

    private final String clanName;
    private Inventory inventory;

    public ClanMainHolder(String clanName) {
        this.clanName = clanName;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String getClanName() {
        return clanName;
    }
}
