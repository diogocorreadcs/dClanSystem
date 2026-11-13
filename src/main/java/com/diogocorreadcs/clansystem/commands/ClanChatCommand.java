package com.diogocorreadcs.clansystem.commands;

import com.diogocorreadcs.clansystem.ClanSystem;
import com.diogocorreadcs.clansystem.managers.ClanManager;
import com.diogocorreadcs.clansystem.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ClanChatCommand implements CommandExecutor {

    private final ClanManager clanManager;

    public ClanChatCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ClanSystem plugin = ClanSystem.getPlugin(ClanSystem.class);
        FileConfiguration config = plugin.getConfig();
        String prefix = config.getString("messages.prefix", "&2[dClanSystem] &7");

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cVocê não pertence a nenhum clã."));
            return true;
        }

        if (args.length > 0) {
            StringBuilder message = new StringBuilder();
            for (String arg : args) {
                message.append(arg).append(" ");
            }

            String format = ChatColor.translateAlternateColorCodes('&', "&2[Chat-Clã] &e" + player.getName() + "&7: &f") + message.toString().trim();
            for (java.util.UUID memberUuid : clan.getMembers()) {
                Player member = org.bukkit.Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.sendMessage(format);
                }
            }
            clanManager.recordActivity(player.getUniqueId(), "chat-message");
            return true;
        }

        boolean toggled = clanManager.toggleClanChat(player.getUniqueId());
        if (toggled) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aCanal do clã ativado! Suas mensagens irão apenas para aliados."));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cCanal do clã desativado. Você voltou ao chat público."));
        }

        return true;
    }
}