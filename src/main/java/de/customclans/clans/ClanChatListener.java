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
 */
public class ClanChatListener implements Listener {

    private final ClanManager clanManager;
    private final ClanChatManager clanChatManager;

    public ClanChatListener(ClanManager clanManager, ClanChatManager clanChatManager) {
        this.clanManager = clanManager;
        this.clanChatManager = clanChatManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!clanChatManager.isEnabled(player.getUniqueId())) {
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            // Player left/was removed from their clan while chat mode was still on.
            clanChatManager.disable(player.getUniqueId());
            player.sendMessage(color("&cDu bist in keinem Clan mehr. Clan-Chat wurde deaktiviert."));
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clanChatManager.disable(event.getPlayer().getUniqueId());
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
