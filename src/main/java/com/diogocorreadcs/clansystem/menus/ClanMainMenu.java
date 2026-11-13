package com.diogocorreadcs.clansystem.menus;

import com.diogocorreadcs.clansystem.models.Clan;
import com.diogocorreadcs.clansystem.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ClanMainMenu {

    public static final String TITLE = ChatColor.DARK_GREEN + "Painel do Clã";

    public static void open(Player player, Clan clan) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
        String leaderName = leader.getName() != null ? leader.getName() : "Desconhecido";

        inv.setItem(10, new ItemBuilder(Material.BOOK)
                .setName("&aInformações do Clã")
                .setLore(
                        "&7Nome: &f" + clan.getName(),
                        "&7Tag: &f[" + clan.getTag() + "]",
                        "&7Líder: &e" + leaderName,
                        "&7Membros: &f" + clan.getMembers().size()
                )
                .build());

        inv.setItem(12, new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                .setName("&eGerenciar Membros")
                .setLore("&7Clique para visualizar a lista", "&7e gerenciar cargos/expulsões.")
                .build());

        inv.setItem(14, new ItemBuilder(Material.PAPER)
                .setName("&6Convidar Jogador")
                .setLore("&7Clique para ver como convidar", "&7novos membros para o clã.")
                .build());

        if (clan.getLeader().equals(player.getUniqueId())) {
            inv.setItem(16, new ItemBuilder(Material.TNT)
                    .setName("&cDesfazer Clã")
                    .setLore("&7Atenção: Esta ação é irreversível!", "&7Clique para deletar o clã.")
                    .build());
        } else {
            inv.setItem(16, new ItemBuilder(Material.BARRIER)
                    .setName("&cSair do Clã")
                    .setLore("&7Clique para abandonar este clã.")
                    .build());
        }

        player.openInventory(inv);
    }
}