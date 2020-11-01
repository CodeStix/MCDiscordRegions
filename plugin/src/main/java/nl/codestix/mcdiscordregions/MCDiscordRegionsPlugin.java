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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

public class MCDiscordRegionsPlugin extends JavaPlugin {

    public RegionListener regionListener;
    public PlayerListener playerListener;

    private static final String CONFIG_HOST = "host";
    private static final String CONFIG_CATEGORY = "category";
    private static final String CONFIG_ENTRY_CHANNEL = "entry-channel-name";
    private static final String CONFIG_USE_WHITELIST = "use-whitelist";
    private static final String CONFIG_KICK_DISCORD_LEAVE = "kick-on-discord-leave";
    private static final String CONFIG_KICK_DISCORD_LEAVE_MESSAGE = "kick-on-discord-leave-message";
    private static final String CONFIG_MIN_MOVE_INTERVAL = "min-move-interval";

    private StringFlag discordChannelFlag = new StringFlag("discord-channel");
    private WorldGuardHandler.Factory worldGuardHandlerFactory;
    private DiscordConnection connection;

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


        String host = getConfig().getString(CONFIG_HOST, "ws://172.18.168.254:8080");
        getLogger().info("Connecting to Discord regions server at " + host);
        try {
            WebSocketConnection ws = new WebSocketConnection(new URI(host));
            ws.connectBlocking();
            ws.send("test");
            connection = ws;
        } catch (URISyntaxException e) {
            getLogger().severe("Could not connect to websocket, invalid host: " + host);
            getPluginLoader().disablePlugin(this);
            return;
        } catch(InterruptedException e) {
            getLogger().severe("Could not connect to websocket: " + e.getMessage());
            getPluginLoader().disablePlugin(this);
            return;
        }

        getLogger().info("Is configured correctly!");
        instance = this;
    }

    @Override
    public void onDisable() {
        saveConfig();
        WorldGuard.getInstance().getPlatform().getSessionManager().unregisterHandler(worldGuardHandlerFactory);
    }
}
