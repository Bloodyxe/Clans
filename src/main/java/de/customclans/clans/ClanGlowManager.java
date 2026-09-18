package de.customclans.clans;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players have /clan glow turned on. See ClanGlowTask for the actual glow logic.
 */
public class ClanGlowManager {

    private final Set<UUID> enabled = new HashSet<>();

    public boolean isEnabled(UUID uuid) {
        return enabled.contains(uuid);
    }

    /** @return the new state (true = now enabled, false = now disabled) */
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
