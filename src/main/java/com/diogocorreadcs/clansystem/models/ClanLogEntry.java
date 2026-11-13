package com.diogocorreadcs.clansystem.models;

import java.util.UUID;

public class ClanLogEntry {

    private final long timestamp;
    private final String action;
    private final UUID player;
    private final UUID actor;

    public ClanLogEntry(long timestamp, String action, UUID player, UUID actor) {
        this.timestamp = timestamp;
        this.action = action;
        this.player = player;
        this.actor = actor;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAction() {
        return action;
    }

    public UUID getPlayer() {
        return player;
    }

    public UUID getActor() {
        return actor;
    }
}
