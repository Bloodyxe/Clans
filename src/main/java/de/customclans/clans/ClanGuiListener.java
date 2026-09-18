package de.customclans.clans;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Handles clicks inside all of the clan GUIs: the main menu (ClanMainHolder), the member
 * list (ClanInfoHolder) and the bank menu (ClanBankHolder).
 */
public class ClanGuiListener implements Listener {

    private final ClanManager clanManager;
    private final EconomyHook economyHook;
    private final BankActionManager bankActionManager;

    public ClanGuiListener(ClanManager clanManager, EconomyHook economyHook, BankActionManager bankActionManager) {
        this.clanManager = clanManager;
        this.economyHook = economyHook;
        this.bankActionManager = bankActionManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof ClanMainHolder mainHolder) {
            event.setCancelled(true);
            handleMainClick(event, mainHolder);
        } else if (holder instanceof ClanInfoHolder infoHolder) {
            event.setCancelled(true);
            handleInfoClick(event, infoHolder);
        } else if (holder instanceof ClanBankHolder bankHolder) {
            event.setCancelled(true);
            handleBankClick(event, bankHolder);
        }
    }

    // ---------------------------------------------------------------- main menu

    private void handleMainClick(InventoryClickEvent event, ClanMainHolder mainHolder) {
        if (!(event.getWhoClicked() instanceof Player viewer)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        Clan clan = clanManager.getClanByName(mainHolder.getClanName());
        if (clan == null) {
            return;
        }
        Clan viewerClan = clanManager.getClanByPlayer(viewer.getUniqueId());
        if (viewerClan == null || !viewerClan.getName().equalsIgnoreCase(clan.getName())) {
            return;
        }

        switch (slot) {
            case ClanMainGui.MEMBERS_SLOT -> {
                viewer.closeInventory();
                viewer.openInventory(ClanInfoGui.build(clan));
            }
            case ClanMainGui.BANK_SLOT -> {
                viewer.closeInventory();
                viewer.openInventory(ClanBankGui.build(clan, economyHook));
            }
            case ClanMainGui.PVP_SLOT -> {
                if (viewerClan.getRank(viewer.getUniqueId()) != Rank.LEADER) {
                    viewer.sendMessage(color("&cOnly the clan leader can toggle Clan PVP."));
                    return;
                }
                clan.setPvpEnabled(!clan.isPvpEnabled());
                clanManager.save(clan);
                viewer.closeInventory();
                viewer.openInventory(ClanMainGui.build(clan, economyHook));
                viewer.sendMessage(color(clan.isPvpEnabled()
                        ? "&aClan PVP is now &fON&a."
                        : "&cClan PVP is now &fOFF&c."));
            }
            default -> {
                // clicked a filler pane or empty slot - do nothing
            }
        }
    }

    // ---------------------------------------------------------------- member list

    private void handleInfoClick(InventoryClickEvent event, ClanInfoHolder clanHolder) {
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

    // ---------------------------------------------------------------- bank menu

    private void handleBankClick(InventoryClickEvent event, ClanBankHolder bankHolder) {
        if (!(event.getWhoClicked() instanceof Player viewer)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        Clan clan = clanManager.getClanByName(bankHolder.getClanName());
        if (clan == null) {
            return;
        }
        Clan viewerClan = clanManager.getClanByPlayer(viewer.getUniqueId());
        if (viewerClan == null || !viewerClan.getName().equalsIgnoreCase(clan.getName())) {
            return;
        }
        if (!economyHook.isReady()) {
            viewer.sendMessage(color("&cNo economy plugin connected. Bank commands are disabled."));
            return;
        }

        if (slot == ClanBankGui.DEPOSIT_SLOT) {
            bankActionManager.setPending(viewer.getUniqueId(), BankActionManager.Action.DEPOSIT);
            viewer.closeInventory();
            viewer.sendMessage(color("&aType the amount you want to deposit into the clan bank in chat."));
            viewer.sendMessage(color("&7Type &fcancel &7to cancel."));
        } else if (slot == ClanBankGui.WITHDRAW_SLOT) {
            Rank rank = viewerClan.getRank(viewer.getUniqueId());
            if (rank != Rank.LEADER && rank != Rank.OFFICER) {
                viewer.sendMessage(color("&cOnly leaders and officers can withdraw money."));
                return;
            }
            bankActionManager.setPending(viewer.getUniqueId(), BankActionManager.Action.WITHDRAW);
            viewer.closeInventory();
            viewer.sendMessage(color("&aType the amount you want to withdraw from the clan bank in chat."));
            viewer.sendMessage(color("&7Type &fcancel &7to cancel."));
        }
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
