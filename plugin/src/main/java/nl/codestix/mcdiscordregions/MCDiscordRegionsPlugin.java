package nl.codestix.mcdiscordregions;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import nl.codestix.mcdiscordregions.command.DiscordRegionsCommand;
import nl.codestix.mcdiscordregions.command.NoDiscordCommand;
import nl.codestix.mcdiscordregions.listener.PlayerListener;
import nl.codestix.mcdiscordregions.listener.RegionListener;
import nl.codestix.mcdiscordregions.websocket.WebSocketConnection;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MCDiscordRegionsPlugin extends JavaPlugin implements DiscordEvents {

    public RegionListener regionListener;
    public PlayerListener playerListener;
    public DiscordConnection connection;
    public DiscordRegionsCommand command;

    public static final String CONFIG_HOST = "host";
    public static final String CONFIG_ID = "id";
    public static final String CONFIG_GLOBAL_REGION = "global-region-name";
    public static final String CONFIG_USE_WHITELIST = "use-whitelist";
    public static final String CONFIG_KICK_DISCORD_LEAVE = "kick-on-discord-leave";
    public static final String CONFIG_KICK_DISCORD_LEAVE_MESSAGE = "kick-on-discord-leave-message";
    public static final String CONFIG_MIN_MOVE_INTERVAL = "min-move-interval";

    private StringFlag discordChannelFlag = new StringFlag("discord-channel");
    private WorldGuardHandler.Factory worldGuardHandlerFactory;
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
        String host = getConfig().getString(CONFIG_HOST, "ws://localhost:8080");
        getLogger().info("Connecting to Discord Regions bot at " + host);
        try {
            connection = new WebSocketConnection(new URI(host), this, serverId );
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
        String globalRegionName = getConfig().getString(CONFIG_GLOBAL_REGION, "Global");
        regionListener = new RegionListener(discordChannelFlag, connection, globalRegionName);
        Bukkit.getPluginManager().registerEvents(regionListener, this);
        playerListener = new PlayerListener(connection);
        Bukkit.getPluginManager().registerEvents(playerListener, this);

        // Configure commands
        command = new DiscordRegionsCommand(this);
        getCommand("dregion").setExecutor(command);
        getCommand("nodiscord").setExecutor(new NoDiscordCommand(connection));

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
        if (getConfig().getBoolean(CONFIG_KICK_DISCORD_LEAVE)) {
            Player player = getServer().getPlayer(uuid);
            if (player != null)
            {
                // Kick player on main thread
                getServer().getScheduler().scheduleSyncDelayedTask(this, () -> player.kickPlayer(getConfig().getString(CONFIG_KICK_DISCORD_LEAVE_MESSAGE)));
                getLogger().info("Kicked player " + player.getName());
            }
        }

    }

    @Override
    public void userJoined(UUID uuid) {
        // User joined Discord channel, send the join message back, this will cause un-deafen
        Player pl = getServer().getPlayer(uuid);
        if (pl != null) {
            connection.join(pl.getUniqueId());
            connection.regionMove(pl.getUniqueId(), regionListener.getRegionName(WorldGuardHandler.getPlayerRegion(pl)));
        }
    }

    @Override
    public void userRequired(UUID uuid, String userBindKey) {
        Player player = getServer().getPlayer(uuid);
        if (player == null)
            return;

        boolean required = getConfig().getBoolean(CONFIG_USE_WHITELIST);

        String message;
        if (userBindKey == null) {
            // User is already bound, just needs to be in a Discord channel
            if (required)
                message = "§eThis server requires you to join their Discord Minecraft channels before joining.";
            else
                message = "§eThis server makes use of Discord regions, join a Minecraft Regions Discord channel to use this feature.";

        }
        else {
            // User is not yet bound to a Minecraft account
            if (required)
                message = String.format("This server requires you to join their Discord Minecraft channels before joining. Connect your Minecraft account to Discord by entering the following code in the #minecraft-bind channel (case sensitive): §f%s", userBindKey);
            else
                message = String.format("This server makes use of Discord regions. If you want to connect your Minecraft account to Discord, enter the following code in any channel in the Discord of this Minecraft server (case sensitive): §f%s", userBindKey);
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
    }

    @Override
    public void regionGotLimited(String regionName, int limit) {
        command.regionGotLimited(regionName, limit);
    }

    @Override
    public void regionLimitFailed(String regionName) {
        command.regionLimitFailed(regionName);
    }

    @Override
    public void regionLimitReached(UUID uuid, String regionName) {
         regionListener.fullRegion(regionName);
    }
}
