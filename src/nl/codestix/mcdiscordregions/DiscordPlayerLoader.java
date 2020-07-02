package nl.codestix.mcdiscordregions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DiscordPlayerLoader {

    private YamlConfiguration config;
    private File file;
    private final String DISCORD_USERS_PATH = "discord-user-ids";
    private HashMap<UUID, Long> discordTagsPerPlayer = new HashMap<>();

    public DiscordPlayerLoader(File file) throws IOException, InvalidConfigurationException {
        config = new YamlConfiguration();
        this.file = file;
        if (file.exists())
        {
            config.load(file);

            ConfigurationSection section = config.getConfigurationSection(DISCORD_USERS_PATH);
            if (section != null) {
                Set<String> keys = section.getKeys(false);
                for(String key : keys)
                    discordTagsPerPlayer.put(UUID.fromString(key), (long)section.get(key));
            }
        }
    }

    public boolean containsPlayer(UUID playerId) {
        synchronized(discordTagsPerPlayer) {
            return discordTagsPerPlayer.containsKey(playerId);
        }
    }

    public void setDiscordUser(UUID playerId, long userId) {
        synchronized(discordTagsPerPlayer) {
            discordTagsPerPlayer.put(playerId, userId);
        }
    }

    public long getDiscordUser(UUID playerId) {
        synchronized(discordTagsPerPlayer) {
            return discordTagsPerPlayer.get(playerId);
        }
    }

    public void save() throws IOException {
        ConfigurationSection section = config.getConfigurationSection(DISCORD_USERS_PATH);
        synchronized(discordTagsPerPlayer) {
            Set<Map.Entry<UUID, Long>> entries = discordTagsPerPlayer.entrySet();
            for(Map.Entry<UUID, Long> entry : entries)
                section.set(entry.getKey().toString(), entry.getValue());
        }

        config.save(file);
    }
}
