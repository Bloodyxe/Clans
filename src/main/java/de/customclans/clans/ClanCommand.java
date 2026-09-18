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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final CustomClans plugin;
    private final ClanManager clanManager;
    private final EconomyHook economyHook;
    private final ClanChatManager clanChatManager;

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "delete", "bank", "promote", "demote", "kick", "home", "info", "transfer", "chat"
    );

    public ClanCommand(CustomClans plugin, ClanManager clanManager, EconomyHook economyHook, ClanChatManager clanChatManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.economyHook = economyHook;
        this.clanChatManager = clanChatManager;
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
            msg(player, "&cNutzung: /clan create <name>");
            return true;
        }
        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            msg(player, "&cDu bist bereits in einem Clan.");
            return true;
        }
        String name = args[1];
        if (name.length() < 3 || name.length() > 16) {
            msg(player, "&cDer Clan-Name muss zwischen 3 und 16 Zeichen lang sein.");
            return true;
        }
        if (clanManager.clanExists(name)) {
            msg(player, "&cEin Clan mit diesem Namen existiert bereits.");
            return true;
        }
        clanManager.createClan(name, player.getUniqueId());
        msg(player, "&aClan &f" + name + " &awurde erstellt! Du bist der Anführer.");
        return true;
    }

    // ---------------------------------------------------------------- delete

    private boolean handleDelete(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cDu bist in keinem Clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
            msg(player, "&cNur der Anführer kann den Clan auflösen.");
            return true;
        }
        for (UUID member : List.copyOf(clan.getMembers().keySet())) {
            clanManager.unregisterMembership(member);
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                msg(online, "&cDein Clan &f" + clan.getName() + " &cwurde aufgelöst.");
            }
        }
        clanManager.delete(clan);
        msg(player, "&aClan &f" + clan.getName() + " &awurde aufgelöst.");
        return true;
    }

    // ---------------------------------------------------------------- bank

    private boolean handleBank(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cDu bist in keinem Clan.");
            return true;
        }
        if (!economyHook.isReady()) {
            msg(player, "&cKeine Economy verbunden (Vault/EssentialsX fehlt). Bank-Befehle sind deaktiviert.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cNutzung: /clan bank <deposit|withdraw|balance> [betrag]");
            return true;
        }

        String action = args[1].toLowerCase();
        if (action.equals("balance")) {
            msg(player, "&7Clan-Bank: &a" + economyHook.format(clan.getBankBalance()));
            return true;
        }

        if (args.length < 3) {
            msg(player, "&cNutzung: /clan bank " + action + " <betrag>");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            msg(player, "&cUngültiger Betrag.");
            return true;
        }
        if (amount <= 0) {
            msg(player, "&cDer Betrag muss größer als 0 sein.");
            return true;
        }

        if (action.equals("deposit")) {
            if (!economyHook.has(player, amount)) {
                msg(player, "&cDu hast nicht genug Geld.");
                return true;
            }
            economyHook.withdrawPlayer(player, amount);
            clan.deposit(amount);
            clanManager.save(clan);
            msg(player, "&aDu hast &f" + economyHook.format(amount) + " &ain die Clan-Bank eingezahlt.");
        } else if (action.equals("withdraw")) {
            Rank rank = clan.getRank(player.getUniqueId());
            if (rank != Rank.LEADER && rank != Rank.OFFICER) {
                msg(player, "&cNur Anführer und Offiziere dürfen Geld abheben.");
                return true;
            }
            if (!clan.withdraw(amount)) {
                msg(player, "&cDie Clan-Bank hat nicht genug Guthaben.");
                return true;
            }
            economyHook.depositPlayer(player, amount);
            clanManager.save(clan);
            msg(player, "&aDu hast &f" + economyHook.format(amount) + " &aaus der Clan-Bank abgehoben.");
        } else {
            msg(player, "&cNutzung: /clan bank <deposit|withdraw|balance> [betrag]");
        }
        return true;
    }

    // ---------------------------------------------------------------- promote / demote

    private boolean handlePromote(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cDu bist in keinem Clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
            msg(player, "&cNur der Anführer kann Mitglieder befördern.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cNutzung: /clan promote <spieler>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cDieser Spieler ist nicht in deinem Clan.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            msg(player, "&cDu kannst dich nicht selbst befördern.");
            return true;
        }
        Rank current = clan.getRank(target.getUniqueId());
        if (current == Rank.OFFICER) {
            msg(player, "&cDieser Spieler ist bereits Offizier.");
            return true;
        }
        clan.addMember(target.getUniqueId(), Rank.OFFICER);
        clanManager.save(clan);
        msg(player, "&a" + target.getName() + " wurde zum Offizier befördert.");
        notifyIfOnline(target, "&aDu wurdest im Clan " + clan.getName() + " zum Offizier befördert.");
        return true;
    }

    private boolean handleDemote(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cDu bist in keinem Clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
            msg(player, "&cNur der Anführer kann Mitglieder degradieren.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cNutzung: /clan demote <spieler>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cDieser Spieler ist nicht in deinem Clan.");
            return true;
        }
        Rank current = clan.getRank(target.getUniqueId());
        if (current != Rank.OFFICER) {
            msg(player, "&cDieser Spieler ist kein Offizier.");
            return true;
        }
        clan.addMember(target.getUniqueId(), Rank.MEMBER);
        clanManager.save(clan);
        msg(player, "&a" + target.getName() + " wurde zum normalen Mitglied degradiert.");
        notifyIfOnline(target, "&cDu wurdest im Clan " + clan.getName() + " degradiert.");
        return true;
    }

    // ---------------------------------------------------------------- kick

    private boolean handleKick(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cDu bist in keinem Clan.");
            return true;
        }
        Rank myRank = clan.getRank(player.getUniqueId());
        if (myRank != Rank.LEADER && myRank != Rank.OFFICER) {
            msg(player, "&cNur Anführer und Offiziere dürfen Mitglieder kicken.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cNutzung: /clan kick <spieler>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cDieser Spieler ist nicht in deinem Clan.");
            return true;
        }
        if (target.getUniqueId().equals(clan.getOwner())) {
            msg(player, "&cDer Anführer kann nicht gekickt werden.");
            return true;
        }
        Rank targetRank = clan.getRank(target.getUniqueId());
        if (myRank == Rank.OFFICER && targetRank == Rank.OFFICER) {
            msg(player, "&cOffiziere können keine anderen Offiziere kicken.");
            return true;
        }
        clan.removeMember(target.getUniqueId());
        clanManager.unregisterMembership(target.getUniqueId());
        clanManager.save(clan);
        msg(player, "&a" + target.getName() + " wurde aus dem Clan geworfen.");
        notifyIfOnline(target, "&cDu wurdest aus dem Clan " + clan.getName() + " geworfen.");
        return true;
    }

    // ---------------------------------------------------------------- home

    private boolean handleHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cDu bist in keinem Clan.");
            return true;
        }
        if (!clan.hasHome()) {
            msg(player, "&cDein Clan hat noch kein Home. Ein Offizier/Anführer kann es mit /setclanhome setzen.");
            return true;
        }
        player.teleport(clan.getHome());
        msg(player, "&aDu wurdest zum Clan-Home teleportiert.");
        return true;
    }

    // ---------------------------------------------------------------- info

    private boolean handleInfo(CommandSender sender, String[] args) {
        Clan clan;
        if (args.length >= 2) {
            clan = clanManager.getClanByName(args[1]);
            if (clan == null) {
                sender.sendMessage(color("&cEs gibt keinen Clan mit dem Namen '" + args[1] + "'."));
                return true;
            }
        } else {
            Player player = requirePlayer(sender);
            if (player == null) return true;
            clan = clanManager.getClanByPlayer(player.getUniqueId());
            if (clan == null) {
                msg(player, "&cDu bist in keinem Clan. Nutze /clan info <name> für einen anderen Clan.");
                return true;
            }
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwner());
        sender.sendMessage(color("&6=== Clan: " + clan.getName() + " ==="));
        sender.sendMessage(color("&7Anführer: &f" + owner.getName()));
        sender.sendMessage(color("&7Mitglieder: &f" + clan.getMembers().size()));
        sender.sendMessage(color("&7Bank: &f" + economyHook.format(clan.getBankBalance())));

        String memberList = clan.getMembers().entrySet().stream()
                .map(e -> {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
                    String rankTag = switch (e.getValue()) {
                        case LEADER -> "&6[Anführer]";
                        case OFFICER -> "&e[Offizier]";
                        case MEMBER -> "&7[Mitglied]";
                    };
                    return rankTag + " &f" + p.getName();
                })
                .collect(Collectors.joining("&7, "));
        sender.sendMessage(color("&7" + memberList));
        return true;
    }

    // ---------------------------------------------------------------- transfer

    private boolean handleTransfer(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cDu bist in keinem Clan.");
            return true;
        }
        if (clan.getRank(player.getUniqueId()) != Rank.LEADER) {
            msg(player, "&cNur der Anführer kann die Führung übertragen.");
            return true;
        }
        if (args.length < 2) {
            msg(player, "&cNutzung: /clan transfer <spieler>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!clan.isMember(target.getUniqueId())) {
            msg(player, "&cDieser Spieler ist nicht in deinem Clan.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            msg(player, "&cDu bist bereits der Anführer.");
            return true;
        }
        clan.addMember(player.getUniqueId(), Rank.OFFICER);
        clan.addMember(target.getUniqueId(), Rank.LEADER);
        clan.setOwner(target.getUniqueId());
        clanManager.save(clan);
        msg(player, "&aDu hast die Führung des Clans an " + target.getName() + " übergeben.");
        notifyIfOnline(target, "&aDu bist jetzt Anführer des Clans " + clan.getName() + "!");
        return true;
    }

    // ---------------------------------------------------------------- chat

    private boolean handleChat(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            msg(player, "&cDu bist in keinem Clan.");
            return true;
        }

        boolean nowEnabled = clanChatManager.toggle(player.getUniqueId());
        if (nowEnabled) {
            msg(player, "&a[Clan-Chat] &7Aktiviert. Nachrichten gehen jetzt nur noch an deinen Clan.");
            msg(player, "&7Nutze &f/clan chat &7erneut zum Ausschalten.");
        } else {
            msg(player, "&7[Clan-Chat] &cDeaktiviert. Du schreibst jetzt wieder im normalen Chat.");
        }
        return true;
    }

    // ---------------------------------------------------------------- helpers

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
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
        sender.sendMessage(color("&6=== Clan-Befehle ==="));
        sender.sendMessage(color("&f/clan create <name> &7- Clan gründen"));
        sender.sendMessage(color("&f/clan delete &7- Clan auflösen"));
        sender.sendMessage(color("&f/clan bank <deposit|withdraw|balance> [betrag] &7- Clan-Bank"));
        sender.sendMessage(color("&f/clan promote <spieler> &7- zum Offizier befördern"));
        sender.sendMessage(color("&f/clan demote <spieler> &7- degradieren"));
        sender.sendMessage(color("&f/clan kick <spieler> &7- aus dem Clan werfen"));
        sender.sendMessage(color("&f/clan home &7- zum Clan-Home teleportieren"));
        sender.sendMessage(color("&f/clan info [name] &7- Clan-Infos anzeigen"));
        sender.sendMessage(color("&f/clan transfer <spieler> &7- Führung übertragen"));
        sender.sendMessage(color("&f/clan chat &7- Clan-Chat an-/ausschalten"));
        sender.sendMessage(color("&f/setclanhome [name] &7- Clan-Home setzen"));
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
        return result;
    }
}
