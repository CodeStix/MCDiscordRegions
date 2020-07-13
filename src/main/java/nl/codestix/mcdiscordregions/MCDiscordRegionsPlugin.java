package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;

public class MCDiscordRegionsPlugin extends JavaPlugin {

    public ConfigDiscordPlayerDatabase playerDatabase;
    public RegionEvents regionEvents;
    public DiscordBot bot;

    private static final String CONFIG_DISCORD_BOT_TOKEN = "discord.token";
    private static final String CONFIG_DISCORD_SERVER = "discord.server";
    private static final String CONFIG_DISCORD_CATEGORY = "discord.category";
    private static final String CONFIG_DISCORD_ENTRY_CHANNEL = "discord.entry-channel-name";
    private static final String CONFIG_DISCORD_GLOBAL_CHANNEL = "discord.global-channel-name";
    private static final String CONFIG_DISCORD_AUTO_CREATE_CHANNELS = "discord.auto-create-channels";
    private static final String CONFIG_MINECRAFT_USE_WHITELIST = "minecraft.use-whitelist";
    private static final String CONFIG_MINECRAFT_KICK_DISCORD_LEAVE = "minecraft.kick-on-discord-leave";
    private static final String CONFIG_MINECRAFT_KICK_DISCORD_LEAVE_MESSAGE = "minecraft.kick-on-discord-leave-message";

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            playerDatabase = new ConfigDiscordPlayerDatabase(new File(getDataFolder(), "players.yml"));
        }
        catch(IOException | InvalidConfigurationException ex) {
            getLogger().warning("Could not load players.yml, please ensure correct yml format or delete the file to recreate it.");
            return;
        }

        String token = getConfig().getString(CONFIG_DISCORD_BOT_TOKEN);
        if (token == null) {
            getLogger().warning("Please enter your bot client secret in the config file of this plugin!");
            getPluginLoader().disablePlugin(this);
            return;
        }

        String guildId = getConfig().getString(CONFIG_DISCORD_SERVER);
        try {
            bot = new DiscordBot(token, guildId, playerDatabase);
        }
        catch(NullPointerException ex) {
            getLogger().warning("No discord server was found, first, invite the bot to a server, then use this plugin.");
        }
        catch(InterruptedException ex) {
            getLogger().warning("Login got interrupted: " + ex);
        }
        catch(LoginException ex) {
            getLogger().warning("Invalid token: " + ex);
            getPluginLoader().disablePlugin(this);
            return;
        }

        bot.allowCreateNewChannel = getConfig().getBoolean(CONFIG_DISCORD_AUTO_CREATE_CHANNELS, true);

        regionEvents = new RegionEvents(this);
        regionEvents.setUseWhitelist(getConfig().getBoolean(CONFIG_MINECRAFT_USE_WHITELIST, false));
        regionEvents.kickOnDiscordLeave = getConfig().getBoolean(CONFIG_MINECRAFT_KICK_DISCORD_LEAVE, true);
        regionEvents.kickOnDiscordLeaveMessage = getConfig().getString(CONFIG_MINECRAFT_KICK_DISCORD_LEAVE_MESSAGE, "Not registered.");
        bot.setDiscordPlayerEventsListener(regionEvents);
        Bukkit.getPluginManager().registerEvents(regionEvents, this);

        String categoryName = getConfig().getString(CONFIG_DISCORD_CATEGORY);
        if (bot.getGuild() != null && categoryName != null) {
            getLogger().info(String.format("Setting discord category to '%s'", categoryName));
            bot.getCategoryByNameOrCreate(categoryName, category -> {
                bot.setCategory(category);

                String entryChannelName = getConfig().getString(CONFIG_DISCORD_ENTRY_CHANNEL);
                if (entryChannelName != null) {
                    getLogger().info(String.format("Setting entry voice channel to '%s'", entryChannelName));
                    bot.setEntryChannel(entryChannelName, false);
                }

                String globalChannelName = getConfig().getString(CONFIG_DISCORD_GLOBAL_CHANNEL);
                if (globalChannelName != null) {
                    getLogger().info(String.format("Setting global voice channel to '%s'", globalChannelName));
                    bot.setGlobalChannel(globalChannelName, false);
                }
            });
        }

        getCommand("dregion").setExecutor(new DiscordRegionsCommand(this));

        getLogger().info("Is configured correctly!");
    }

    @Override
    public void onDisable() {
        if (bot != null)
        {
            bot.letAllCategoryPlayersLeave();
            bot.destroy();
        }
        if (playerDatabase != null) {
            try {
                playerDatabase.save();
            } catch (IOException ex) {
                getLogger().warning("Could not save players.yml: " + ex);
            }
        }
    }

    @Override
    public void saveConfig() {
        FileConfiguration c = getConfig();
        c.set(CONFIG_DISCORD_SERVER, bot.getGuild().getIdLong());
        c.set(CONFIG_DISCORD_CATEGORY, bot.getCategory().getName());
        c.set(CONFIG_DISCORD_ENTRY_CHANNEL, bot.getEntryChannel().getName());
        c.set(CONFIG_DISCORD_GLOBAL_CHANNEL, bot.getGlobalChannel().getName());
        c.set(CONFIG_DISCORD_AUTO_CREATE_CHANNELS, bot.allowCreateNewChannel);
        c.set(CONFIG_MINECRAFT_USE_WHITELIST, regionEvents.getUseWhitelist());
        c.set(CONFIG_MINECRAFT_KICK_DISCORD_LEAVE, regionEvents.kickOnDiscordLeave);
        c.set(CONFIG_MINECRAFT_KICK_DISCORD_LEAVE_MESSAGE, regionEvents.kickOnDiscordLeaveMessage);
        super.saveConfig();
    }
}
