package de.customclans.clans;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory representation of a single clan. Persistence is handled by {@link ClanManager}.
 */
public class Clan {

    private String name;
    private UUID owner;
    /** UUID -> Rank (LEADER/OFFICER/MEMBER). The owner is always LEADER. */
    private final Map<UUID, Rank> members = new LinkedHashMap<>();
    /** Exactly one home per clan, independent of every other clan's home. */
    private Location home;
    private double bankBalance;

    public Clan(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members.put(owner, Rank.LEADER);
        this.bankBalance = 0.0;
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

    public void addMember(UUID uuid, Rank rank) {
        members.put(uuid, rank);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
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
}
