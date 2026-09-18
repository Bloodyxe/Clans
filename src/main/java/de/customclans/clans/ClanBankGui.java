package de.customclans.clans;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the 27-slot clan bank GUI (opened from the main menu's bank button).
 *
 * Layout:
 *   slot 4  - balance display (top-center)
 *   slot 12 - deposit button (below-left of the balance)
 *   slot 14 - withdraw button (below-right of the balance)
 *   everything else - black stained glass pane filler
 */
public final class ClanBankGui {

    public static final int SIZE = 27;
    public static final int BALANCE_SLOT = 4;
    public static final int DEPOSIT_SLOT = 12;
    public static final int WITHDRAW_SLOT = 14;

    private ClanBankGui() {
    }

    public static Inventory build(Clan clan, EconomyHook economyHook) {
        ClanBankHolder holder = new ClanBankHolder(clan.getName());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, color("&8Clan Bank: " + clan.getDisplayName()));
        holder.setInventory(inventory);

        ItemStack filler = fillerPane();
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(BALANCE_SLOT, balanceItem(clan, economyHook));
        inventory.setItem(DEPOSIT_SLOT, depositItem());
        inventory.setItem(WITHDRAW_SLOT, withdrawItem());

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

    private static ItemStack balanceItem(Clan clan, EconomyHook economyHook) {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&6Clan Bank Balance"));
            List<String> lore = new ArrayList<>();
            String balance = economyHook.isReady()
                    ? economyHook.format(clan.getBankBalance())
                    : String.valueOf(clan.getBankBalance());
            lore.add(color("&a" + balance));
            meta.setLore(lore);
            chest.setItemMeta(meta);
        }
        return chest;
    }

    private static ItemStack depositItem() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&aDeposit"));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Click, then type an amount in chat"));
            lore.add(color("&7to pay money into the clan bank."));
            lore.add(color("&7You can use shorthand, e.g. &f100m &7= 100,000,000"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack withdrawItem() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&cWithdraw"));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Click, then type an amount in chat"));
            lore.add(color("&7to take money out of the clan bank."));
            lore.add(color("&7You can use shorthand, e.g. &f100m &7= 100,000,000"));
            lore.add(color("&7(Leader / Officer only)"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
