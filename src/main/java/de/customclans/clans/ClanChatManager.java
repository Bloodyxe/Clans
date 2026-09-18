package de.customclans.clans;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players currently have Clan-Chat mode toggled on (/clan chat).
 * Purely in-memory - resets on server restart, which is fine for a chat mode toggle.
 */
public class ClanChatManager {

    private final Set<UUID> enabled = new HashSet<>();

    public boolean isEnabled(UUID uuid) {
        return enabled.contains(uuid);
    }

    /** @return true if clan chat is now ON for this player, false if it's now OFF */
    public boolean toggle(UUID uuid) {
        if (enabled.contains(uuid)) {
            enabled.remove(uuid);
            return false;
        }
        enabled.add(uuid);
        return true;
    }

    public void disable(UUID uuid) {
        enabled.remove(uuid);
    }
}
