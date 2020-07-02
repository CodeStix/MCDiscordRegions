package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.utils.Compression;
import org.apache.commons.lang.NullArgumentException;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.List;

public class DiscordBot {

    private JDA bot;

    private Category category;
    private HashMap<String, VoiceChannel> channels = new HashMap<>();
    private Guild guild;
    private VoiceChannel entryChannel;

    public DiscordBot(String token) throws LoginException, InterruptedException
    {
        bot = JDABuilder
                .createDefault(token)
                .setCompression(Compression.NONE) // <- compression not working with Bukkit?
                .build();
        bot.awaitReady();
    }

    public void setGuild(String guildId) {
        setGuild(bot.getGuildById(guildId));
    }

    public void setGuild(Guild guild) {
        if (guild == null)
            throw new NullArgumentException("Guild is null.");
        this.category = null;
        this.entryChannel = null;
        this.channels.clear();
        this.guild = guild;
    }

    public Guild getFirstGuild() {
        List<Guild> guilds = bot.getGuilds();
        return guilds.size() <= 0 ? null : guilds.get(0);
    }

    public Category getCategoryByName(String name) {
        if (guild == null)
            throw new NullArgumentException("No server selected, select a server first using setGuild.");
        List<Category> categories = guild.getCategories();
        for(int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            if (c.getName().equalsIgnoreCase(name))
                return c;
        }
        return null;
    }

    public VoiceChannel getChannelByName(String name) {
        return channels.get(name);
    }

    public void setCategory(String name) throws NullArgumentException {
        if (guild == null)
            throw new NullArgumentException("Guild must be set first. Use setGuild.");
        Category category = getCategoryByName(name);
        if (category == null) {
            guild.createCategory(name).queue(c ->  {
                setCategory(c);
            });
        }
        else {
            setCategory(category);
        }
    }

    public void setCategory(Category category) throws NullArgumentException {
        if (category == null)
            throw new NullArgumentException("Category is null.");
        if (this.category == category)
            return;

        this.entryChannel = null;
        this.category = category;
        updateChannelCache();

        // TODO: set category permissions
    }

    public Guild getGuild() {
        return guild;
    }

    private void updateChannelCache() {
        // Cache the channels by name
        channels.clear();
        for(VoiceChannel channel : category.getVoiceChannels())
            channels.put(channel.getName(), channel);
    }

    public void setEntryChannel(String entryChannelName, boolean allowRenamePrevious) {
        if (category == null)
            throw new NullArgumentException("Category is null, this must be set first using setCategory.");

        if (allowRenamePrevious && entryChannel != null) {
            entryChannel.getManager().setName(entryChannelName).queue();
        }
        else {
            if (channels.containsKey(entryChannelName))
                entryChannel = channels.get(entryChannelName);
            else
                category.createVoiceChannel(entryChannelName).queue(c -> {
                    entryChannel = c;
                });
        }

        updateChannelCache();
    }

    public Category getCategory() {
        return category;
    }

    public VoiceChannel getEntryChannel() {
        return entryChannel;
    }

    public HashMap<String, VoiceChannel> getChannels() {
        return channels;
    }

    public void destroy() {
        bot.shutdownNow();
    }

    public void sendMessage(String message) {
        String id = "719991301994709003";
        TextChannel channel = bot.getTextChannelById(id);


        channel.sendMessage(message).queue();
    }

}
