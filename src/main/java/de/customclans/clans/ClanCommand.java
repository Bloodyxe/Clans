package de.customclans.clans;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final CustomClans plugin;
    private final ClanManager clanManager;
    private final EconomyHook economyHook;
    private final ClanChatManager clanChatManager;
    private final InviteManager inviteManager;

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "delete", "bank", "promote", "demote", "kick", "home", "info", "transfer", "chat", "invite", "permission", "leave"
    );

    public ClanCommand(CustomClans plugin, ClanManager clanManager, EconomyHook economyHook,
                        ClanChatManager clanChatManager, InviteManager inviteManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.economyHook = economyHook;
        this.clanChatManager = clanChatManager;
        this.inviteManager = inviteManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender);
            case "bank":
                return handleBank(sender, args);
            case "promote":
                return handlePromote(sender, args);
            case "demote":
                return handleDemote(sender, args);
            case "kick":
                return handleKick(sender, args);
            case "home":
                return handleHome(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "transfer":
                return handleTransfer(sender, args);
            case "chat":
                return handleChat(sender);
            case "invite":
                return handleInvite(sender, args);
            case "permission":
                return handlePermission(sender, args);
            case "leave":
                return handleLeave(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }

    // ---------------------------------------------------------------- create

    private boolean handleCreate(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (args.length < 2) {
            msg(player, "&cUsage: /clan create <name>");
            return true;
        }
        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            msg(player, "&cYou are already in a clan.");
            return true;
        }
        String name = ChatColor.translateAlternateColorCodes('&', args[1]);
        String visible = ChatColor.stripColor(name);
        if (visible.length() < 3 || visible.length() > 16) {
            msg(player, "&cThe clan name must be between 3 and 16 characters long.");
            return true;
        }
        if (clanManager.clanExists(name)) {
            msg(player, "&cA clan with that name already exists (color codes don't count as a different name).");
            return true;
        }
        clanManager.createClan(name, player.getUniqueId());
        msg(player, "&aClan &f" + name + " &awas created! You are the leader.");
        return true;
    }

    // ---------------------------------------------------------------- delete

    private boolean handleDelete(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
            msg(player, "&cOnly the leader can disband the clan.");
            return true;
        }
        for (UUID member : List.copyOf(clan.getMembers().keySet())) {
            clanManager.unregisterMembership(member);
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                msg(online, "&cYour clan &f" + clan.getName() + " &chas been disbanded.");
            }
        }
        clanManager.delete(clan);
        msg(player, "&aClan &f" + clan.getName() + " &ahas been disbanded.");
        return true;
    }

    // ---------------------------------------------------------------- bank

    private boolean handleBank(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        if (!economyHook.isReady()) {
            msg(player, "&cNo economy plugin connected (Vault/EssentialsX missing). Bank commands are disabled.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cUsage: /clan bank <deposit|withdraw|balance> [amount]");
            return true;
        }

        String action = args[1].toLowerCase();
        if (action.equals("balance")) {
            msg(player, "&7Clan bank: &a" + economyHook.format(clan.getBankBalance()));
            return true;
        }

        if (args.length < 3) {
            msg(player, "&cUsage: /clan bank " + action + " <amount>");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            msg(player, "&cInvalid amount.");
            return true;
        }
        if (amount <= 0) {
            msg(player, "&cThe amount must be greater than 0.");
            return true;
        }

        if (action.equals("deposit")) {
            if (!economyHook.has(player, amount)) {
                msg(player, "&cYou don't have enough money.");
                return true;
            }
            economyHook.withdrawPlayer(player, amount);
            clan.deposit(amount);
            clanManager.save(clan);
            msg(player, "&aYou deposited &f" + economyHook.format(amount) + " &ainto the clan bank.");
        } else if (action.equals("withdraw")) {
            Rank rank = clan.getRank(player.getUniqueId());
            if (rank != Rank.LEADER && rank != Rank.OFFICER) {
                msg(player, "&cOnly leaders and officers can withdraw money.");
                return true;
            }
            if (!clan.withdraw(amount)) {
                msg(player, "&cThe clan bank doesn't have enough funds.");
                return true;
            }
            economyHook.depositPlayer(player, amount);
            clanManager.save(clan);
            msg(player, "&aYou withdrew &f" + economyHook.format(amount) + " &afrom the clan bank.");
        } else {
            msg(player, "&cUsage: /clan bank <deposit|withdraw|balance> [amount]");
        }
        return true;
    }

    // ---------------------------------------------------------------- promote / demote

    private boolean handlePromote(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
            msg(player, "&cOnly the leader can promote members.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cUsage: /clan promote <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cThat player is not in your clan.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            msg(player, "&cYou can't promote yourself.");
            return true;
        }
        Rank current = clan.getRank(target.getUniqueId());
        if (current == Rank.OFFICER) {
            msg(player, "&cThat player is already an officer.");
            return true;
        }
        clan.addMember(target.getUniqueId(), Rank.OFFICER);
        clanManager.save(clan);
        msg(player, "&a" + target.getName() + " was promoted to officer.");
        notifyIfOnline(target, "&aYou were promoted to officer in clan " + clan.getName() + ".");
        return true;
    }

    private boolean handleDemote(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
            msg(player, "&cOnly the leader can demote members.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cUsage: /clan demote <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cThat player is not in your clan.");
            return true;
        }
        Rank current = clan.getRank(target.getUniqueId());
        if (current != Rank.OFFICER) {
            msg(player, "&cThat player isn't an officer.");
            return true;
        }
        clan.addMember(target.getUniqueId(), Rank.MEMBER);
        clanManager.save(clan);
        msg(player, "&a" + target.getName() + " was demoted to member.");
        notifyIfOnline(target, "&cYou were demoted in clan " + clan.getName() + ".");
        return true;
    }

    // ---------------------------------------------------------------- kick

    private boolean handleKick(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        Rank myRank = clan.getRank(player.getUniqueId());
        if (myRank != Rank.LEADER && myRank != Rank.OFFICER) {
            msg(player, "&cOnly leaders and officers can kick members.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cUsage: /clan kick <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cThat player is not in your clan.");
            return true;
        }
        if (target.getUniqueId().equals(clan.getOwner())) {
            msg(player, "&cThe leader can't be kicked.");
            return true;
        }
        Rank targetRank = clan.getRank(target.getUniqueId());
        if (myRank == Rank.OFFICER && targetRank == Rank.OFFICER) {
            msg(player, "&cOfficers can't kick other officers.");
            return true;
        }
        clan.removeMember(target.getUniqueId());
        clanManager.unregisterMembership(target.getUniqueId());
        clanManager.save(clan);
        msg(player, "&a" + target.getName() + " was kicked from the clan.");
        notifyIfOnline(target, "&cYou were kicked from clan " + clan.getName() + ".");
        return true;
    }

    // ---------------------------------------------------------------- home

    private boolean handleHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("delete")) {
            if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
                msg(player, "&cOnly the clan leader can delete the clan home.");
                return true;
            }
            if (!clan.hasHome()) {
                msg(player, "&cYour clan doesn't have a home set.");
                return true;
            }
            clan.setHome(null);
            clanManager.save(clan);
            msg(player, "&aThe clan home has been deleted.");
            return true;
        }

        if (args.length >= 2) {
            msg(player, "&cNamed homes are disabled - each clan only has one home. Use /clan home without any extra text.");
            return true;
        }

        if (!clan.hasHome()) {
            msg(player, "&cYour clan doesn't have a home yet. The leader can set one with /setclanhome.");
            return true;
        }
        if (!clan.hasHomePermission(player.getUniqueId())) {
            msg(player, "&cYou don't have permission to use the clan home. Ask your leader for access via /clan permission home.");
            return true;
        }
        player.teleport(clan.getHome());
        msg(player, "&aTeleported to the clan home.");
        return true;
    }

    // ---------------------------------------------------------------- info

    private boolean handleInfo(CommandSender sender, String[] args) {
        // Looking up ANOTHER clan by name: only the owner and member count, nothing else.
        if (args.length >= 2) {
            Clan clan = clanManager.getClanByName(args[1]);
            if (clan == null) {
                sender.sendMessage(color("&cThere is no clan named '" + args[1] + "'."));
                return true;
            }
            OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwner());
            sender.sendMessage(color("&6=== Clan: " + clan.getName() + " ==="));
            sender.sendMessage(color("&7Leader: &f" + owner.getName()));
            sender.sendMessage(color("&7Members: &f" + clan.getMembers().size() + "/" + Clan.MAX_MEMBERS));
            return true;
        }

        // Your own clan: opens the detailed member GUI (status, rank, home access).
        Player player = requirePlayer(sender);
        if (player == null) return true;
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan. Use /clan info <name> to look up another clan.");
            return true;
        }
        player.openInventory(ClanMainGui.build(clan, economyHook));
        return true;
    }

    // ---------------------------------------------------------------- leave

    private boolean handleLeave(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) == Rank.LEADER) {
            msg(player, "&cAs the leader you can't leave the clan. Use /clan transfer <player> "
                    + "to hand over leadership first, or /clan delete to disband the clan.");
            return true;
        }
        clan.removeMember(player.getUniqueId());
        clanManager.unregisterMembership(player.getUniqueId());
        clanManager.save(clan);
        msg(player, "&7You left clan " + clan.getName() + ".");
        for (UUID memberId : clan.getMembers().keySet()) {
            Player online = Bukkit.getPlayer(memberId);
            if (online != null) {
                msg(online, "&7" + player.getName() + " left the clan.");
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- transfer

    private boolean handleTransfer(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
            msg(player, "&cOnly the leader can transfer leadership.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cUsage: /clan transfer <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cThat player is not in your clan.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            msg(player, "&cYou are already the leader.");
            return true;
        }
        clan.addMember(player.getUniqueId(), Rank.OFFICER);
        clan.addMember(target.getUniqueId(), Rank.LEADER);
        clan.setOwner(target.getUniqueId());
        clanManager.save(clan);
        msg(player, "&aYou transferred clan leadership to " + target.getName() + ".");
        notifyIfOnline(target, "&aYou are now the leader of clan " + clan.getName() + "!");
        return true;
    }

    // ---------------------------------------------------------------- chat

    private boolean handleChat(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }

        boolean nowEnabled = clanChatManager.toggle(player.getUniqueId());
        if (nowEnabled) {
            msg(player, "&a[Clan Chat] &7Enabled. Your messages now only go to your clan.");
            msg(player, "&7Use &f/clan chat &7again to turn it off.");
        } else {
            msg(player, "&7[Clan Chat] &cDisabled. You're back in normal chat.");
        }
        return true;
    }

    // ---------------------------------------------------------------- invite

    private boolean handleInvite(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (args.length >= 2 && args[1].equalsIgnoreCase("accept")) {
            return handleInviteAccept(player);
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("decline")) {
            return handleInviteDecline(player);
        }

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        Rank myRank = clan.getRank(player.getUniqueId());
        if (myRank != Rank.LEADER && myRank != Rank.OFFICER) {
            msg(player, "&cOnly leaders and officers can invite players.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cUsage: /clan invite <player>");
            return true;
        }
        if (clan.isFull()) {
            msg(player, "&cYour clan is full (" + Clan.MAX_MEMBERS + "/" + Clan.MAX_MEMBERS + ").");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg(player, "&cThat player isn't online.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            msg(player, "&cYou can't invite yourself.");
            return true;
        }
        if (clanManager.getClanByPlayer(target.getUniqueId()) != null) {
            msg(player, "&cThat player is already in a clan.");
            return true;
        }

        inviteManager.invite(target.getUniqueId(), clan.getName());
        msg(player, "&aInvite sent to " + target.getName() + ".");
        msg(target, "&a" + player.getName() + " invited you to join clan " + clan.getName() + "!");
        msg(target, "&7Use &f/clan invite accept &7or &f/clan invite decline&7.");
        return true;
    }

    private boolean handleInviteAccept(Player player) {
        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            msg(player, "&cYou are already in a clan.");
            return true;
        }
        String clanName = inviteManager.getPendingClan(player.getUniqueId());
        if (clanName == null) {
            msg(player, "&cYou don't have a pending clan invite.");
            return true;
        }
        Clan clan = clanManager.getClanByName(clanName);
        inviteManager.clear(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cThat clan no longer exists.");
            return true;
        }
        if (clan.isFull()) {
            msg(player, "&cThat clan is now full (" + Clan.MAX_MEMBERS + "/" + Clan.MAX_MEMBERS + ").");
            return true;
        }
        clan.addMember(player.getUniqueId(), Rank.MEMBER);
        clanManager.registerMembership(clan, player.getUniqueId());
        clanManager.save(clan);
        msg(player, "&aYou joined clan " + clan.getName() + "!");
        for (UUID memberId : clan.getMembers().keySet()) {
            if (memberId.equals(player.getUniqueId())) continue;
            Player online = Bukkit.getPlayer(memberId);
            if (online != null) {
                msg(online, "&a" + player.getName() + " joined the clan!");
            }
        }
        return true;
    }

    private boolean handleInviteDecline(Player player) {
        if (!inviteManager.hasPendingInvite(player.getUniqueId())) {
            msg(player, "&cYou don't have a pending clan invite.");
            return true;
        }
        inviteManager.clear(player.getUniqueId());
        msg(player, "&7You declined the clan invite.");
        return true;
    }

    // ---------------------------------------------------------------- permission

    private boolean handlePermission(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cYou are not in a clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
            msg(player, "&cOnly the clan leader can manage permissions.");
            return true;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("home")) {
            msg(player, "&cUsage: /clan permission home <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cThat player is not in your clan.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            msg(player, "&cThe leader always has home access.");
            return true;
        }

        boolean nowAllowed = clan.toggleHomePermission(target.getUniqueId());
        clanManager.save(clan);
        if (nowAllowed) {
            msg(player, "&a" + target.getName() + " can now use /clan home.");
            notifyIfOnline(target, "&aYou were granted access to /clan home in clan " + clan.getName() + ".");
        } else {
            msg(player, "&c" + target.getName() + " can no longer use /clan home.");
            notifyIfOnline(target, "&cYour access to /clan home in clan " + clan.getName() + " was revoked.");
        }
        return true;
    }

    // ---------------------------------------------------------------- helpers

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return null;
        }
        return (Player) sender;
    }

    private void notifyIfOnline(OfflinePlayer target, String message) {
        Player online = target.getPlayer();
        if (online != null) {
            msg(online, message);
        }
    }

    private void msg(Player player, String message) {
        player.sendMessage(color(message));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(color("&6=== Clan Commands ==="));
        sender.sendMessage(color("&f/clan create <name> &7- Found a clan (max " + Clan.MAX_MEMBERS + " members)"));
        sender.sendMessage(color("&f/clan delete &7- Disband your clan"));
        sender.sendMessage(color("&f/clan bank <deposit|withdraw|balance> [amount] &7- Clan bank"));
        sender.sendMessage(color("&f/clan promote <player> &7- Promote to officer"));
        sender.sendMessage(color("&f/clan demote <player> &7- Demote to member"));
        sender.sendMessage(color("&f/clan kick <player> &7- Kick from the clan"));
        sender.sendMessage(color("&f/clan home &7- Teleport to the clan home (needs access)"));
        sender.sendMessage(color("&f/clan home delete &7- Delete the clan home (leader only)"));
        sender.sendMessage(color("&f/clan permission home <player> &7- Grant/revoke home access (leader only)"));
        sender.sendMessage(color("&f/clan info &7- Open your clan's member list (GUI)"));
        sender.sendMessage(color("&f/clan info <name> &7- Show another clan's leader & member count"));
        sender.sendMessage(color("&f/clan transfer <player> &7- Transfer leadership"));
        sender.sendMessage(color("&f/clan chat &7- Toggle clan chat"));
        sender.sendMessage(color("&f/clan invite <player> &7- Invite a player"));
        sender.sendMessage(color("&f/clan invite accept|decline &7- Respond to an invite"));
        sender.sendMessage(color("&f/clan leave &7- Leave your clan (not for the leader)"));
        sender.sendMessage(color("&f/setclanhome &7- Set the clan home at your position"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    result.add(sub);
                }
            }
            return result;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("bank")) {
                for (String opt : List.of("deposit", "withdraw", "balance")) {
                    if (opt.startsWith(args[1].toLowerCase())) {
                        result.add(opt);
                    }
                }
                return result;
            }
            if (sub.equals("home")) {
                if ("delete".startsWith(args[1].toLowerCase())) {
                    result.add("delete");
                }
                return result;
            }
            if (sub.equals("invite")) {
                for (String opt : List.of("accept", "decline")) {
                    if (opt.startsWith(args[1].toLowerCase())) {
                        result.add(opt);
                    }
                }
                String partial = args[1].toLowerCase();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().toLowerCase().startsWith(partial)) {
                        result.add(online.getName());
                    }
                }
                return result;
            }
            if (sub.equals("permission")) {
                if ("home".startsWith(args[1].toLowerCase())) {
                    result.add("home");
                }
                return result;
            }
            if (sub.equals("promote") || sub.equals("demote") || sub.equals("kick") || sub.equals("transfer")) {
                if (!(sender instanceof Player player)) {
                    return result;
                }
                Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
                if (clan == null) {
                    return result;
                }
                String partial = args[1].toLowerCase();
                for (UUID member : clan.getMembers().keySet()) {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(member);
                    String pname = p.getName();
                    if (pname != null && pname.toLowerCase().startsWith(partial)) {
                        result.add(pname);
                    }
                }
                return result;
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("permission") && args[1].equalsIgnoreCase("home")) {
                if (!(sender instanceof Player player)) {
                    return result;
                }
                Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
                if (clan == null) {
                    return result;
                }
                String partial = args[2].toLowerCase();
                for (UUID member : clan.getMembers().keySet()) {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(member);
                    String pname = p.getName();
                    if (pname != null && pname.toLowerCase().startsWith(partial)) {
                        result.add(pname);
                    }
                }
                return result;
            }
        }
        return result;
    }
}
