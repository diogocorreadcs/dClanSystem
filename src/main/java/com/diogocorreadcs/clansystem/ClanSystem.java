package com.diogocorreadcs.clansystem;

import com.diogocorreadcs.clansystem.commands.ClanChatCommand;
import com.diogocorreadcs.clansystem.commands.ClanCommand;
import com.diogocorreadcs.clansystem.integrations.ClanPlaceholderExpansion;
import com.diogocorreadcs.clansystem.listeners.ClanChatListener;
import com.diogocorreadcs.clansystem.listeners.FriendlyFireListener;
import com.diogocorreadcs.clansystem.listeners.MenuListener;
import com.diogocorreadcs.clansystem.managers.ClanManager;
import com.diogocorreadcs.clansystem.managers.ClanHomeManager;
import com.diogocorreadcs.clansystem.managers.VaultEconomyHook;
import org.bukkit.plugin.java.JavaPlugin;

public class ClanSystem extends JavaPlugin {

    private ClanManager clanManager;
    private VaultEconomyHook economyHook;
    private ClanHomeManager homeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.clanManager = new ClanManager(this);
        this.economyHook = new VaultEconomyHook(this);
        this.homeManager = new ClanHomeManager(this, clanManager, economyHook);

        ClanCommand clanCommand = new ClanCommand(clanManager, economyHook, homeManager);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);

        getCommand("cc").setExecutor(new ClanChatCommand(clanManager));
        getServer().getPluginManager().registerEvents(homeManager, this);
        getServer().getPluginManager().registerEvents(new ClanChatListener(clanManager), this);

        boolean friendlyFireEnabled = getConfig().getBoolean("settings.friendly-fire-enabled", false);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(clanManager, friendlyFireEnabled), this);
        getServer().getPluginManager().registerEvents(new MenuListener(clanManager), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ClanPlaceholderExpansion(this, clanManager).register();
            getLogger().info("Integração com PlaceholderAPI ativada.");
        }

        getLogger().info("dClanSystem habilitado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("dClanSystem desabilitado.");
    }

    public ClanManager getClanManager() {
        return clanManager;
    }
}