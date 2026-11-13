package com.diogocorreadcs.clansystem.listeners;

import com.diogocorreadcs.clansystem.ClanSystem;
import com.diogocorreadcs.clansystem.managers.ClanManager;
import com.diogocorreadcs.clansystem.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class FriendlyFireListener implements Listener {

    private final ClanManager clanManager;
    private final boolean friendlyFireEnabled;

    public FriendlyFireListener(ClanManager clanManager, boolean friendlyFireEnabled) {
        this.clanManager = clanManager;
        this.friendlyFireEnabled = friendlyFireEnabled;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (friendlyFireEnabled) {
            return;
        }

        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player damager = (Player) event.getDamager();

            Clan victimClan = clanManager.getPlayerClan(victim.getUniqueId());
            Clan damagerClan = clanManager.getPlayerClan(damager.getUniqueId());

            if (victimClan != null && damagerClan != null && victimClan.equals(damagerClan)) {
                ClanSystem plugin = ClanSystem.getPlugin(ClanSystem.class);
                FileConfiguration config = plugin.getConfig();

                String prefix = config.getString("messages.prefix", "&2[dClanSystem] &7");
                String message = config.getString("messages.clan.friendly-fire-blocked", "&cVocê não pode ferir membros do seu próprio clã!");

                damager.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
                event.setCancelled(true);
            }
        }
    }
}