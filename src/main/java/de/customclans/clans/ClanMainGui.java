package de.customclans.clans;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Builds the 6-row (54 slot) clan main menu opened by /clan info on your own clan.
 *
 * Layout:
 *   slot 14 - clock showing when the clan was created
 *   slot 21 - member list button (opens ClanInfoGui)
 *   slot 25 - clan bank button (opens ClanBankGui)
 *   slot 32 - Clan PVP on/off toggle (leader only)
 *   everything else - black stained glass pane filler
 */
public final class ClanMainGui {

    public static final int SIZE = 54;
    public static final int CLOCK_SLOT = 14;
    public static final int MEMBERS_SLOT = 21;
    public static final int BANK_SLOT = 25;
    public static final int PVP_SLOT = 32;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private ClanMainGui() {
    }

    public static Inventory build(Clan clan, EconomyHook economyHook) {
        ClanMainHolder holder = new ClanMainHolder(clan.getName());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, color("&8Clan: " + clan.getName()));
        holder.setInventory(inventory);

        ItemStack filler = fillerPane();
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(CLOCK_SLOT, clockItem(clan));
        inventory.setItem(MEMBERS_SLOT, membersItem(clan));
        inventory.setItem(BANK_SLOT, bankItem(clan, economyHook));
        inventory.setItem(PVP_SLOT, pvpItem(clan));

        return inventory;
    }

    private static ItemStack fillerPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack clockItem(Clan clan) {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&6Clan Founded"));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7" + DATE_FORMAT.format(new Date(clan.getCreatedAt()))));
            meta.setLore(lore);
            clock.setItemMeta(meta);
        }
        return clock;
    }

    private static ItemStack membersItem(Clan clan) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwner());
            meta.setOwningPlayer(owner);
            meta.setDisplayName(color("&bMembers"));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7" + clan.getMembers().size() + "/" + Clan.MAX_MEMBERS + " members"));
            lore.add(color("&eClick to view the member list"));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack bankItem(Clan clan, EconomyHook economyHook) {
        ItemStack ingot = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = ingot.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&6Clan Bank"));
            List<String> lore = new ArrayList<>();
            String balance = economyHook.isReady()
                    ? economyHook.format(clan.getBankBalance())
                    : String.valueOf(clan.getBankBalance());
            lore.add(color("&7Balance: &a" + balance));
            lore.add(color("&eClick to deposit or withdraw"));
            meta.setLore(lore);
            ingot.setItemMeta(meta);
        }
        return ingot;
    }

    private static ItemStack pvpItem(Clan clan) {
        boolean enabled = clan.isPvpEnabled();
        ItemStack sword = new ItemStack(enabled ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&cClan PVP: " + (enabled ? "&aON" : "&cOFF")));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Allow clan members to damage each other."));
            lore.add(color("&eClick to toggle &7(leader only)"));
            meta.setLore(lore);
            sword.setItemMeta(meta);
        }
        return sword;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
