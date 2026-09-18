package de.customclans.clans;

public enum Rank {
    LEADER,
    OFFICER,
    MEMBER;

    public boolean atLeast(Rank other) {
        return this.ordinal() <= other.ordinal();
    }
}
