package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.utils.Compression;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Bukkit;

import javax.security.auth.login.LoginException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class DiscordBot implements EventListener {

    private JDA bot;

    private Category category;
    private HashMap<String, VoiceChannel> channels = new HashMap<>();
    private HashMap<Long, Member> channelMembers = new HashMap<>();
    private Guild guild;
    private VoiceChannel entryChannel;
    private VoiceChannel globalChannel;
    private IDiscordPlayerLoader playerLoader;

    public DiscordBot(String token) throws LoginException, InterruptedException
    {
        bot = JDABuilder
                .createDefault(token)
                .setCompression(Compression.NONE) // <- compression not working with Bukkit?
                .addEventListeners(this)
                .build();
        bot.awaitReady();
    }

    public void setPlayerLoader(IDiscordPlayerLoader playerLoader) {
        this.playerLoader = playerLoader;
    }

    public void setGuild(String guildId) {
        setGuild(bot.getGuildById(guildId));
    }

    public void setGuild(Guild guild) {
        if (guild == null)
            throw new NullArgumentException("Guild is null.");
        this.category = null;
        this.entryChannel = null;
        this.channelMembers.clear();
        this.channels.clear();
        this.guild = guild;
    }

    public Guild getFirstGuild() {
        List<Guild> guilds = bot.getGuilds();
        return guilds.size() <= 0 ? null : guilds.get(0);
    }

    public void getChannelByNameOrCreate(String name, Consumer<VoiceChannel> callback) {
        if (channels.containsKey(name))
            callback.accept(channels.get(name));
        else
            category.createVoiceChannel(name).queue(vc -> {
                updateChannelCache();
                callback.accept(vc);
            });
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

    public Collection<Member> getChannelMembers() {
        return channelMembers.values();
    }

    public VoiceChannel getGlobalChannel() {
        if (globalChannel == null)
            return entryChannel;
        else
            return globalChannel;
    }

    public void setGlobalChannel(String globalChannelName, boolean allowRenamePrevious) {
        if (category == null)
            throw new NullArgumentException("Category is null, this must be set first using setCategory.");

        if (allowRenamePrevious && globalChannel != null) {
            globalChannel.getManager().setName(globalChannelName).queue();
        }
        else if (channels.containsKey(globalChannelName)) {
            globalChannel = channels.get(globalChannelName);
        }
        else {
            category.createVoiceChannel(globalChannelName).queue(c -> {
                globalChannel = c;
                updateChannelCache();
            });
        }
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

    public void updateChannelCache() {
        // Cache the channels by name
        channels.clear();
        for(VoiceChannel channel : category.getVoiceChannels())
            channels.put(channel.getName(), channel);

        // Cache the members by user id
        channelMembers.clear();
        for(Member member : category.getMembers())
            channelMembers.put(member.getUser().getIdLong(), member);
    }

    public boolean isInVoiceChannel(Member member) {
        return channelMembers.containsKey(member.getUser().getIdLong());
    }

    public void setEntryChannel(String entryChannelName, boolean allowRenamePrevious) {
        if (category == null)
            throw new NullArgumentException("Category is null, this must be set first using setCategory.");

        if (allowRenamePrevious && entryChannel != null) {
            entryChannel.getManager().setName(entryChannelName).queue();
        }
        else if (channels.containsKey(entryChannelName)) {
            entryChannel = channels.get(entryChannelName);
        }
        else {
            category.createVoiceChannel(entryChannelName).queue(c -> {
                entryChannel = c;
                updateChannelCache();
            });
        }
    }

    public String getName() {
        return bot.getSelfUser().getName();
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

    @Override
    public void onEvent(GenericEvent genericEvent) {
//        Bukkit.getLogger().info("Event: " + genericEvent.getClass().getName());
        if (genericEvent instanceof PrivateMessageReceivedEvent)
            onPrivateMessageReceived((PrivateMessageReceivedEvent)genericEvent);
        else if (genericEvent instanceof GuildVoiceJoinEvent)
            onMemberVoiceJoin((GuildVoiceJoinEvent)genericEvent);
        else if (genericEvent instanceof GuildVoiceLeaveEvent)
            onMemberVoiceLeave((GuildVoiceLeaveEvent)genericEvent);
        else if (genericEvent instanceof GuildVoiceMoveEvent)
            onMemberVoiceMove((GuildVoiceMoveEvent)genericEvent);
    }

    private void onMemberVoiceJoin(GuildVoiceJoinEvent event) {
        Member mem = event.getMember();
        channelMembers.put(mem.getUser().getIdLong(), mem);
    }

    private void onMemberVoiceLeave(GuildVoiceLeaveEvent event) {
        Member mem = event.getMember();
        channelMembers.remove(mem.getUser().getIdLong());
        playerLoader.unregisterPlayer(mem, true); // async because on bot thread
    }

    private void onMemberVoiceMove(GuildVoiceMoveEvent event) {
        Member mem = event.getMember();
        long id = mem.getUser().getIdLong();
        if (channelMembers.containsKey(id)) {
            if (event.getChannelJoined().getParent().getIdLong() != category.getIdLong()) { // if moved outside of MCDiscordRegions category
                channelMembers.remove(id);
                playerLoader.unregisterPlayer(mem, true); // async because on bot thread
            }
        }
        else {
            Bukkit.getLogger().warning("Unregistered member got moved into a regions channel, but could not be registered. The user should enter the Entry channel and send their name to the bot in private.");
            /*if (event.getChannelJoined().getParent().getIdLong() != category.getIdLong()) { // if moved inside of MCDiscordRegions category
                channelMembers.remove(id);
                playerLoader.unregisterPlayer(mem);
            }*/
        }
    }

    private void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = channelMembers.get(event.getAuthor().getIdLong());

        if (member == null)
            return;
        GuildVoiceState state = member.getVoiceState();
        if (state == null)
            return;

        if (state.getChannel().getIdLong() != entryChannel.getIdLong()) {
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

        playerLoader.registerPlayer(playerUUID, member);

        message.addReaction("✅").queue();
    }
}
