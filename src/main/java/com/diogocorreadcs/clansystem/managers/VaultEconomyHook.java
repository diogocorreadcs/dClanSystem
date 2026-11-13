package com.diogocorreadcs.clansystem.managers;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultEconomyHook {

    private final JavaPlugin plugin;
    private final double creationCost;

    public VaultEconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.creationCost = Math.max(0.0, plugin.getConfig().getDouble("settings.clan-creation-cost", 0.0));
    }

    public double getCreationCost() {
        return Math.max(0.0, plugin.getConfig().getDouble("settings.clan-creation-cost", creationCost));
    }

    public boolean isAvailable() {
        return getEconomy() != null;
    }

    public boolean hasFunds(Player player, double amount) {
        if (amount <= 0.0) {
            return true;
        }
        Economy economy = getEconomy();
        return economy != null && economy.has(player, amount);
    }

    public String format(double amount) {
        Economy economy = getEconomy();
        return economy == null ? String.valueOf(amount) : economy.format(amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (amount <= 0.0) {
            return true;
        }
        Economy economy = getEconomy();
        if (economy == null || !economy.has(player, amount)) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public void refund(Player player, double amount) {
        if (amount <= 0.0) {
            return;
        }
        Economy economy = getEconomy();
        if (economy == null) {
            plugin.getLogger().severe("Não foi possível reembolsar " + player.getName()
                    + " após uma falha ao criar o clã: nenhum provedor de economia Vault está disponível.");
            return;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().severe("Não foi possível reembolsar " + player.getName()
                    + " após uma falha ao criar o clã: " + response.errorMessage);
        }
    }

    private Economy getEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return registration == null ? null : registration.getProvider();
    }
}
