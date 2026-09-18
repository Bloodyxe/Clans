package de.customclans.clans;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;

/**
 * Marks an Inventory as the clan-info GUI and remembers which member each slot represents,
 * so the click listener knows who was clicked without guessing from the item stack alone.
 */
public class ClanInfoHolder implements InventoryHolder {

    private final String clanName;
    private final Map<Integer, UUID> slotMembers;
    private Inventory inventory;
    private int backSlot = -1;

    public ClanInfoHolder(String clanName, Map<Integer, UUID> slotMembers) {
        this.clanName = clanName;
        this.slotMembers = slotMembers;
    }

    public void setBackSlot(int backSlot) {
        this.backSlot = backSlot;
    }

    public int getBackSlot() {
        return backSlot;
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

    public UUID getMemberAt(int slot) {
        return slotMembers.get(slot);
    }
}
