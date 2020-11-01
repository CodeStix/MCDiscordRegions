package nl.codestix.mcdiscordregions;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import nl.codestix.mcdiscordregions.command.DiscordRegionsCommand;
import nl.codestix.mcdiscordregions.listener.PlayerListener;
import nl.codestix.mcdiscordregions.listener.RegionListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MCDiscordRegionsPlugin extends JavaPlugin {

    public RegionListener regionListener;
    public PlayerListener playerListener;

    private static final String CONFIG_DISCORD_CATEGORY = "discord.category";
    private static final String CONFIG_DISCORD_ENTRY_CHANNEL = "discord.entry-channel-name";
    private static final String CONFIG_MINECRAFT_USE_WHITELIST = "minecraft.use-whitelist";
    private static final String CONFIG_MINECRAFT_KICK_DISCORD_LEAVE = "minecraft.kick-on-discord-leave";
    private static final String CONFIG_MINECRAFT_KICK_DISCORD_LEAVE_MESSAGE = "minecraft.kick-on-discord-leave-message";
    private static final String CONFIG_DISCORD_MIN_MOVE_INTERVAL = "discord.min-move-interval";

    private StringFlag discordChannelFlag;
    private WorldGuardHandler.Factory worldGuardHandlerFactory;

    private static MCDiscordRegionsPlugin instance;

    public static MCDiscordRegionsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        FlagRegistry reg = WorldGuard.getInstance().getFlagRegistry();
        Flag<?> f = reg.get(discordChannelFlag.getName());
        if (f == null) {
            discordChannelFlag = new StringFlag("discord-channel");
            reg.register(discordChannelFlag);
        }
        else {
            discordChannelFlag = (StringFlag)f;
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Register WorldGuard handler
        worldGuardHandlerFactory = new WorldGuardHandler.Factory();
        WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(worldGuardHandlerFactory, null);

        // Configure event listeners
        regionListener = new RegionListener(this, discordChannelFlag);
        Bukkit.getPluginManager().registerEvents(regionListener, this);
        playerListener = new PlayerListener();
        Bukkit.getPluginManager().registerEvents(playerListener, this);

        // Configure commands
        getCommand("dregion").setExecutor(new DiscordRegionsCommand(this));

        getLogger().info("Is configured correctly!");
        instance = this;
    }

    @Override
    public void onDisable() {
        saveConfig();
        WorldGuard.getInstance().getPlatform().getSessionManager().unregisterHandler(worldGuardHandlerFactory);
    }

    @Override
    public void saveConfig() {
        FileConfiguration c = getConfig();
        // ...
        super.saveConfig();
    }
}
