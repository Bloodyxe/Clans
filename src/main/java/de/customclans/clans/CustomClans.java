package de.customclans.clans;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomClans extends JavaPlugin {

    private ClanManager clanManager;
    private EconomyHook economyHook;
    private ClanChatManager clanChatManager;
    private InviteManager inviteManager;
    private BankActionManager bankActionManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        economyHook = new EconomyHook();
        if (!economyHook.setup()) {
            getLogger().warning("No Vault economy provider found (e.g. EssentialsX). "
                    + "/clan bank deposit and /clan bank withdraw will be disabled until "
                    + "Vault and an economy plugin are installed.");
        } else {
            getLogger().info("Vault economy connected successfully.");
        }

        clanManager = new ClanManager(getDataFolder(), getLogger());
        clanManager.loadAll();

        clanChatManager = new ClanChatManager();
        inviteManager = new InviteManager();
        bankActionManager = new BankActionManager();

        getServer().getPluginManager().registerEvents(
                new ClanChatListener(clanManager, clanChatManager, economyHook, bankActionManager), this);
        getServer().getPluginManager().registerEvents(
                new ClanGuiListener(clanManager, economyHook, bankActionManager), this);
        getServer().getPluginManager().registerEvents(
                new ClanPvpListener(clanManager), this);

        ClanCommand clanCommand = new ClanCommand(this, clanManager, economyHook, clanChatManager, inviteManager);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);

        SetClanHomeCommand setHomeCommand = new SetClanHomeCommand(clanManager);
        getCommand("setclanhome").setExecutor(setHomeCommand);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CustomClansExpansion(this, clanManager).register();
            getLogger().info("PlaceholderAPI found - %customclans_clan% and %customclans_tag% "
                    + "placeholders are now available (e.g. for the TAB plugin).");
        } else {
            getLogger().info("PlaceholderAPI not found - clan placeholders for TAB etc. are disabled.");
        }

        getLogger().info("CustomClans has been enabled.");
    }

    @Override
    public void onDisable() {
        if (clanManager != null) {
            for (Clan clan : clanManager.getAllClans().values()) {
                clanManager.save(clan);
            }
        }
        getLogger().info("CustomClans has been disabled, all clans saved.");
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }
}
