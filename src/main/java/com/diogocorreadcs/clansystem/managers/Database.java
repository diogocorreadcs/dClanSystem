package com.diogocorreadcs.clansystem.managers;

import com.diogocorreadcs.clansystem.models.Clan;
import com.diogocorreadcs.clansystem.models.ClanLogEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Database {

    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration config;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    public void initDatabase() {
        if (!plugin.getDataFolder().exists()) {
            boolean created = plugin.getDataFolder().mkdirs();
            if (!created && !plugin.getDataFolder().exists()) {
                plugin.getLogger().warning("Failed to create data folder.");
            }
        }

        file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                if (!created && !file.exists()) {
                    plugin.getLogger().warning("Failed to create data.yml file.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveClan(Clan clan) {
        String path = "clans." + clan.getName().toLowerCase();
        config.set(path + ".name", clan.getName());
        config.set(path + ".tag", clan.getTag());
        config.set(path + ".leader", clan.getLeader().toString());

        List<String> memberList = new ArrayList<>();
        for (UUID uuid : clan.getMembers()) {
            memberList.add(uuid.toString());
        }
        config.set(path + ".members", memberList);
        config.set(path + ".activity-score", clan.getActivityScore());

        Location home = clan.getHome();
        if (home == null || home.getWorld() == null) {
            config.set(path + ".home", null);
        } else {
            config.set(path + ".home.world", home.getWorld().getName());
            config.set(path + ".home.x", home.getX());
            config.set(path + ".home.y", home.getY());
            config.set(path + ".home.z", home.getZ());
            config.set(path + ".home.yaw", home.getYaw());
            config.set(path + ".home.pitch", home.getPitch());
        }

        List<Map<String, Object>> history = new ArrayList<>();
        for (ClanLogEntry entry : clan.getHistory()) {
            Map<String, Object> record = new HashMap<>();
            record.put("timestamp", entry.getTimestamp());
            record.put("action", entry.getAction());
            record.put("player", entry.getPlayer().toString());
            record.put("actor", entry.getActor() == null ? "" : entry.getActor().toString());
            history.add(record);
        }
        config.set(path + ".history", history);

        saveFile();
    }

    public void removeClan(String name) {
        config.set("clans." + name.toLowerCase(), null);
        saveFile();
    }

    public Map<String, Clan> loadClans() {
        Map<String, Clan> loadedClans = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("clans");
        if (section == null) return loadedClans;

        for (String key : section.getKeys(false)) {
            String name = config.getString("clans." + key + ".name");
            String tag = config.getString("clans." + key + ".tag");
            String leaderStr = config.getString("clans." + key + ".leader");
            List<String> memberList = config.getStringList("clans." + key + ".members");

            if (name != null && tag != null && leaderStr != null) {
                UUID leader = UUID.fromString(leaderStr);
                Clan clan = new Clan(name, tag, leader);

                for (String memberStr : memberList) {
                    try {
                        UUID memberUuid = UUID.fromString(memberStr);
                        clan.addMember(memberUuid);
                    } catch (IllegalArgumentException ignored) {}
                }

                clan.setActivityScore(config.getLong("clans." + key + ".activity-score", 0L));
                String homePath = "clans." + key + ".home";
                String worldName = config.getString(homePath + ".world");
                World world = worldName == null ? null : Bukkit.getWorld(worldName);
                if (world != null && config.contains(homePath + ".x")
                        && config.contains(homePath + ".y") && config.contains(homePath + ".z")) {
                    clan.setHome(new Location(world,
                            config.getDouble(homePath + ".x"),
                            config.getDouble(homePath + ".y"),
                            config.getDouble(homePath + ".z"),
                            (float) config.getDouble(homePath + ".yaw"),
                            (float) config.getDouble(homePath + ".pitch")));
                }

                for (Map<?, ?> record : config.getMapList("clans." + key + ".history")) {
                    try {
                        long timestamp = Long.parseLong(String.valueOf(record.get("timestamp")));
                        String action = String.valueOf(record.get("action"));
                        UUID memberUuid = UUID.fromString(String.valueOf(record.get("player")));
                        Object actorValue = record.get("actor");
                        UUID actorUuid = actorValue == null || String.valueOf(actorValue).isEmpty()
                                ? null : UUID.fromString(String.valueOf(actorValue));
                        clan.addHistoryEntry(new ClanLogEntry(timestamp, action, memberUuid, actorUuid));
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Ignorando registro de histórico inválido do clã " + name + ".");
                    }
                }

                loadedClans.put(name.toLowerCase(), clan);
            }
        }
        return loadedClans;
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
}