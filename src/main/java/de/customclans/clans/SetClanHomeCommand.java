package de.customclans.clans;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetClanHomeCommand implements CommandExecutor {

    private final ClanManager clanManager;

    public SetClanHomeCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cDu bist in keinem Clan.");
            return true;
        }
        Rank rank = clan.getRank(player.getUniqueId());
        if (rank != Rank.LEADER && rank != Rank.OFFICER) {
            msg(player, "&cNur Anführer und Offiziere dürfen das Clan-Home setzen.");
            return true;
        }

        clan.setHome(player.getLocation());
        clanManager.save(clan);
        msg(player, "&aDas Clan-Home wurde an deiner Position gesetzt.");
        return true;
    }

    private void msg(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
