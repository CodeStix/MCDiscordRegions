package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import net.raidstone.wgevents.events.*;

import javax.security.auth.login.LoginException;

public class MCDiscordRegionsPlugin extends JavaPlugin implements Listener {

    private DiscordBot discord;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String token = getConfig().getString("token");
        if (token == null) {
            getLogger().warning("Please enter your bot client secret in the config file of this plugin!");
            return;
        }

        try {
            discord = new DiscordBot(token);
        }
        catch(InterruptedException ex) {
            getLogger().warning("Login got interrupted: " + ex);
        }
        catch(LoginException ex) {
            getLogger().warning("Invalid token: " + ex);
            return;
        }

        String serverId = getConfig().getString("server");
        if (serverId != null) {
            discord.setGuild(serverId);
        }
        else {
            Guild firstGuild = discord.getFirstGuild();
            if (firstGuild != null) {
                discord.setGuild(firstGuild);
                getLogger().warning("No discord server configured, please set a server id " +
                        "in the config or use /drg server <id> to set a server. " +
                        "NOTE: Currently using the first server the bot is in: " + firstGuild.getName());
            }
            else {
                getLogger().warning("No discord server configured, please set a server id " +
                        "in the config or use /drg server <id> to set a server.");
            }
        }

        String categoryName = getConfig().getString("category");
        if (categoryName != null && discord.getGuild() != null) {
            getLogger().info(String.format("Setting discord category to '%s'", categoryName));
            try {
                discord.setCategory(categoryName);
            }
            catch(PermissionException ex) {
                getLogger().warning("Could not set category due to permissions: " + ex.getMessage());
            }
            catch(NullArgumentException ex) {
                getLogger().warning(String.format("The configured category '%s' was not found.", categoryName));
            }
        }

        String entryChannelName = getConfig().getString("entry-channel-name");
        if (entryChannelName != null && discord.getCategory() != null) {
            getLogger().info(String.format("Setting entry voice channel to '%s'", entryChannelName));
            try {
                discord.setEntryChannel(entryChannelName, false);
            }
            catch(PermissionException ex) {
                getLogger().warning("Could not set entry channel name due to permissions: " + ex.getMessage());
            }
        }

        getCommand("drg").setExecutor(new Commands(this, discord));
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("Is configured correctly!");
    }

    @Override
    public void onDisable() {
        discord.destroy();
        getLogger().info("Is now disabled!");
    }

    @EventHandler
    public void onRegionEntered(RegionEnteredEvent event)
    {
        Player player = event.getPlayer();
        if (player == null) return;

        String regionName = event.getRegionName();
        Bukkit.getServer().broadcastMessage(player.getName() + " entered " + regionName);
    }

    @EventHandler
    public void onRegionExit(RegionLeftEvent event) {

        Player player = event.getPlayer();
        if (player == null) return;

        String regionName = event.getRegionName();
        Bukkit.getServer().broadcastMessage(player.getName() + " left " + regionName);
    }
}
