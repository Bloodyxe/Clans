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
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        Rank rank = clan.getRank(player.getUniqueId());
        if (rank != Rank.LEADER) {
            msg(player, "&cOnly the clan leader can set the clan home.");
            return true;
        }
        if (args.length > 0) {
            msg(player, "&cNamed homes are disabled - each clan only has one home. Use /setclanhome without any extra text.");
            return true;
        }
        if (clan.hasHome()) {
            msg(player, "&cYour clan already has a home. Use /clan home delete first if you want to set a new one.");
            return true;
        }

        clan.setHome(player.getLocation());
        clanManager.save(clan);
        msg(player, "&aThe clan home has been set at your position.");
        return true;
    }

    private void msg(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
