package nl.codestix.mcdiscordregions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigDiscordPlayerDatabase implements IDiscordPlayerDatabase {

    private YamlConfiguration config;
    private File file;
    private HashMap<Long, UUID> players = new HashMap<>();

    public Integer maxPlayers;
    public boolean acceptNewPlayers;

    private static final String CONFIG_DISCORD_PLAYERS = "discord-minecraft-players";
    private static final String CONFIG_MAX_PLAYERS = "max-players";
    private static final String CONFIG_ACCEPT_NEW_PLAYERS = "accept-new-players";

    public ConfigDiscordPlayerDatabase(File file) throws IOException, InvalidConfigurationException {
        config = new YamlConfiguration();
        this.file = file;
        if (file.exists())
        {
            config.load(file);

            ConfigurationSection section = config.getConfigurationSection(CONFIG_DISCORD_PLAYERS);
            if (section != null) {
                Set<String> keys = section.getKeys(false);
                for(String key : keys)
                    players.put(Long.parseLong(key), UUID.fromString(section.get(key).toString()));
            }
        }

        maxPlayers = (Integer)config.get(CONFIG_MAX_PLAYERS);
        acceptNewPlayers = config.getBoolean(CONFIG_ACCEPT_NEW_PLAYERS, true);
    }

    public void save() throws IOException {
        config.set(CONFIG_MAX_PLAYERS, maxPlayers);
        config.set(CONFIG_ACCEPT_NEW_PLAYERS, acceptNewPlayers);

        ConfigurationSection section = config.getConfigurationSection(CONFIG_DISCORD_PLAYERS);
        if (section == null)
            section = config.createSection(CONFIG_DISCORD_PLAYERS);
        synchronized(players) {
            Set<Map.Entry<Long, UUID>> entries = players.entrySet();
            for(Map.Entry<Long, UUID> entry : entries)
                section.set(entry.getKey().toString(), entry.getValue().toString());
        }

        config.save(file);
    }

    @Override
    public boolean putPlayer(long userId, UUID playerId) {
        synchronized(players) {
            if ((maxPlayers != null && players.size() >= maxPlayers) || !acceptNewPlayers)
                return false;
            players.put(userId, playerId);
            return true;
        }
    }

    @Override
    public boolean removePlayer(long userId) {
        synchronized (players) {
            return players.remove(userId) != null;
        }
    }

    @Override
    public UUID getPlayer(long userId) {
        synchronized (players) {
            return players.get(userId);
        }
    }

    @Override
    public int getPlayerCount() {
        return players.size();
    }
}
