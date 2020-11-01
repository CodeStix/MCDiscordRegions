package nl.codestix.mcdiscordregions;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import nl.codestix.mcdiscordregions.command.DiscordRegionsCommand;
import nl.codestix.mcdiscordregions.listener.PlayerListener;
import nl.codestix.mcdiscordregions.listener.RegionListener;
import nl.codestix.mcdiscordregions.websocket.WebSocketConnection;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class MCDiscordRegionsPlugin extends JavaPlugin {

    public RegionListener regionListener;
    public PlayerListener playerListener;

    private static final String CONFIG_HOST = "host";
    private static final String CONFIG_ID = "id";
    private static final String CONFIG_CATEGORY = "category";
    private static final String CONFIG_ENTRY_CHANNEL = "entry-channel-name";
    private static final String CONFIG_USE_WHITELIST = "use-whitelist";
    private static final String CONFIG_KICK_DISCORD_LEAVE = "kick-on-discord-leave";
    private static final String CONFIG_KICK_DISCORD_LEAVE_MESSAGE = "kick-on-discord-leave-message";
    private static final String CONFIG_MIN_MOVE_INTERVAL = "min-move-interval";

    private StringFlag discordChannelFlag = new StringFlag("discord-channel");
    private WorldGuardHandler.Factory worldGuardHandlerFactory;
    private DiscordConnection connection;
    private String serverId;

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
        if (getConfig().contains(CONFIG_ID)) {
            serverId =  getConfig().getString(CONFIG_ID);
        }
        else {
            serverId = UUID.randomUUID().toString();
            getConfig().set(CONFIG_ID, serverId);
        }

        // Connect to Discord Bot
        String host = getConfig().getString(CONFIG_HOST, "ws://172.18.168.254:8080");
        getLogger().info("Connecting to Discord Regions bot at " + host);
        try {
            WebSocketConnection ws = new WebSocketConnection(new URI(host), serverId);
            ws.connectBlocking();
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

        getLogger().info("Create a Discord category (in a server with the Minecraft Regions bot) with the following name (including hashtags) to connect it with this server: ###" + serverId);

        // Register WorldGuard handler
        worldGuardHandlerFactory = new WorldGuardHandler.Factory();
        WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(worldGuardHandlerFactory, null);

        // Configure event listeners
        regionListener = new RegionListener(discordChannelFlag, connection);
        Bukkit.getPluginManager().registerEvents(regionListener, this);
        playerListener = new PlayerListener(connection);
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
}
