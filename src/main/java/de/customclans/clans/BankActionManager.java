package de.customclans.clans;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers that a player clicked "deposit" or "withdraw" in the clan bank GUI and is now
 * expected to type an amount in chat. ClanChatListener checks this before treating a chat
 * message as clan chat.
 */
public class BankActionManager {

    public enum Action {
        DEPOSIT, WITHDRAW
    }

    private final Map<UUID, Action> pending = new HashMap<>();

    public void setPending(UUID uuid, Action action) {
        pending.put(uuid, action);
    }

    public Action getPending(UUID uuid) {
        return pending.get(uuid);
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void clear(UUID uuid) {
        pending.remove(uuid);
    }
}
