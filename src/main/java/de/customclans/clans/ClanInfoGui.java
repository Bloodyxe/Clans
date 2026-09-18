package de.customclans.clans;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the clickable member-list GUI shown by /clan info (own clan, no arguments).
 * Each member is a player head showing their rank, online status and /clan home access;
 * the leader can click a non-leader head to toggle their home access (see ClanGuiListener).
 */
public final class ClanInfoGui {

    private ClanInfoGui() {
    }

    /** Extra fully-empty rows added below the last member, so the menu doesn't feel cramped. */
    private static final int EXTRA_EMPTY_ROWS = 2;
    private static final int MAX_ROWS = 6;

    public static Inventory build(Clan clan) {
        int neededRows = Math.max(1, (int) Math.ceil(clan.getMembers().size() / 9.0));
        int rows = Math.min(MAX_ROWS, neededRows + EXTRA_EMPTY_ROWS);
        int size = rows * 9;

        Map<Integer, UUID> slotMembers = new HashMap<>();
        ClanInfoHolder holder = new ClanInfoHolder(clan.getName(), slotMembers);
        Inventory inventory = Bukkit.createInventory(holder, size,
                color("&8Clan: " + clan.getDisplayName() + " &7(" + clan.getMembers().size() + "/" + Clan.MAX_MEMBERS + ")"));
        holder.setInventory(inventory);

        List<Map.Entry<UUID, Rank>> sortedMembers = new ArrayList<>(clan.getMembers().entrySet());
        // Leader first, then Officers, then Members.
        sortedMembers.sort(Comparator.comparingInt(entry -> switch (entry.getValue()) {
            case LEADER -> 0;
            case OFFICER -> 1;
            case MEMBER -> 2;
        }));

        int slot = 0;
        for (Map.Entry<UUID, Rank> entry : sortedMembers) {
            UUID memberId = entry.getKey();
            Rank rank = entry.getValue();
            OfflinePlayer offline = Bukkit.getOfflinePlayer(memberId);
            boolean online = offline.isOnline();
            boolean homeAccess = clan.hasHomePermission(memberId);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(offline);

                String rankTag = switch (rank) {
                    case LEADER -> "&6[Leader]";
                    case OFFICER -> "&e[Officer]";
                    case MEMBER -> "&7[Member]";
                };
                meta.setDisplayName(color(rankTag + " &f" + offline.getName()));

                List<String> lore = new ArrayList<>();
                lore.add(color("&7Status: " + (online ? "&aOnline" : "&cOffline")));
                lore.add(color("&7Rank: &f" + rank.name()));
                lore.add(color("&7Home access: " + (homeAccess ? "&aYes" : "&cNo")));
                if (rank != Rank.LEADER) {
                    lore.add(color("&eClick to toggle home access &7(leader only)"));
                }
                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            inventory.setItem(slot, head);
            slotMembers.put(slot, memberId);
            slot++;
        }

        int backSlot = size - 1;
        holder.setBackSlot(backSlot);
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(color("&cBack"));
            backMeta.setLore(List.of(color("&7Return to the clan menu")));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(backSlot, back);

        return inventory;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
