package com.diogocorreadcs.clansystem.menus;

import com.diogocorreadcs.clansystem.models.Clan;
import com.diogocorreadcs.clansystem.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class ClanMembersMenu {

    public static final String TITLE = ChatColor.DARK_GREEN + "Membros do Clã";

    public static void open(Player player, Clan clan) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        boolean isLeader = clan.getLeader().equals(player.getUniqueId());

        int slot = 0;
        for (UUID memberUuid : clan.getMembers()) {
            if (slot >= 45) break;

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUuid);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Desconhecido";
            String status = offlinePlayer.isOnline() ? "&aOnline" : "&cOffline";

            if (memberUuid.equals(clan.getLeader())) {
                inv.setItem(slot, new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                        .setSkullOwner(name)
                        .setName("&6" + name)
                        .setLore("&7Cargo: &6Líder", "&7Status: " + status)
                        .build());
            } else {
                if (isLeader) {
                    inv.setItem(slot, new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                            .setSkullOwner(name)
                            .setName("&a" + name)
                            .setLore("&7Cargo: &eMembro", "&7Status: " + status, "", "&cClique para expulsar.")
                            .build());
                } else {
                    inv.setItem(slot, new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                            .setSkullOwner(name)
                            .setName("&a" + name)
                            .setLore("&7Cargo: &eMembro", "&7Status: " + status)
                            .build());
                }
            }
            slot++;
        }

        inv.setItem(49, new ItemBuilder(Material.ARROW)
                .setName("&cVoltar")
                .setLore("&7Voltar ao painel principal.")
                .build());

        player.openInventory(inv);
    }
}