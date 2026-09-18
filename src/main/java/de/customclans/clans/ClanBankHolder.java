package de.customclans.clans;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an Inventory as the clan bank GUI (27 slots), opened from the main menu's bank button.
 */
public class ClanBankHolder implements InventoryHolder {

    private final String clanName;
    private Inventory inventory;

    public ClanBankHolder(String clanName) {
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
