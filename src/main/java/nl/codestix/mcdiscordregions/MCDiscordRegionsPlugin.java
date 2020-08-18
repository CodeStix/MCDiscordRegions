package nl.codestix.mcdiscordregions;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import nl.codestix.mcdiscordregions.command.DiscordRegionsCommand;
import nl.codestix.mcdiscordregions.database.ConfigDiscordPlayerDatabase;
import nl.codestix.mcdiscordregions.listener.DiscordPlayerListener;
import nl.codestix.mcdiscordregions.listener.PlayerListener;
import nl.codestix.mcdiscordregions.listener.RegionListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;

public class MCDiscordRegionsPlugin extends JavaPlugin {

    public ConfigDiscordPlayerDatabase playerDatabase;
    public DiscordBot bot;

    public RegionListener regionListener;
    public DiscordPlayerListener discordPlayerListener;
    public PlayerListener playerListener;

    private static final String CONFIG_DISCORD_BOT_TOKEN = "discord.token";
    private static final String CONFIG_DISCORD_SERVER = "discord.server";
    private static final String CONFIG_DISCORD_CATEGORY = "discord.category";
    private static final String CONFIG_DISCORD_ENTRY_CHANNEL = "discord.entry-channel-name";
    private static final String CONFIG_MINECRAFT_USE_WHITELIST = "minecraft.use-whitelist";
    private static final String CONFIG_MINECRAFT_KICK_DISCORD_LEAVE = "minecraft.kick-on-discord-leave";
    private static final String CONFIG_MINECRAFT_KICK_DISCORD_LEAVE_MESSAGE = "minecraft.kick-on-discord-leave-message";
    private static final String CONFIG_DISCORD_MIN_MOVE_INTERVAL = "discord.min-move-interval";
    private static final String CONFIG_AUTO_SAVE = "auto-save-config";

    private StringFlag discordChannelFlag = new StringFlag("discord-channel");
    private WorldGuardHandler.Factory worldGuardHandlerFactory;
    private boolean configAutoSave = false;

    private static MCDiscordRegionsPlugin instance;

    public static MCDiscordRegionsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        FlagRegistry reg = WorldGuard.getInstance().getFlagRegistry();
        Flag<?> f = reg.get(discordChannelFlag.getName());
        if (f == null)
            reg.register(discordChannelFlag);
        else
            discordChannelFlag = (StringFlag)f;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        worldGuardHandlerFactory = new WorldGuardHandler.Factory();
        WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(worldGuardHandlerFactory, null);

        try {
            playerDatabase = new ConfigDiscordPlayerDatabase(new File(getDataFolder(), "players.yml"));
        }
        catch(IOException | InvalidConfigurationException ex) {
            getLogger().warning("Could not load players.yml, please ensure correct yml format or delete the file to recreate it.");
            getPluginLoader().disablePlugin(this);
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
            getPluginLoader().disablePlugin(this);
            return;
        }
        catch(InterruptedException ex) {
            getLogger().warning("Login got interrupted: " + ex);
            getPluginLoader().disablePlugin(this);
            return;
        }
        catch(LoginException ex) {
            getLogger().warning("Invalid token: " + ex);
            getPluginLoader().disablePlugin(this);
            return;
        }

        bot.playerMinimumMoveInterval = getConfig().getInt(CONFIG_DISCORD_MIN_MOVE_INTERVAL, bot.playerMinimumMoveInterval);

        regionListener = new RegionListener(this, bot, discordChannelFlag);
        Bukkit.getPluginManager().registerEvents(regionListener, this);

        discordPlayerListener = new DiscordPlayerListener(this, bot, discordChannelFlag);
        discordPlayerListener.setUseWhitelist(getConfig().getBoolean(CONFIG_MINECRAFT_USE_WHITELIST, false));
        discordPlayerListener.kickOnDiscordLeave = getConfig().getBoolean(CONFIG_MINECRAFT_KICK_DISCORD_LEAVE, true);
        discordPlayerListener.kickOnDiscordLeaveMessage = getConfig().getString(CONFIG_MINECRAFT_KICK_DISCORD_LEAVE_MESSAGE, "Not registered.");
        bot.setDiscordPlayerEventsListener(discordPlayerListener);

        playerListener = new PlayerListener(bot, discordPlayerListener);
        Bukkit.getPluginManager().registerEvents(playerListener, this);

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
            });
        }

        getCommand("dregion").setExecutor(new DiscordRegionsCommand(this));

        configAutoSave = getConfig().getBoolean(CONFIG_AUTO_SAVE, true);

        getLogger().info("Is configured correctly!");
    }

    @Override
    public void onDisable() {
        if (bot != null) {
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
        if (configAutoSave)
            saveConfig();
        WorldGuard.getInstance().getPlatform().getSessionManager().unregisterHandler(worldGuardHandlerFactory);
    }

    @Override
    public void saveConfig() {
        FileConfiguration c = getConfig();
        if (bot != null) {
            c.set(CONFIG_DISCORD_SERVER, bot.getGuild() == null ? null : bot.getGuild().getIdLong());
            c.set(CONFIG_DISCORD_CATEGORY, bot.getCategory() == null ? null : bot.getCategory().getName());
            c.set(CONFIG_DISCORD_ENTRY_CHANNEL, bot.getEntryChannel() == null ? null : bot.getEntryChannel().getName());
            c.set(CONFIG_DISCORD_MIN_MOVE_INTERVAL, bot.playerMinimumMoveInterval);
        }
        if (discordPlayerListener != null) {
            c.set(CONFIG_MINECRAFT_USE_WHITELIST, discordPlayerListener.getUseWhitelist());
            c.set(CONFIG_MINECRAFT_KICK_DISCORD_LEAVE, discordPlayerListener.kickOnDiscordLeave);
            c.set(CONFIG_MINECRAFT_KICK_DISCORD_LEAVE_MESSAGE, discordPlayerListener.kickOnDiscordLeaveMessage);
        }
        c.set(CONFIG_AUTO_SAVE, configAutoSave);
        super.saveConfig();
    }
}
