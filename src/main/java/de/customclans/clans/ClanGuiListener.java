package de.customclans.clans;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Handles clicks inside the /clan info GUI (see ClanInfoGui). Clicking a non-leader member's
 * head toggles their /clan home access, but only if the clicker is the clan leader themselves.
 */
public class ClanGuiListener implements Listener {

    private final ClanManager clanManager;

    public ClanGuiListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ClanInfoHolder clanHolder)) {
            return;
        }
        // This is our GUI: never let items be taken out of it.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player viewer)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        UUID targetId = clanHolder.getMemberAt(slot);
        if (targetId == null) {
            return;
        }

        Clan clan = clanManager.getClanByName(clanHolder.getClanName());
        if (clan == null) {
            return;
        }
        Clan viewerClan = clanManager.getClanByPlayer(viewer.getUniqueId());
        if (viewerClan == null || !viewerClan.getName().equalsIgnoreCase(clan.getName())) {
            return;
        }
        if (viewerClan.getRank(viewer.getUniqueId()) != Rank.LEADER) {
            return;
        }
        if (clan.getRank(targetId) == Rank.LEADER) {
            // The leader always has home access - nothing to toggle.
            return;
        }

        boolean nowAllowed = clan.toggleHomePermission(targetId);
        clanManager.save(clan);

        viewer.closeInventory();
        viewer.openInventory(ClanInfoGui.build(clan));
        viewer.sendMessage(color(nowAllowed ? "&aHome access granted." : "&cHome access revoked."));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
