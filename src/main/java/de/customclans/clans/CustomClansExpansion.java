package de.customclans.clans;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes clan info to PlaceholderAPI so other plugins (e.g. TAB) can display it:
 *   %customclans_clan%  -> the player's clan name (colored), empty if in no clan
 *   %customclans_tag%   -> " [ClanName]" (colored, with a leading space), empty if in no clan
 *
 * Reference these in TAB's own config.yml (scoreboard lines / tablist-name-formatting /
 * nametag-formatting) to show the clan on the scoreboard and above the player's head.
 */
public class CustomClansExpansion extends PlaceholderExpansion {

    private final CustomClans plugin;
    private final ClanManager clanManager;

    public CustomClansExpansion(CustomClans plugin, ClanManager clanManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "customclans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "brigantdan246";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            return "";
        }

        if (params.equalsIgnoreCase("clan")) {
            return color(clan.getName());
        }
        if (params.equalsIgnoreCase("tag")) {
            return color(" [" + clan.getName() + "]");
        }
        return null;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
