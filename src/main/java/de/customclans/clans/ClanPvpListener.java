package de.customclans.clans;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Cancels damage between two players who are in the same clan, unless that clan's leader
 * has turned Clan PVP on (see the main menu's PVP toggle button).
 */
public class ClanPvpListener implements Listener {

    private final ClanManager clanManager;

    public ClanPvpListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (victim.getUniqueId().equals(attacker.getUniqueId())) {
            return;
        }

        Clan victimClan = clanManager.getClanByPlayer(victim.getUniqueId());
        Clan attackerClan = clanManager.getClanByPlayer(attacker.getUniqueId());
        if (victimClan == null || attackerClan == null) {
            return;
        }
        if (!victimClan.getName().equalsIgnoreCase(attackerClan.getName())) {
            return;
        }
        if (!victimClan.isPvpEnabled()) {
            event.setCancelled(true);
        }
    }
}
