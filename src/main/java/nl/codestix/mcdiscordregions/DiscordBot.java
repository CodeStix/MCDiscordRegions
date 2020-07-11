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
    private HashMap<Long, Member> categoryMembers = new HashMap<>();
    private Guild guild;
    private VoiceChannel entryChannel;
    private VoiceChannel globalChannel;
    private IDiscordPlayerEvents discordPlayerListener;
    private IDiscordPlayerDatabase playerDatabase;

    public DiscordBot(String token, IDiscordPlayerDatabase playerDatabase) throws LoginException, InterruptedException
    {
        this.playerDatabase = playerDatabase;

        bot = JDABuilder
                .createDefault(token)
                .setCompression(Compression.NONE) // <- compression not working with Bukkit?
                .addEventListeners(this)
                .build();
        bot.awaitReady();
    }

    public void setDiscordPlayerEventsListener(IDiscordPlayerEvents listener) {
        discordPlayerListener = listener;
    }

    public void setGuild(String guildId) {
        setGuild(bot.getGuildById(guildId));
    }

    public void setGuild(Guild guild) {
        if (guild == null)
            throw new NullArgumentException("Guild is null.");
        this.category = null;
        this.entryChannel = null;
        this.categoryMembers.clear();
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

    public Collection<Member> getCategoryMembers() {
        return categoryMembers.values();
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
        categoryMembers.clear();
        for(Member member : category.getMembers())
            categoryMembers.put(member.getUser().getIdLong(), member);
    }

    public boolean isInVoiceChannel(Member member) {
        return categoryMembers.containsKey(member.getUser().getIdLong());
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

    public Member getMember(long userId) {
        return categoryMembers.get(userId);
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

    private void memberJoinCategory(Member mem) {
        categoryMembers.put(mem.getUser().getIdLong(), mem);
        UUID player = playerDatabase.getPlayer(mem.getUser().getIdLong());
        if (player != null)
            discordPlayerListener.onDiscordPlayerJoin(player, mem);
        // else: needs to register themselves with the Discord bot in private.
    }

    private void memberLeaveCategory(Member mem) {
        categoryMembers.remove(mem.getUser().getIdLong());
        discordPlayerListener.onDiscordPlayerLeave(mem); // async because on bot thread
    }

    private void onMemberVoiceJoin(GuildVoiceJoinEvent event) {
        if (event.getChannelJoined().getParent().getIdLong() == category.getIdLong()) { // if joining mc category
            memberJoinCategory(event.getMember());
        }
    }

    private void onMemberVoiceLeave(GuildVoiceLeaveEvent event) {
        if (event.getChannelLeft().getParent().getIdLong() == category.getIdLong()) {
            memberLeaveCategory(event.getMember());
        }
    }

    private void onMemberVoiceMove(GuildVoiceMoveEvent event) {
        if (event.getChannelLeft().getParent().getIdLong() != category.getIdLong()
         && event.getChannelJoined().getParent().getIdLong() == category.getIdLong()) { // if moved inside mc category
            memberJoinCategory(event.getMember());

        }
        else if (event.getChannelLeft().getParent().getIdLong() == category.getIdLong()
            && event.getChannelJoined().getParent().getIdLong() != category.getIdLong()) { // if moved outside of mc category
            Member mem = event.getMember();
            memberLeaveCategory(event.getMember());
        }

        // Bukkit.getLogger().warning("Unregistered member got moved into a regions channel, but could not be registered. The user should enter the Entry channel and send their name to the bot in private.");
    }

    private void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = categoryMembers.get(event.getAuthor().getIdLong());

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

        if (!playerDatabase.putPlayer(member.getUser().getIdLong(), playerUUID)) {
            message.addReaction("❌").queue();
            return;
        }

        discordPlayerListener.onDiscordPlayerJoin(playerUUID, member);

        message.addReaction("✅").queue();
    }
}
