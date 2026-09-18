package de.customclans.clans;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Runs every second: for every player who turned /clan glow on, makes their online clan mates
 * within 40 blocks glow (vanilla Glowing entity flag).
 *
 * Note: the vanilla glow flag is a property of the entity itself, so anyone who can see that
 * player (not just clan members with glow enabled) will see the glow outline too - vanilla
 * Bukkit/Paper has no per-viewer-only glow without a packet library like ProtocolLib.
 */
public class ClanGlowTask extends BukkitRunnable {

    public static final int RANGE = 40;

    private final ClanManager clanManager;
    private final ClanGlowManager glowManager;
    /** Players currently glowing because of this feature, so we know who to un-glow later. */
    private final Set<UUID> currentlyGlowing = new HashSet<>();

    public ClanGlowTask(ClanManager clanManager, ClanGlowManager glowManager) {
        this.clanManager = clanManager;
        this.glowManager = glowManager;
    }

    @Override
    public void run() {
        Set<UUID> shouldGlow = new HashSet<>();

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!glowManager.isEnabled(viewer.getUniqueId())) {
                continue;
            }
            Clan clan = clanManager.getClanByPlayer(viewer.getUniqueId());
            if (clan == null) {
                continue;
            }

            for (UUID memberId : clan.getMembers().keySet()) {
                if (memberId.equals(viewer.getUniqueId())) {
                    continue;
                }
                Player member = Bukkit.getPlayer(memberId);
                if (member == null || !member.isOnline()) {
                    continue;
                }
                if (!member.getWorld().equals(viewer.getWorld())) {
                    continue;
                }
                if (member.getLocation().distance(viewer.getLocation()) <= RANGE) {
                    shouldGlow.add(memberId);
                }
            }
        }

        for (UUID id : shouldGlow) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && !p.isGlowing()) {
                p.setGlowing(true);
            }
        }
        for (UUID id : currentlyGlowing) {
            if (!shouldGlow.contains(id)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isGlowing()) {
                    p.setGlowing(false);
                }
            }
        }

        currentlyGlowing.clear();
        currentlyGlowing.addAll(shouldGlow);
    }

    /** Clears the glow flag from everyone currently glowing because of this feature. */
    public void resetAll() {
        for (UUID id : currentlyGlowing) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.setGlowing(false);
            }
        }
        currentlyGlowing.clear();
    }
}
