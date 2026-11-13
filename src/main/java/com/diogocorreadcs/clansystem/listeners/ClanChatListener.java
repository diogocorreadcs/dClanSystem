package com.diogocorreadcs.clansystem.listeners;

import com.diogocorreadcs.clansystem.managers.ClanManager;
import com.diogocorreadcs.clansystem.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ClanChatListener implements Listener {

    private final ClanManager clanManager;

    public ClanChatListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (clanManager.isInClanChat(player.getUniqueId())) {
            event.setCancelled(true);

            Clan clan = clanManager.getPlayerClan(player.getUniqueId());
            if (clan == null) {
                clanManager.toggleClanChat(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Você não está mais em um clã. Chat do clã desativado.");
                return;
            }

            String format = ChatColor.translateAlternateColorCodes('&', "&2[Chat-Clã] &e" + player.getName() + "&7: &f") + event.getMessage();

            for (UUID memberUuid : clan.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.sendMessage(format);
                }
            }
            Bukkit.getScheduler().runTask(com.diogocorreadcs.clansystem.ClanSystem.getPlugin(
                    com.diogocorreadcs.clansystem.ClanSystem.class),
                    () -> clanManager.recordActivity(player.getUniqueId(), "chat-message"));
        }
    }
}