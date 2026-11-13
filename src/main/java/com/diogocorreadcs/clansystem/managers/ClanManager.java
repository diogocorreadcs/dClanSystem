package com.diogocorreadcs.clansystem.managers;

import com.diogocorreadcs.clansystem.models.Clan;
import com.diogocorreadcs.clansystem.models.ClanLogEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClanManager {

    private final JavaPlugin plugin;
    private final Database database;
    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<UUID, Clan> playerClans = new HashMap<>();
    private final Map<UUID, String> pendingInvites = new HashMap<>();
    private final Set<UUID> clanChatToggled = new HashSet<>();

    public ClanManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.database = new Database(plugin);
        loadClansFromDatabase();
    }

    private void loadClansFromDatabase() {
        Map<String, Clan> loaded = database.loadClans();
        for (Clan clan : loaded.values()) {
            clans.put(clan.getName().toLowerCase(), clan);
            playerClans.put(clan.getLeader(), clan);
            for (UUID member : clan.getMembers()) {
                playerClans.put(member, clan);
            }
        }
    }

    public boolean createClan(String name, String tag, UUID leader) {
        if (playerClans.containsKey(leader) || clans.containsKey(name.toLowerCase())) {
            return false;
        }

        Clan clan = new Clan(name, tag, leader);
        logMembership(clan, "JOIN", leader, null);
        clans.put(name.toLowerCase(), clan);
        playerClans.put(leader, clan);
        database.saveClan(clan);
        return true;
    }

    public void invitePlayer(UUID targetUuid, String clanName) {
        pendingInvites.put(targetUuid, clanName.toLowerCase());
    }

    public Clan acceptInvite(UUID playerUuid) {
        if (playerClans.containsKey(playerUuid)) {
            return null;
        }

        String clanName = pendingInvites.remove(playerUuid);
        if (clanName == null) {
            return null;
        }

        Clan clan = clans.get(clanName);
        if (clan == null) {
            return null;
        }

        clan.addMember(playerUuid);
        playerClans.put(playerUuid, clan);
        logMembership(clan, "JOIN", playerUuid, null);
        database.saveClan(clan);
        return clan;
    }

    public boolean leaveClan(UUID playerUuid) {
        Clan clan = playerClans.get(playerUuid);
        if (clan == null || clan.getLeader().equals(playerUuid)) {
            return false;
        }

        clan.removeMember(playerUuid);
        playerClans.remove(playerUuid);
        logMembership(clan, "LEAVE", playerUuid, null);
        database.saveClan(clan);
        return true;
    }

    public boolean kickMember(UUID playerUuid) {
        return kickMember(playerUuid, null);
    }

    public boolean kickMember(UUID playerUuid, UUID actorUuid) {
        Clan clan = playerClans.get(playerUuid);
        if (clan == null || clan.getLeader().equals(playerUuid)) {
            return false;
        }

        clan.removeMember(playerUuid);
        playerClans.remove(playerUuid);
        logMembership(clan, "KICK", playerUuid, actorUuid);
        database.saveClan(clan);
        return true;
    }

    private void logMembership(Clan clan, String action, UUID playerUuid, UUID actorUuid) {
        clan.addActivity(plugin.getConfig().getLong("top.activity-points.membership-change", 1L));
        if (!plugin.getConfig().getBoolean("history.enabled", true)) {
            return;
        }
        clan.addHistoryEntry(new ClanLogEntry(System.currentTimeMillis(), action, playerUuid, actorUuid));
        int historyLimit = Math.max(1, plugin.getConfig().getInt("history.max-entries-per-clan", 100));
        while (clan.getHistory().size() > historyLimit) {
            clan.getHistory().remove(0);
        }
    }

    public boolean setHome(String clanName, Location location) {
        Clan clan = getClanByName(clanName);
        if (clan == null || location == null || location.getWorld() == null) {
            return false;
        }
        clan.setHome(location);
        database.saveClan(clan);
        return true;
    }

    public void recordActivity(UUID playerUuid, String activityType) {
        Clan clan = getPlayerClan(playerUuid);
        if (clan == null) {
            return;
        }
        long points = plugin.getConfig().getLong("top.activity-points." + activityType, 1L);
        clan.addActivity(points);
        database.saveClan(clan);
    }

    public List<Clan> getTopClans(String category) {
        List<Clan> result = new ArrayList<>(clans.values());
        if ("online".equalsIgnoreCase(category)) {
            result.sort(Comparator.comparingInt(this::getOnlineMemberCount).reversed()
                    .thenComparing(Clan::getName, String.CASE_INSENSITIVE_ORDER));
        } else if ("activity".equalsIgnoreCase(category) || "atividade".equalsIgnoreCase(category)) {
            result.sort(Comparator.comparingLong(Clan::getActivityScore).reversed()
                    .thenComparing(Clan::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            result.sort(Comparator.comparingInt((Clan clan) -> clan.getMembers().size()).reversed()
                    .thenComparing(Clan::getName, String.CASE_INSENSITIVE_ORDER));
        }
        return result;
    }

    public int getOnlineMemberCount(Clan clan) {
        int online = 0;
        for (UUID member : clan.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                online++;
            }
        }
        return online;
    }

    public Clan getPlayerClan(UUID playerUuid) {
        return playerClans.get(playerUuid);
    }

    public Clan getClanByName(String name) {
        return clans.get(name.toLowerCase());
    }

    public void deleteClan(String name) {
        Clan clan = clans.remove(name.toLowerCase());
        if (clan != null) {
            for (UUID member : clan.getMembers()) {
                playerClans.remove(member);
            }
            database.removeClan(name);
        }
    }

    public boolean toggleClanChat(UUID playerUuid) {
        if (clanChatToggled.contains(playerUuid)) {
            clanChatToggled.remove(playerUuid);
            return false;
        } else {
            clanChatToggled.add(playerUuid);
            return true;
        }
    }

    public boolean isInClanChat(UUID playerUuid) {
        return clanChatToggled.contains(playerUuid);
    }
}