package com.diogocorreadcs.clansystem.listeners;

import com.diogocorreadcs.clansystem.managers.ClanManager;
import com.diogocorreadcs.clansystem.menus.ClanMainMenu;
import com.diogocorreadcs.clansystem.menus.ClanMembersMenu;
import com.diogocorreadcs.clansystem.menus.NoClanMenu;
import com.diogocorreadcs.clansystem.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    private final ClanManager clanManager;

    public MenuListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(ClanMainMenu.TITLE) && !title.equals(ClanMembersMenu.TITLE) && !title.equals(NoClanMenu.TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        if (title.equals(NoClanMenu.TITLE)) {
            if (itemName.equals("Criar um Clã")) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Digite no chat: /clan criar <nome> <tag>");
            } else if (itemName.equals("Aceitar Convite")) {
                player.closeInventory();
                player.chat("/clan aceitar");
            }
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.closeInventory();
            return;
        }

        if (title.equals(ClanMainMenu.TITLE)) {
            if (itemName.equals("Gerenciar Membros")) {
                ClanMembersMenu.open(player, clan);
            } else if (itemName.equals("Convidar Jogador")) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Digite no chat: /clan convidar <jogador>");
            } else if (itemName.equals("Desfazer Clã")) {
                player.closeInventory();
                player.chat("/clan deletar");
            } else if (itemName.equals("Sair do Clã")) {
                player.closeInventory();
                player.chat("/clan sair");
            }
            return;
        }

        if (title.equals(ClanMembersMenu.TITLE)) {
            if (event.getCurrentItem().getType() == Material.ARROW) {
                ClanMainMenu.open(player, clan);
                return;
            }

            if (event.getCurrentItem().getType() == Material.SKULL_ITEM) {
                if (!clan.getLeader().equals(player.getUniqueId())) {
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(itemName);
                if (target != null && target.getUniqueId() != null) {
                    if (target.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + "Você não pode expulsar a si mesmo.");
                        return;
                    }

                    if (clanManager.kickMember(target.getUniqueId(), player.getUniqueId())) {
                        player.sendMessage(ChatColor.GREEN + "Jogador " + itemName + " expulso do clã.");
                        ClanMembersMenu.open(player, clan);
                    }
                }
            }
        }
    }
}