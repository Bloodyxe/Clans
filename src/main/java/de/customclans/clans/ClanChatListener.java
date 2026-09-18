package de.customclans.clans;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * While a player has Clan-Chat mode on (toggled via /clan chat), their normal chat
 * messages are intercepted here and rerouted so only online members of their own
 * clan can see them.
 *
 * Before that, this also handles the clan bank's chat-input flow: after clicking
 * deposit/withdraw in the bank GUI (see ClanGuiListener), the player's next chat
 * message is parsed as an amount instead of being sent as clan chat or normal chat.
 */
public class ClanChatListener implements Listener {

    private final ClanManager clanManager;
    private final ClanChatManager clanChatManager;
    private final EconomyHook economyHook;
    private final BankActionManager bankActionManager;

    public ClanChatListener(ClanManager clanManager, ClanChatManager clanChatManager,
                             EconomyHook economyHook, BankActionManager bankActionManager) {
        this.clanManager = clanManager;
        this.clanChatManager = clanChatManager;
        this.economyHook = economyHook;
        this.bankActionManager = bankActionManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (bankActionManager.hasPending(player.getUniqueId())) {
            event.setCancelled(true);
            handleBankAmount(player, event.getMessage());
            return;
        }

        if (!clanChatManager.isEnabled(player.getUniqueId())) {
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            // Player left/was removed from their clan while chat mode was still on.
            clanChatManager.disable(player.getUniqueId());
            player.sendMessage(color("&cYou are no longer in a clan. Clan chat has been disabled."));
            return;
        }

        event.setCancelled(true);

        String formatted = color("&d[Clan] &f" + player.getName() + "&7: &f" + event.getMessage());
        Server server = player.getServer();
        for (UUID member : clan.getMembers().keySet()) {
            Player online = server.getPlayer(member);
            if (online != null) {
                online.sendMessage(formatted);
            }
        }
    }

    private void handleBankAmount(Player player, String message) {
        BankActionManager.Action action = bankActionManager.getPending(player.getUniqueId());
        bankActionManager.clear(player.getUniqueId());

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(color("&7Bank action cancelled."));
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(color("&cYou are not in a clan."));
            return;
        }
        if (!economyHook.isReady()) {
            player.sendMessage(color("&cNo economy plugin connected. Bank commands are disabled."));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(message.trim());
        } catch (NumberFormatException e) {
            player.sendMessage(color("&cInvalid amount. Bank action cancelled."));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(color("&cThe amount must be greater than 0."));
            return;
        }

        if (action == BankActionManager.Action.DEPOSIT) {
            if (!economyHook.has(player, amount)) {
                player.sendMessage(color("&cYou don't have enough money."));
                return;
            }
            economyHook.withdrawPlayer(player, amount);
            clan.deposit(amount);
            clanManager.save(clan);
            player.sendMessage(color("&aYou deposited &f" + economyHook.format(amount) + " &ainto the clan bank."));
        } else if (action == BankActionManager.Action.WITHDRAW) {
            Rank rank = clan.getRank(player.getUniqueId());
            if (rank != Rank.LEADER && rank != Rank.OFFICER) {
                player.sendMessage(color("&cOnly leaders and officers can withdraw money."));
                return;
            }
            if (!clan.withdraw(amount)) {
                player.sendMessage(color("&cThe clan bank doesn't have enough funds."));
                return;
            }
            economyHook.depositPlayer(player, amount);
            clanManager.save(clan);
            player.sendMessage(color("&aYou withdrew &f" + economyHook.format(amount) + " &afrom the clan bank."));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clanChatManager.disable(event.getPlayer().getUniqueId());
        bankActionManager.clear(event.getPlayer().getUniqueId());
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
