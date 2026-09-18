package de.customclans.clans;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads/saves every clan as its own YAML file under plugins/CustomClans/clans/<name>.yml
 * and keeps an in-memory index so lookups don't hit disk on every command.
 */
public class ClanManager {

    private final File clansFolder;
    private final Logger logger;

    /** normalized clan name (color codes stripped, lowercase) -> Clan */
    private final Map<String, Clan> clansByName = new HashMap<>();
    /** player UUID -> normalized clan name, for quick "which clan is this player in" lookups */
    private final Map<UUID, String> playerIndex = new HashMap<>();

    /**
     * Normalizes a clan name for uniqueness/lookup purposes: strips color codes (so &4WWL and
     * WWL are treated as the same name) and lowercases it.
     */
    public static String normalize(String name) {
        String translated = ChatColor.translateAlternateColorCodes('&', name);
        return ChatColor.stripColor(translated).toLowerCase();
    }

    public ClanManager(File dataFolder, Logger logger) {
        this.clansFolder = new File(dataFolder, "clans");
        this.logger = logger;
        if (!clansFolder.exists()) {
            clansFolder.mkdirs();
        }
    }

    public void loadAll() {
        clansByName.clear();
        playerIndex.clear();
        File[] files = clansFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                Clan clan = loadFromFile(file);
                if (clan != null) {
                    clansByName.put(normalize(clan.getName()), clan);
                    for (UUID member : clan.getMembers().keySet()) {
                        playerIndex.put(member, normalize(clan.getName()));
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not load clan file: " + file.getName(), e);
            }
        }
        logger.info("[CustomClans] Loaded " + clansByName.size() + " clan(s).");
    }

    private Clan loadFromFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String name = yaml.getString("name");
        String ownerStr = yaml.getString("owner");
        if (name == null || ownerStr == null) {
            return null;
        }
        UUID owner = UUID.fromString(ownerStr);
        Clan clan = new Clan(name, owner);
        clan.getMembers().clear();

        if (yaml.isConfigurationSection("members")) {
            for (String uuidStr : yaml.getConfigurationSection("members").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Rank rank = Rank.valueOf(yaml.getString("members." + uuidStr, "MEMBER"));
                    clan.addMember(uuid, rank);
                } catch (IllegalArgumentException ignored) {
                    // malformed entry, skip
                }
            }
        }
        if (!clan.isMember(owner)) {
            clan.addMember(owner, Rank.LEADER);
        }

        clan.setBankBalance(yaml.getDouble("bank", 0.0));
        clan.setPvpEnabled(yaml.getBoolean("pvp", false));
        long createdAt = yaml.getLong("created_at", 0L);
        if (createdAt > 0L) {
            clan.setCreatedAt(createdAt);
        }
        String colorStart = yaml.getString("color_start");
        String colorEnd = yaml.getString("color_end");
        if (colorStart != null && colorEnd != null) {
            clan.setColor(colorStart, colorEnd);
        }

        for (String uuidStr : yaml.getStringList("home_permissions")) {
            try {
                clan.getHomeAllowed().add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
                // malformed entry, skip
            }
        }

        if (yaml.isConfigurationSection("home")) {
            String worldName = yaml.getString("home.world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world != null) {
                double x = yaml.getDouble("home.x");
                double y = yaml.getDouble("home.y");
                double z = yaml.getDouble("home.z");
                float yaw = (float) yaml.getDouble("home.yaw");
                float pitch = (float) yaml.getDouble("home.pitch");
                clan.setHome(new Location(world, x, y, z, yaw, pitch));
            }
        }

        return clan;
    }

    public void save(Clan clan) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", clan.getName());
        yaml.set("owner", clan.getOwner().toString());
        yaml.set("bank", clan.getBankBalance());
        yaml.set("pvp", clan.isPvpEnabled());
        yaml.set("created_at", clan.getCreatedAt());
        if (clan.hasCustomColor()) {
            yaml.set("color_start", clan.getColorStart());
            yaml.set("color_end", clan.getColorEnd());
        }

        for (Map.Entry<UUID, Rank> entry : clan.getMembers().entrySet()) {
            yaml.set("members." + entry.getKey() + "", entry.getValue().name());
        }

        List<String> homePermissions = new ArrayList<>();
        for (UUID uuid : clan.getHomeAllowed()) {
            homePermissions.add(uuid.toString());
        }
        yaml.set("home_permissions", homePermissions);

        if (clan.hasHome()) {
            Location loc = clan.getHome();
            yaml.set("home.world", loc.getWorld().getName());
            yaml.set("home.x", loc.getX());
            yaml.set("home.y", loc.getY());
            yaml.set("home.z", loc.getZ());
            yaml.set("home.yaw", loc.getYaw());
            yaml.set("home.pitch", loc.getPitch());
        }

        File file = fileFor(clan.getName());
        try {
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save clan file: " + file.getName(), e);
        }
    }

    public void delete(Clan clan) {
        clansByName.remove(normalize(clan.getName()));
        for (UUID member : clan.getMembers().keySet()) {
            playerIndex.remove(member);
        }
        File file = fileFor(clan.getName());
        if (file.exists()) {
            file.delete();
        }
    }

    public void createClan(String name, UUID owner) {
        Clan clan = new Clan(name, owner);
        clansByName.put(normalize(name), clan);
        playerIndex.put(owner, normalize(name));
        save(clan);
    }

    public void registerMembership(Clan clan, UUID uuid) {
        playerIndex.put(uuid, normalize(clan.getName()));
    }

    public void unregisterMembership(UUID uuid) {
        playerIndex.remove(uuid);
    }

    public Clan getClanByName(String name) {
        return clansByName.get(normalize(name));
    }

    public Clan getClanByPlayer(UUID uuid) {
        String clanName = playerIndex.get(uuid);
        return clanName != null ? clansByName.get(clanName) : null;
    }

    /** Color-code independent: "WWL" and "&4WWL" are considered the same name. */
    public boolean clanExists(String name) {
        return clansByName.containsKey(normalize(name));
    }

    public Map<String, Clan> getAllClans() {
        return clansByName;
    }

    private File fileFor(String clanName) {
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', clanName));
        String safe = stripped.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return new File(clansFolder, safe.toLowerCase() + ".yml");
    }
}
