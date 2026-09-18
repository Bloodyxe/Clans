package de.customclans.clans;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks pending clan invites. A player can have at most one pending invite at a time -
 * inviting them again simply overwrites it. Purely in-memory, doesn't need to survive restarts.
 */
public class InviteManager {

    /** invited player UUID -> clan name (lowercase) they were invited to */
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public void invite(UUID player, String clanName) {
        pendingInvites.put(player, clanName.toLowerCase());
    }

    public String getPendingClan(UUID player) {
        return pendingInvites.get(player);
    }

    public boolean hasPendingInvite(UUID player) {
        return pendingInvites.containsKey(player);
    }

    public void clear(UUID player) {
        pendingInvites.remove(player);
    }
}
