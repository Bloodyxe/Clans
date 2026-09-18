package de.customclans.clans;

import org.bukkit.plugin.java.JavaPlugin;

public class CustomClans extends JavaPlugin {

    private ClanManager clanManager;
    private EconomyHook economyHook;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        economyHook = new EconomyHook();
        if (!economyHook.setup()) {
            getLogger().warning("Kein Vault-Economy-Provider gefunden (z.B. EssentialsX). "
                    + "/clan bank deposit und /clan bank withdraw funktionieren erst, sobald "
                    + "Vault + ein Economy-Plugin installiert sind.");
        } else {
            getLogger().info("Vault-Economy erfolgreich verbunden.");
        }

        clanManager = new ClanManager(getDataFolder(), getLogger());
        clanManager.loadAll();

        ClanCommand clanCommand = new ClanCommand(this, clanManager, economyHook);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);

        SetClanHomeCommand setHomeCommand = new SetClanHomeCommand(clanManager);
        getCommand("setclanhome").setExecutor(setHomeCommand);

        getLogger().info("CustomClans wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (clanManager != null) {
            for (Clan clan : clanManager.getAllClans().values()) {
                clanManager.save(clan);
            }
        }
        getLogger().info("CustomClans wurde deaktiviert, alle Clans gespeichert.");
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }
}
