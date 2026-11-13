package com.diogocorreadcs.clansystem.integrations;

import com.diogocorreadcs.clansystem.ClanSystem;
import com.diogocorreadcs.clansystem.managers.ClanManager;
import com.diogocorreadcs.clansystem.models.Clan;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class ClanPlaceholderExpansion extends PlaceholderExpansion {

    private final ClanSystem plugin;
    private final ClanManager clanManager;

    public ClanPlaceholderExpansion(ClanSystem plugin, ClanManager clanManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
    }

    @Override
    public String getIdentifier() {
        return "dclansystem";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.toLowerCase(java.util.Locale.ROOT).startsWith("top_")) {
            String[] parts = params.substring(4).split("_", 2);
            if (parts.length == 2) {
                try {
                    int position = Integer.parseInt(parts[1]);
                    if (position < 1) {
                        return null;
                    }
                    java.util.List<Clan> topClans = clanManager.getTopClans(parts[0]);
                    if (position > topClans.size()) {
                        return "";
                    }
                    Clan topClan = topClans.get(position - 1);
                    if ("name".equalsIgnoreCase(parts[0])) {
                        return topClan.getName();
                    }
                    if ("tag".equalsIgnoreCase(parts[0])) {
                        return topClan.getTag();
                    }
                    if ("members".equalsIgnoreCase(parts[0])) {
                        return String.valueOf(topClan.getMembers().size());
                    }
                    if ("online".equalsIgnoreCase(parts[0])) {
                        return String.valueOf(clanManager.getOnlineMemberCount(topClan));
                    }
                    if ("activity".equalsIgnoreCase(parts[0]) || "score".equalsIgnoreCase(parts[0])) {
                        return String.valueOf(topClan.getActivityScore());
                    }
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if ("has_clan".equalsIgnoreCase(params)) {
            return String.valueOf(clan != null);
        }
        if ("has_home".equalsIgnoreCase(params)) {
            return String.valueOf(clan != null && clan.getHome() != null);
        }
        if (clan == null) {
            if ("role".equalsIgnoreCase(params)) {
                return "none";
            }
            return "";
        }

        if ("name".equalsIgnoreCase(params)) {
            return clan.getName();
        }
        if ("tag".equalsIgnoreCase(params)) {
            return clan.getTag();
        }
        if ("members".equalsIgnoreCase(params)) {
            return String.valueOf(clan.getMembers().size());
        }
        if ("online".equalsIgnoreCase(params)) {
            return String.valueOf(clanManager.getOnlineMemberCount(clan));
        }
        if ("activity".equalsIgnoreCase(params) || "score".equalsIgnoreCase(params)) {
            return String.valueOf(clan.getActivityScore());
        }
        if ("leader".equalsIgnoreCase(params)) {
            OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
            return leader.getName() == null ? clan.getLeader().toString() : leader.getName();
        }
        if ("role".equalsIgnoreCase(params)) {
            return clan.getLeader().equals(player.getUniqueId()) ? "leader" : "member";
        }
        return null;
    }
}
