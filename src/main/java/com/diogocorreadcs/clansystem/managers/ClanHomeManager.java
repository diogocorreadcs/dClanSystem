package com.diogocorreadcs.clansystem.managers;

import com.diogocorreadcs.clansystem.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClanHomeManager implements Listener {

    private final JavaPlugin plugin;
    private final ClanManager clanManager;
    private final VaultEconomyHook economyHook;
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    public ClanHomeManager(JavaPlugin plugin, ClanManager clanManager, VaultEconomyHook economyHook) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.economyHook = economyHook;
    }

    public void teleportHome(final Player player, final Clan clan) {
        final FileConfiguration config = plugin.getConfig();
        final Location home = clan.getHome();
        final String prefix = config.getString("messages.prefix", "&2[dClanSystem] &7");
        if (home == null || home.getWorld() == null) {
            send(player, prefix, config.getString("messages.home.not-set", "&cO clã ainda não definiu uma home."));
            return;
        }

        int warmup = Math.max(0, config.getInt("home.warmup-seconds", 5));
        double cost = Math.max(0.0, config.getDouble("home.teleport-cost", 0.0));
        if (cost > 0.0 && !economyHook.isAvailable()) {
            send(player, prefix, config.getString("messages.economy-unavailable",
                    "&cA economia não está disponível. Avise um administrador."));
            return;
        }
        if (!economyHook.hasFunds(player, cost)) {
            send(player, prefix, config.getString("messages.home.insufficient-funds",
                    "&cVocê precisa de {amount} para usar a home.")
                    .replace("{amount}", economyHook.format(cost)));
            return;
        }

        cancelTeleport(player.getUniqueId(), false);
        if (warmup == 0) {
            completeTeleport(player, home, cost, prefix, config);
            return;
        }

        send(player, prefix, config.getString("messages.home.warmup-start",
                "&aTeleporte em {seconds} segundos. Não se mova.")
                .replace("{seconds}", String.valueOf(warmup)));
        final int[] elapsed = {0};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            elapsed[0]++;
            int remaining = warmup - elapsed[0];
            if (remaining > 0 && remaining % Math.max(1, config.getInt("home.countdown-interval-seconds", 1)) == 0) {
                send(player, prefix, config.getString("messages.home.warmup-countdown",
                        "&eTeleportando em {seconds}s...").replace("{seconds}", String.valueOf(remaining)));
            }
            if (remaining <= 0) {
                cancelTeleport(player.getUniqueId(), false);
                completeTeleport(player, home, cost, prefix, config);
            }
        }, 20L, 20L);
        pendingTeleports.put(player.getUniqueId(), task);
    }

    private void completeTeleport(Player player, Location home, double cost, String prefix,
                                  FileConfiguration config) {
        if (!player.isOnline()) {
            return;
        }
        if (cost > 0.0 && !economyHook.withdraw(player, cost)) {
            send(player, prefix, config.getString("messages.home.insufficient-funds",
                    "&cVocê precisa de {amount} para usar a home.")
                    .replace("{amount}", economyHook.format(cost)));
            return;
        }
        if (player.teleport(home.clone())) {
            send(player, prefix, config.getString("messages.home.teleported", "&aVocê chegou à home do clã."));
        } else {
            economyHook.refund(player, cost);
            send(player, prefix, config.getString("messages.home.teleport-failed",
                    "&cO teleporte foi bloqueado; nenhum valor foi cobrado."));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("home.cancel-on-move", true)
                || event.getTo() == null || !hasMovedBlock(event.getFrom(), event.getTo())) {
            return;
        }
        cancelTeleport(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (plugin.getConfig().getBoolean("home.cancel-on-damage", true)
                && event.getEntity() instanceof Player
                && pendingTeleports.containsKey(event.getEntity().getUniqueId())) {
            cancelTeleport(event.getEntity().getUniqueId(), true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelTeleport(event.getPlayer().getUniqueId(), false);
    }

    private boolean hasMovedBlock(Location from, Location to) {
        return from.getWorld() != to.getWorld()
                || from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    private void cancelTeleport(UUID playerId, boolean notify) {
        BukkitTask task = pendingTeleports.remove(playerId);
        if (task != null) {
            task.cancel();
            if (notify) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) {
                    send(player, plugin.getConfig().getString("messages.prefix", "&2[dClanSystem] &7"),
                            plugin.getConfig().getString("messages.home.warmup-cancelled",
                                    "&cTeleporte cancelado porque você se moveu ou sofreu dano."));
                }
            }
        }
    }

    private void send(Player player, String prefix, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
