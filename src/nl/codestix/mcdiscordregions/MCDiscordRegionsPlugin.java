package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.EventListener;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class MCDiscordRegionsPlugin extends JavaPlugin implements EventListener {

    private RegionEvents regionEvents;
    private DiscordBot bot;
    private DiscordPlayerLoader playerLoader;

    private int saveDiscordTagsTaskId;

    public void saveDiscordTags() {
        getLogger().info("Saving players.yml");
        try {
            playerLoader.save();
        } catch (IOException ex) {
            getLogger().warning("Could not save players.yml: " + ex);
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            playerLoader = new DiscordPlayerLoader(new File(getDataFolder(), "players.yml"));
        }
        catch(InvalidConfigurationException ex) {
            getLogger().warning("Invalid player.yml: " + ex);
            return;
        }
        catch (IOException ex) {
            getLogger().warning("Could not load player.yml: " + ex);
            return;
        }

        final int DISCORD_TAG_SAVE_INTERVAL = 20 * 60 * 2;
        saveDiscordTagsTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> saveDiscordTags(), DISCORD_TAG_SAVE_INTERVAL, DISCORD_TAG_SAVE_INTERVAL);

        String token = getConfig().getString("token");
        if (token == null) {
            getLogger().warning("Please enter your bot client secret in the config file of this plugin!");
            return;
        }

        try {
            bot = new DiscordBot(token, this);
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
            bot.setGuild(serverId);
        }
        else {
            Guild firstGuild = bot.getFirstGuild();
            if (firstGuild != null) {
                bot.setGuild(firstGuild);
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
        if (categoryName != null && bot.getGuild() != null) {
            getLogger().info(String.format("Setting discord category to '%s'", categoryName));
            try {
                bot.setCategory(categoryName);
            }
            catch(PermissionException ex) {
                getLogger().warning("Could not set category due to permissions: " + ex.getMessage());
            }
            catch(NullArgumentException ex) {
                getLogger().warning(String.format("The configured category '%s' was not found.", categoryName));
            }
        }

        String entryChannelName = getConfig().getString("entry-channel-name");
        if (entryChannelName != null && bot.getCategory() != null) {
            getLogger().info(String.format("Setting entry voice channel to '%s'", entryChannelName));
            try {
                bot.setEntryChannel(entryChannelName, false);
            }
            catch(PermissionException ex) {
                getLogger().warning("Could not set entry channel name due to permissions: " + ex.getMessage());
            }
        }

        regionEvents = new RegionEvents(this, bot, playerLoader, false);

        getCommand("drg").setExecutor(new DiscordRegionsCommand(this, bot));
        Bukkit.getPluginManager().registerEvents(regionEvents, this);

        getLogger().info("Is configured correctly!");
    }

    @Override
    public void onDisable() {
        bot.destroy();
        Bukkit.getScheduler().cancelTask(saveDiscordTagsTaskId);
        saveDiscordTags();
        getLogger().info("Is now disabled!");
    }

    @Override
    public void onEvent(GenericEvent genericEvent) {
        if (genericEvent instanceof PrivateMessageReceivedEvent)
            onPrivateMessageReceived((PrivateMessageReceivedEvent)genericEvent);
    }

    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        Message message = event.getMessage();

        List<Member> members = bot.getEntryChannel().getMembers();
        Member member = null;
        for(Member m : members) {
            if (m.getUser().getIdLong() == event.getAuthor().getIdLong()) {
                member = m;
                break;
            }
        }

        if (member == null) {
            message.addReaction("❓").queue(); // not in entry channel
            return;
        }

        String playerName = message.getContentRaw();
        if (!MojangAPI.isValidName(playerName)) {
            message.addReaction("❌").queue(); // not a valid name
            return;
        }

        UUID playerUUID = MojangAPI.playerNameToUUID(playerName);
        if (playerUUID == null) {
            message.addReaction("❌").queue(); // player not found
            return;
        }

        regionEvents.registerPlayer(playerUUID, member);

        message.addReaction("✅").queue();
    }
}
