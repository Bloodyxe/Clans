package de.customclans.clans;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory representation of a single clan. Persistence is handled by {@link ClanManager}.
 */
public class Clan {

    /** Maximum number of members (including the leader) a single clan can have. */
    public static final int MAX_MEMBERS = 15;

    private String name;
    private UUID owner;
    /** UUID -> Rank (LEADER/OFFICER/MEMBER). The owner is always LEADER. */
    private final Map<UUID, Rank> members = new LinkedHashMap<>();
    /** Exactly one home per clan, independent of every other clan's home. */
    private Location home;
    /** Non-leader members explicitly granted /clan home access. The leader always has access. */
    private final Set<UUID> homeAllowed = new HashSet<>();
    private double bankBalance;
    /** Epoch millis when the clan was founded, shown in the main GUI's clock item. */
    private long createdAt;
    /** Whether members of this clan can damage each other. Leader-only toggle. */
    private boolean pvpEnabled = false;

    public Clan(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members.put(owner, Rank.LEADER);
        this.bankBalance = 0.0;
        this.createdAt = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Map<UUID, Rank> getMembers() {
        return members;
    }

    public Rank getRank(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public boolean isFull() {
        return members.size() >= MAX_MEMBERS;
    }

    public void addMember(UUID uuid, Rank rank) {
        members.put(uuid, rank);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        homeAllowed.remove(uuid);
    }

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public boolean hasHome() {
        return home != null;
    }

    /** The leader always has home access; other members need to be explicitly granted it. */
    public boolean hasHomePermission(UUID uuid) {
        return getRank(uuid) == Rank.LEADER || homeAllowed.contains(uuid);
    }

    public Set<UUID> getHomeAllowed() {
        return homeAllowed;
    }

    /** @return true if the member now HAS home access, false if it was just revoked */
    public boolean toggleHomePermission(UUID uuid) {
        if (homeAllowed.contains(uuid)) {
            homeAllowed.remove(uuid);
            return false;
        }
        homeAllowed.add(uuid);
        return true;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }

    public void deposit(double amount) {
        this.bankBalance += amount;
    }

    /** @return true if there was enough balance and the withdrawal succeeded */
    public boolean withdraw(double amount) {
        if (bankBalance < amount) {
            return false;
        }
        bankBalance -= amount;
        return true;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }
}
