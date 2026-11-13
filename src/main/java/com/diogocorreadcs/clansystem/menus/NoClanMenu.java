package com.diogocorreadcs.clansystem.menus;

import com.diogocorreadcs.clansystem.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class NoClanMenu {

    public static final String TITLE = ChatColor.DARK_GRAY + "Você não possui um Clã";

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        inv.setItem(11, new ItemBuilder(Material.EMERALD)
                .setName("&aCriar um Clã")
                .setLore("&7Use o comando abaixo no chat:", "&e/clan criar <nome> <tag>")
                .build());

        inv.setItem(15, new ItemBuilder(Material.PAPER)
                .setName("&eAceitar Convite")
                .setLore("&7Recebeu um convite de clã?", "&7Clique para aceitar agora!")
                .build());

        player.openInventory(inv);
    }
}