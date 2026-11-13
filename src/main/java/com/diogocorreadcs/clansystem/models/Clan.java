package com.diogocorreadcs.clansystem.models;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Clan {

    private final String name;
    private final String tag;
    private UUID leader;
    private final Set<UUID> members;
    private final List<ClanLogEntry> history;
    private Location home;
    private long activityScore;

    public Clan(String name, String tag, UUID leader) {
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader);
        this.history = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID member) {
        this.members.add(member);
    }

    public void removeMember(UUID member) {
        this.members.remove(member);
    }

    public List<ClanLogEntry> getHistory() {
        return history;
    }

    public void addHistoryEntry(ClanLogEntry entry) {
        history.add(entry);
    }

    public Location getHome() {
        return home == null ? null : home.clone();
    }

    public void setHome(Location home) {
        this.home = home == null ? null : home.clone();
    }

    public long getActivityScore() {
        return activityScore;
    }

    public void addActivity(long points) {
        if (points > 0) {
            activityScore += points;
        }
    }

    public void setActivityScore(long activityScore) {
        this.activityScore = Math.max(0, activityScore);
    }
}