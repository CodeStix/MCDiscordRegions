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
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.Compression;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Bukkit;

import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.function.Consumer;

public class DiscordBot implements EventListener {

    private JDA bot;

    private Category category;
    private HashMap<String, VoiceChannel> channels = new HashMap<>();
    private HashMap<Long, Member> categoryMembers = new HashMap<>();
    private Guild guild;
    private VoiceChannel entryChannel;
    private IDiscordPlayerEvents discordPlayerListener;
    private IDiscordPlayerDatabase playerDatabase;

    public DiscordBot(String token, String guildId, IDiscordPlayerDatabase playerDatabase) throws LoginException, InterruptedException
    {
        this.playerDatabase = playerDatabase;

        bot = JDABuilder
                .createDefault(token)
                .setCompression(Compression.NONE) // <- compression not working with Bukkit?
                .addEventListeners(this)
                .build();
        bot.awaitReady();

        if (guildId == null) {
            List<Guild> guilds = bot.getGuilds();
            guild = guilds.size() <= 0 ? null : guilds.get(0);
        }
        else {
            guild = bot.getGuildById(guildId);
        }

        if (guild == null)
            throw new NullPointerException("No guild was found.");
    }

    public void setDiscordPlayerEventsListener(IDiscordPlayerEvents listener) {
        discordPlayerListener = listener;
    }

    public Guild getGuild() {
        return guild;
    }

    public void getCategoryByNameOrCreate(String name, Consumer<Category> callback) {
        Category category = getCategoryByName(name);
        if (category == null)
            guild.createCategory(name).queue(callback);
        else
            callback.accept(category);
    }

    public void createChannel(String name, Consumer<VoiceChannel> callback) {
        category.createVoiceChannel(name).queue(vc -> {
            updateChannelCache();
            callback.accept(vc);
        });
    }

    public Category getCategoryByName(String name) {
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

    public void setCategory(Category category) throws NullArgumentException {
        if (this.category == category)
            return;

        this.entryChannel = null;
        this.category = category;
        updateChannelCache();
        updateMemberCache();

        // TODO: set category permissions
    }

    public void updateMemberCache() { // Cache members by user id
        letAllCategoryPlayersLeave();

        categoryMembers.clear();
        if (category != null) {
            for(Member member : category.getMembers()) {
                long id = member.getUser().getIdLong();
                categoryMembers.put(id, member);
                UUID player = playerDatabase.getPlayer(id);
                if (player != null)
                    discordPlayerListener.onDiscordPlayerJoin(player, member);
            }
        }
    }

    public void updateChannelCache() {  // Cache channels by name
        channels.clear();
        if (category != null) {
            for(VoiceChannel channel : category.getVoiceChannels())
                channels.put(channel.getName(), channel);
        }
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

    public void letAllCategoryPlayersLeave() {
        for(Member mem : categoryMembers.values()) {
            UUID player = playerDatabase.getPlayer(mem.getUser().getIdLong());
            if (player != null)
                discordPlayerListener.onDiscordPlayerLeave(player, mem);
        }
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
        long id = mem.getUser().getIdLong();
        categoryMembers.put(id, mem);
        UUID player = playerDatabase.getPlayer(id);
        if (player != null)
            discordPlayerListener.onDiscordPlayerJoin(player, mem);
        // else: needs to register themselves with the Discord bot in private.
    }

    private void memberLeaveCategory(Member mem) {
        long id = mem.getUser().getIdLong();
        categoryMembers.remove(id);
        UUID player = playerDatabase.getPlayer(id);
        if (player != null)
            discordPlayerListener.onDiscordPlayerLeave(player, mem);
    }

    private void onMemberVoiceJoin(GuildVoiceJoinEvent event) {
        if (category != null && event.getChannelJoined().getParent().getIdLong() == category.getIdLong()) { // if joining mc category
            memberJoinCategory(event.getMember());
        }
    }

    private void onMemberVoiceLeave(GuildVoiceLeaveEvent event) {
        if (category != null && event.getChannelLeft().getParent().getIdLong() == category.getIdLong()) {
            memberLeaveCategory(event.getMember());
        }
    }

    private void onMemberVoiceMove(GuildVoiceMoveEvent event) {
        if (category != null
            && event.getChannelLeft().getParent().getIdLong() != category.getIdLong()
            && event.getChannelJoined().getParent().getIdLong() == category.getIdLong()) { // if moved inside mc category

            memberJoinCategory(event.getMember());
        }
        else if (category != null
            && event.getChannelLeft().getParent().getIdLong() == category.getIdLong()
            && event.getChannelJoined().getParent().getIdLong() != category.getIdLong()) { // if moved outside of mc category

            memberLeaveCategory(event.getMember());
        }

        // Bukkit.getLogger().warning("Unregistered member got moved into a regions channel, but could not be registered. The user should enter the Entry channel and send their name to the bot in private.");
    }

    private void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = categoryMembers.get(event.getAuthor().getIdLong());

        if (member == null)
            return;

        /* if (state.getChannel().getIdLong() != entryChannel.getIdLong()) {
            message.addReaction("❓").queue(); // not in entry channel
            return;
        }*/

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

    public void move(Member member, VoiceChannel voiceChannel) {
        guild.moveVoiceMember(member, voiceChannel).queue();
    }
}
