package com.diogocorreadcs.clansystem.commands;

import com.diogocorreadcs.clansystem.ClanSystem;
import com.diogocorreadcs.clansystem.managers.ClanManager;
import com.diogocorreadcs.clansystem.managers.ClanHomeManager;
import com.diogocorreadcs.clansystem.managers.VaultEconomyHook;
import com.diogocorreadcs.clansystem.menus.ClanMainMenu;
import com.diogocorreadcs.clansystem.menus.NoClanMenu;
import com.diogocorreadcs.clansystem.models.Clan;
import com.diogocorreadcs.clansystem.models.ClanLogEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final ClanManager clanManager;
    private final VaultEconomyHook economyHook;
    private final ClanHomeManager homeManager;

    public ClanCommand(ClanManager clanManager, VaultEconomyHook economyHook, ClanHomeManager homeManager) {
        this.clanManager = clanManager;
        this.economyHook = economyHook;
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ClanSystem plugin = ClanSystem.getPlugin(ClanSystem.class);
        FileConfiguration config = plugin.getConfig();
        String prefix = config.getString("messages.prefix", "&2[dClanSystem] &7");

        if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
            return showTop(sender, args, config, prefix);
        }

        if (!(sender instanceof Player)) {
            String msg = config.getString("messages.only-players", "&cEste comando só pode ser executado por jogadores.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            Clan clan = clanManager.getPlayerClan(player.getUniqueId());
            if (clan != null) {
                ClanMainMenu.open(player, clan);
            } else {
                NoClanMenu.open(player);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("criar")) {
            if (!hasPermission(player, "dclansystem.command.create", config, prefix)) {
                return true;
            }
            int maxNameLength = config.getInt("settings.max-clan-name-length", 16);
            int maxTagLength = config.getInt("settings.max-clan-tag-length", 4);

            if (args.length < 3) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cUso correto: /clan criar <nome> <tag>"));
                return true;
            }

            String name = args[1];
            String tag = args[2];

            if (name.length() > maxNameLength) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cO nome do clã deve ter no máximo " + maxNameLength + " caracteres."));
                return true;
            }

            if (tag.length() > maxTagLength) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cA tag do clã deve ter no máximo " + maxTagLength + " caracteres."));
                return true;
            }

            if (clanManager.getPlayerClan(player.getUniqueId()) != null || clanManager.getClanByName(name) != null) {
                String rawMsg = config.getString("messages.clan.already-in-clan", "&cVocê já pertence a um clã ou este nome já está em uso.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            double creationCost = economyHook.getCreationCost();
            if (creationCost > 0.0 && !economyHook.isAvailable()) {
                sendConfiguredMessage(player, prefix, config.getString("messages.economy-unavailable",
                        "&cA economia não está disponível. Avise um administrador."));
                return true;
            }
            if (!economyHook.withdraw(player, creationCost)) {
                String insufficientFunds = config.getString("messages.insufficient-funds",
                        "&cVocê precisa de {amount} para criar um clã.")
                        .replace("{amount}", economyHook.format(creationCost));
                sendConfiguredMessage(player, prefix, insufficientFunds);
                return true;
            }

            boolean success = clanManager.createClan(name, tag, player.getUniqueId());
            if (success) {
                String rawMsg = config.getString("messages.clan.created", "&aClã [{tag}] {name} criado com sucesso!");
                rawMsg = rawMsg.replace("{tag}", tag).replace("{name}", name);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
            } else {
                economyHook.refund(player, creationCost);
                String rawMsg = config.getString("messages.clan.already-in-clan", "&cVocê já pertence a um clã ou este nome já está em uso.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!hasPermission(player, "dclansystem.command.info", config, prefix)) {
                return true;
            }
            Clan clan = clanManager.getPlayerClan(player.getUniqueId());
            if (clan == null) {
                String rawMsg = config.getString("messages.clan.not-in-clan", "&cVocê não pertence a nenhum clã.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            player.sendMessage(ChatColor.DARK_GREEN + "=== Informações do Clã ===");
            player.sendMessage(ChatColor.YELLOW + "Nome: " + ChatColor.WHITE + clan.getName());
            player.sendMessage(ChatColor.YELLOW + "Tag: " + ChatColor.WHITE + "[" + clan.getTag() + "]");
            player.sendMessage(ChatColor.YELLOW + "Total de membros: " + ChatColor.WHITE + clan.getMembers().size());
            return true;
        }

        if (args[0].equalsIgnoreCase("log") || args[0].equalsIgnoreCase("historico")
                || args[0].equalsIgnoreCase("histórico")) {
            if (!hasPermission(player, "dclansystem.command.log", config, prefix)) {
                return true;
            }
            return showHistory(player, args, config, prefix);
        }

        if (args[0].equalsIgnoreCase("h") || args[0].equalsIgnoreCase("base")
                || args[0].equalsIgnoreCase("home")) {
            return handleHome(player, args, config, prefix);
        }

        if (args[0].equalsIgnoreCase("convidar")) {
            if (!hasPermission(player, "dclansystem.command.invite", config, prefix)) {
                return true;
            }
            Clan clan = clanManager.getPlayerClan(player.getUniqueId());
            if (clan == null) {
                String rawMsg = config.getString("messages.clan.not-in-clan", "&cVocê não pertence a nenhum clã.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            if (!clan.getLeader().equals(player.getUniqueId())) {
                String rawMsg = config.getString("messages.clan.not-leader", "&cApenas o líder do clã pode convidar jogadores.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cUso correto: /clan convidar <jogador>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                String rawMsg = config.getString("messages.clan.player-offline", "&cJogador não encontrado ou offline.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            if (clanManager.getPlayerClan(target.getUniqueId()) != null) {
                String rawMsg = config.getString("messages.clan.target-in-clan", "&cEste jogador já pertence a um clã.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            clanManager.invitePlayer(target.getUniqueId(), clan.getName());

            String sentMsg = config.getString("messages.clan.invite-sent", "&aConvite enviado para &e{player}&a!")
                    .replace("{player}", target.getName());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + sentMsg));

            String receivedMsg = config.getString("messages.clan.invite-received", "&aVocê recebeu um convite para entrar no clã &e[{tag}] {name}&a! Use &e/clan aceitar &apara entrar.")
                    .replace("{tag}", clan.getTag())
                    .replace("{name}", clan.getName());
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + receivedMsg));
            return true;
        }

        if (args[0].equalsIgnoreCase("aceitar")) {
            if (!hasPermission(player, "dclansystem.command.accept", config, prefix)) {
                return true;
            }
            if (clanManager.getPlayerClan(player.getUniqueId()) != null) {
                String rawMsg = config.getString("messages.clan.already-in-clan", "&cVocê já pertence a um clã.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            Clan clan = clanManager.acceptInvite(player.getUniqueId());
            if (clan == null) {
                String rawMsg = config.getString("messages.clan.no-invite", "&cVocê não possui nenhum convite pendente ou o clã não existe mais.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            String joinedMsg = config.getString("messages.clan.joined", "&aVocê entrou no clã &e[{tag}] {name}&a!")
                    .replace("{tag}", clan.getTag())
                    .replace("{name}", clan.getName());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + joinedMsg));
            return true;
        }

        if (args[0].equalsIgnoreCase("sair")) {
            if (!hasPermission(player, "dclansystem.command.leave", config, prefix)) {
                return true;
            }
            Clan clan = clanManager.getPlayerClan(player.getUniqueId());
            if (clan == null) {
                String rawMsg = config.getString("messages.clan.not-in-clan", "&cVocê não pertence a nenhum clã.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            if (clan.getLeader().equals(player.getUniqueId())) {
                String rawMsg = config.getString("messages.clan.leader-cannot-leave", "&cVocê é o líder do clã. Use &e/clan deletar &cpara desfazê-lo.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            clanManager.leaveClan(player.getUniqueId());
            String leftMsg = config.getString("messages.clan.left", "&cVocê saiu do clã &e{name}&c.")
                    .replace("{name}", clan.getName());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + leftMsg));
            return true;
        }

        if (args[0].equalsIgnoreCase("deletar")) {
            if (!hasPermission(player, "dclansystem.command.disband", config, prefix)) {
                return true;
            }
            Clan clan = clanManager.getPlayerClan(player.getUniqueId());
            if (clan == null) {
                String rawMsg = config.getString("messages.clan.not-in-clan", "&cVocê não pertence a nenhum clã.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            if (!clan.getLeader().equals(player.getUniqueId())) {
                String rawMsg = config.getString("messages.clan.not-leader", "&cApenas o líder pode deletar o clã.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + rawMsg));
                return true;
            }

            clanManager.deleteClan(clan.getName());
            String disbandedMsg = config.getString("messages.clan.disbanded", "&cO seu clã foi desfeito.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + disbandedMsg));
            return true;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cSubcomando inválido. Use /clan para abrir o menu."));
        return true;
    }

    private boolean hasPermission(Player player, String permission, FileConfiguration config, String prefix) {
        if (player.hasPermission(permission)) {
            return true;
        }
        String message = config.getString("messages.no-permission", "&cVocê não tem permissão para executar este comando.");
        sendConfiguredMessage(player, prefix, message);
        return false;
    }

    private void sendConfiguredMessage(Player player, String prefix, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    private boolean handleHome(Player player, String[] args, FileConfiguration config, String prefix) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            sendConfiguredMessage(player, prefix, config.getString("messages.clan.not-in-clan",
                    "&cVocê não pertence a nenhum clã."));
            return true;
        }
        if (args.length > 1 && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("definir"))) {
            if (args.length > 2) {
                sendConfiguredMessage(player, prefix, config.getString("messages.home.usage",
                        "&cUse /clan h [set]."));
                return true;
            }
            if (!hasPermission(player, "dclansystem.command.sethome", config, prefix)) {
                return true;
            }
            if (!clan.getLeader().equals(player.getUniqueId())) {
                sendConfiguredMessage(player, prefix, config.getString("messages.clan.not-leader",
                        "&cApenas o líder do clã pode executar esta ação."));
                return true;
            }
            double cost = Math.max(0.0, config.getDouble("home.set-cost", 0.0));
            if (cost > 0.0 && !economyHook.isAvailable()) {
                sendConfiguredMessage(player, prefix, config.getString("messages.economy-unavailable",
                        "&cA economia não está disponível. Avise um administrador."));
                return true;
            }
            if (!economyHook.hasFunds(player, cost)) {
                sendConfiguredMessage(player, prefix, config.getString("messages.home.insufficient-funds",
                        "&cVocê precisa de {amount} para definir a home.")
                        .replace("{amount}", economyHook.format(cost)));
                return true;
            }
            if (!economyHook.withdraw(player, cost)) {
                sendConfiguredMessage(player, prefix, config.getString("messages.home.insufficient-funds",
                        "&cVocê precisa de {amount} para definir a home.")
                        .replace("{amount}", economyHook.format(cost)));
                return true;
            }
            if (clanManager.setHome(clan.getName(), player.getLocation())) {
                sendConfiguredMessage(player, prefix, config.getString("messages.home.set",
                        "&aHome do clã definida na sua localização."));
            } else {
                economyHook.refund(player, cost);
                sendConfiguredMessage(player, prefix, config.getString("messages.home.set-failed",
                        "&cNão foi possível definir a home."));
            }
            return true;
        }
        if (!hasPermission(player, "dclansystem.command.home", config, prefix)) {
            return true;
        }
        if (args.length > 1) {
            sendConfiguredMessage(player, prefix, config.getString("messages.home.usage",
                    "&cUse /clan h [set]."));
            return true;
        }
        if (clan.getHome() == null) {
            sendConfiguredMessage(player, prefix, config.getString("messages.home.not-set",
                    "&cO clã ainda não definiu uma home."));
            return true;
        }
        homeManager.teleportHome(player, clan);
        return true;
    }

    private boolean showHistory(Player player, String[] args, FileConfiguration config, String prefix) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            sendConfiguredMessage(player, prefix, config.getString("messages.clan.not-in-clan",
                    "&cVocê não pertence a nenhum clã."));
            return true;
        }
        if (config.getBoolean("history.leader-only", true)
                && !clan.getLeader().equals(player.getUniqueId())) {
            sendConfiguredMessage(player, prefix, config.getString("messages.history.leader-only",
                    "&cApenas o líder pode consultar o histórico do clã."));
            return true;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException exception) {
                sendConfiguredMessage(player, prefix, config.getString("messages.history.invalid-page",
                        "&cNúmero de página inválido."));
                return true;
            }
        }
        if (args.length > 2) {
            sendConfiguredMessage(player, prefix, config.getString("messages.history.invalid-page",
                    "&cNúmero de página inválido."));
            return true;
        }
        int pageSize = Math.max(1, config.getInt("history.entries-per-page", 8));
        int totalPages = Math.max(1, (int) Math.ceil(clan.getHistory().size() / (double) pageSize));
        if (page > totalPages) {
            sendConfiguredMessage(player, prefix, config.getString("messages.history.invalid-page",
                    "&cNúmero de página inválido."));
            return true;
        }
        String title = config.getString("messages.history.title", "&6Histórico do clã &e{clan} &7({page}/{pages})")
                .replace("{clan}", clan.getName()).replace("{page}", String.valueOf(page))
                .replace("{pages}", String.valueOf(totalPages));
        sendConfiguredMessage(player, prefix, title);

        if (clan.getHistory().isEmpty()) {
            sendConfiguredMessage(player, prefix, config.getString("messages.history.empty",
                    "&7Ainda não há registros no histórico."));
            return true;
        }

        SimpleDateFormat dateFormat;
        try {
            dateFormat = new SimpleDateFormat(config.getString("history.date-format", "dd/MM/yyyy HH:mm:ss"),
                    Locale.getDefault());
        } catch (IllegalArgumentException exception) {
            ClanSystem.getPlugin(ClanSystem.class).getLogger().warning(
                    "Formato history.date-format inválido; usando dd/MM/yyyy HH:mm:ss.");
            dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        }
        List<ClanLogEntry> entries = clan.getHistory();
        int start = entries.size() - (page * pageSize);
        for (int index = entries.size() - 1 - ((page - 1) * pageSize); index >= Math.max(0, start); index--) {
            ClanLogEntry entry = entries.get(index);
            String memberName = getPlayerName(entry.getPlayer());
            String actorName = entry.getActor() == null ? "-" : getPlayerName(entry.getActor());
            String actionKey = entry.getAction().toLowerCase(Locale.ROOT);
            String action = config.getString("history.actions." + actionKey, actionKey);
            String line = config.getString("messages.history.entry",
                    "&7{date} &e{player} &f{action} &7por &e{actor}")
                    .replace("{date}", dateFormat.format(new Date(entry.getTimestamp())))
                    .replace("{player}", memberName).replace("{action}", action).replace("{actor}", actorName);
            sendConfiguredMessage(player, prefix, line);
        }
        return true;
    }

    private String getPlayerName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString() : name;
    }

    private boolean showTop(CommandSender sender, String[] args, FileConfiguration config, String prefix) {
        if (!sender.hasPermission("dclansystem.command.top")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix
                    + config.getString("messages.no-permission", "&cVocê não tem permissão para executar este comando.")));
            return true;
        }
        String category = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "membros";
        if (category.equals("members")) {
            category = "membros";
        } else if (category.equals("activity")) {
            category = "atividade";
        }
        if (!category.equals("membros") && !category.equals("online") && !category.equals("atividade")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix
                    + config.getString("messages.top.usage", "&cUse /clan top [membros|online|atividade].")));
            return true;
        }
        String title = config.getString("messages.top.title", "&6Top clãs por {category}")
                .replace("{category}", config.getString("messages.top.categories." + category, category));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + title));
        List<String> lines = config.getStringList("messages.top.lines");
        if (lines.isEmpty()) {
            lines = Collections.singletonList("&e{position}. &a[{tag}] {clan} &7- &f{value}");
        }
        List<Clan> topClans = clanManager.getTopClans(category);
        int limit = Math.max(1, config.getInt("top.entries", 10));
        int count = Math.min(limit, topClans.size());
        if (count == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix
                    + config.getString("messages.top.empty", "&7Ainda não existem clãs no ranking.")));
            return true;
        }
        for (int i = 0; i < count; i++) {
            Clan clan = topClans.get(i);
            int online = clanManager.getOnlineMemberCount(clan);
            String value;
            if (category.equals("online")) {
                value = String.valueOf(online);
            } else if (category.equals("atividade")) {
                value = String.valueOf(clan.getActivityScore());
            } else {
                value = String.valueOf(clan.getMembers().size());
            }
            for (String line : lines) {
                line = line.replace("{position}", String.valueOf(i + 1))
                        .replace("{clan}", clan.getName()).replace("{tag}", clan.getTag())
                        .replace("{members}", String.valueOf(clan.getMembers().size()))
                        .replace("{online}", String.valueOf(online))
                        .replace("{score}", String.valueOf(clan.getActivityScore()))
                        .replace("{value}", value);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + line));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("criar", "info", "convidar", "aceitar", "sair", "deletar",
                    "log", "h", "base", "top");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("h") || args[0].equalsIgnoreCase("base"))) {
            return Arrays.asList("set");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return Arrays.asList("membros", "online", "atividade");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("convidar")) {
            return null;
        }
        return Collections.emptyList();
    }
}