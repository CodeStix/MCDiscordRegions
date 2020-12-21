package nl.codestix.mcdiscordregions;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.codestix.mcdiscordregions.command.DiscordRegionsCommand;
import nl.codestix.mcdiscordregions.listener.PlayerListener;
import nl.codestix.mcdiscordregions.listener.RegionListener;
import nl.codestix.mcdiscordregions.websocket.WebSocketConnection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class MCDiscordRegionsPlugin extends JavaPlugin implements DiscordEvents {

    public RegionListener regionListener;
    public PlayerListener playerListener;
    public DiscordConnection connection;
    public DiscordRegionsCommand command;
    public StringFlag discordChannelFlag = new StringFlag("discord-channel");
    public String serverId;
    public int reconnectIntervalTicks = 20 * 8;

    public static final String CONFIG_HOST = "host";
    public static final String CONFIG_ID = "id";
    public static final String CONFIG_REQUIRE_DISCORD = "require-discord";
    public static final String CONFIG_REQUIRE_DISCORD_LEAVE_MESSAGE = "require-discord-leave-message";
    public static final String CONFIG_REQUIRE_DISCORD_REGISTER_MESSAGE = "require-discord-register-message";
    public static final String CONFIG_REQUIRE_DISCORD_JOIN_MESSAGE = "require-discord-join-message";
    public static final String CONFIG_HAS_DISCORD_REGISTER_MESSAGE = "has-discord-register-message";
    public static final String CONFIG_HAS_DISCORD_JOIN_MESSAGE = "has-discord-join-message";

    private WorldGuardHandler.Factory worldGuardHandlerFactory;
    private static MCDiscordRegionsPlugin instance;
    public static MCDiscordRegionsPlugin getInstance() {
        return instance;
    }

    private Integer reconnectTask = null;

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
        String host = getConfig().getString(CONFIG_HOST, "ws://localhost:8080");
        getLogger().info("Connecting to Discord Regions bot at " + host);
        try {
            connection = new WebSocketConnection(new URI(host), this, serverId);
        } catch (URISyntaxException e) {
            getLogger().severe("Could not connect to Discord bot, invalid host: " + host);
            getPluginLoader().disablePlugin(this);
            return;
        } catch(InterruptedException | WebsocketNotConnectedException e) {
            getLogger().severe("Could not connect to Discord bot: " + e.getMessage());
            getPluginLoader().disablePlugin(this);
            return;
        }

        getLogger().info("Create a Discord category (in a server with the Minecraft Regions bot) with the following name (including hashtags) to connect it with this server: ###" + serverId);

        // Register WorldGuard handler
        worldGuardHandlerFactory = new WorldGuardHandler.Factory();
        WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(worldGuardHandlerFactory, null);

        // Configure event listeners
        regionListener = new RegionListener(this);
        Bukkit.getPluginManager().registerEvents(regionListener, this);
        playerListener = new PlayerListener(this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);

        // Configure commands
        command = new DiscordRegionsCommand(this);
        getCommand("dregion").setExecutor(command);

        getLogger().info("Is configured correctly!");
        instance = this;
    }

    @Override
    public void onDisable() {
        if (connection != null)
            connection.close();
        saveConfig();
        WorldGuard.getInstance().getPlatform().getSessionManager().unregisterHandler(worldGuardHandlerFactory);
    }

    @Override
    public void userLeft(UUID uuid) {
        if (getConfig().getBoolean(CONFIG_REQUIRE_DISCORD)) {
            Player player = getServer().getPlayer(uuid);
            if (player != null)
            {
                // Kick player on main thread
                getServer().getScheduler().scheduleSyncDelayedTask(this, () -> player.kickPlayer(getConfig().getString(CONFIG_REQUIRE_DISCORD_LEAVE_MESSAGE)));
                getLogger().info("Kicked player " + player.getName());
            }
        }
    }

    @Override
    public void userJoined(UUID uuid, String regionName) {
        Player pl = getServer().getPlayer(uuid);
        if (pl == null)
            return;

        getLogger().info("Player " + pl.getName() + " joined discord channel " + regionName);


        // User joined Discord channel, send the join message back, this will cause channel move and un-deafen
        String realRegionName = WorldGuardHandler.queryFlag(pl, discordChannelFlag);
        connection.playerJoin(pl.getUniqueId(), realRegionName);
    }

    @Override
    public void userRequired(UUID uuid, String userBindKey) {
        Player player = getServer().getPlayer(uuid);
        if (player == null)
            return;

        boolean required = getConfig().getBoolean(CONFIG_REQUIRE_DISCORD);

        String message;
        if (userBindKey == null) {
            // User is already bound, just needs to be in a Discord channel
            message = getConfig().getString(required ? CONFIG_REQUIRE_DISCORD_JOIN_MESSAGE : CONFIG_HAS_DISCORD_JOIN_MESSAGE);
        }
        else {
            // User is not yet bound to a Minecraft account
            message = String.format(getConfig().getString(required ? CONFIG_REQUIRE_DISCORD_REGISTER_MESSAGE: CONFIG_HAS_DISCORD_REGISTER_MESSAGE), userBindKey);
        }

        if (required)
            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> player.kickPlayer(message));
        else
            player.sendMessage(message);
    }

    @Override
    public void userBound(UUID uuid) {
        Player player = getServer().getPlayer(uuid);
        if (player == null)
            return;

        player.sendMessage("§aAwesome, your Minecraft account is now connected to your Discord account. You only have to do this once for all servers that use this feature. Enjoy!");

        String realRegionName = WorldGuardHandler.queryFlag(player, discordChannelFlag);
        connection.playerJoin(player.getUniqueId(), realRegionName);
    }

    @Override
    public void onDisconnect() {
        if (reconnectTask == null) {
            getLogger().warning("Connection lost with Discord bot. Reconnecting...");
            reconnectTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> connection.reconnect(), 20, reconnectIntervalTicks);
        }
    }

    @Override
    public void onConnect() {
        if (reconnectTask != null) {
            Bukkit.getScheduler().cancelTask(reconnectTask);
            reconnectTask = null;
        }
        getLogger().info("Connected to Discord bot.");
    }
}
