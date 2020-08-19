package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.utils.Compression;
import nl.codestix.mcdiscordregions.database.DiscordPlayerDatabase;
import nl.codestix.mcdiscordregions.event.DiscordPlayerEvents;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

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

    public List<Permission> botAllowPermissions = Arrays.asList(Permission.VOICE_MOVE_OTHERS, Permission.VOICE_MUTE_OTHERS, Permission.VOICE_CONNECT, Permission.VIEW_CHANNEL);
    public List<Permission> channelAllowPermissions = Arrays.asList(Permission.VOICE_SPEAK);
    public List<Permission> channelDenyPermissions = Arrays.asList(Permission.VOICE_CONNECT, Permission.VIEW_CHANNEL, Permission.VOICE_STREAM);
    public List<Permission> entryChannelAllowPermissions = Arrays.asList(Permission.VOICE_CONNECT, Permission.VIEW_CHANNEL);
    public List<Permission> entryChannelDenyPermissions = Arrays.asList(Permission.VOICE_SPEAK, Permission.VOICE_STREAM);

    private DiscordPlayerEvents discordPlayerListener;
    private DiscordPlayerDatabase playerDatabase;

    // To limit player Discord channel moving
    private HashMap<Long, Integer> delayedPlayerMoveTasks = new HashMap<>();
    private HashMap<Long, Long> lastPlayerTryMoveTimes = new HashMap<>();

    private HashMap<UUID, Member> discordPlayers = new HashMap<>();

    public int playerMinimumMoveInterval = 1000;

    public DiscordBot(String token, String guildId, DiscordPlayerDatabase playerDatabase) throws LoginException, InterruptedException
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

    private Role getBotRole() {
         List<Role> roles = bot.getRolesByName(bot.getSelfUser().getName(), false);
         if (roles.size() < 1) {
             throw new IndexOutOfBoundsException("getBotRole has no roles!");
         }
         return roles.get(0);
    }

    public void setDiscordPlayerEventsListener(DiscordPlayerEvents listener) {
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

    public void createNormalChannel(String name, Consumer<VoiceChannel> callback) {
        createChannel(name, channelAllowPermissions, channelDenyPermissions, callback);
    }

    public void createEntryChannel(String name, Consumer<VoiceChannel> callback) {
        createChannel(name, entryChannelAllowPermissions, entryChannelDenyPermissions, callback);
    }

    private void createChannel(String name, List<Permission> allow, List<Permission> deny, Consumer<VoiceChannel> callback) {
        category.createVoiceChannel(name)
                .addRolePermissionOverride(guild.getPublicRole().getIdLong(), allow, deny)
                .addRolePermissionOverride(getBotRole().getIdLong(), botAllowPermissions, new ArrayList<>())
                .queue(vc -> {
            updateChannelCache();
            callback.accept(vc);
        });
    }

    public Category getCategoryByName(String name) {
        List<Category> categories = guild.getCategories();
        for (Category c : categories) {
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
                {
                    discordPlayers.put(player, member);
                    if (discordPlayerListener != null)
                        discordPlayerListener.onDiscordPlayerJoin(player, member);
                }
            }
        }
    }

    public void deleteCategory() {
        for(VoiceChannel channel : channels.values())
            channel.delete().queue();
        category.delete().queue();
        setCategory(null);
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
            createEntryChannel(entryChannelName, c -> {
                entryChannel = c;
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

    public int getChannelCount() {
        return channels.size();
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
            {
                discordPlayers.remove(player);
                if (discordPlayerListener != null)
                    discordPlayerListener.onDiscordPlayerLeave(player, mem);
            }
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

    private void onMemberJoinCategory(Member mem) {
        long id = mem.getUser().getIdLong();
        categoryMembers.put(id, mem);
        UUID player = playerDatabase.getPlayer(id);
        if (player != null)
        {
            discordPlayers.put(player, mem);
            if (discordPlayerListener != null)
                discordPlayerListener.onDiscordPlayerJoin(player, mem);
        }
        // else: needs to register themselves with the Discord bot in private.
    }

    private void onMemberLeaveCategory(Member mem) {
        long id = mem.getUser().getIdLong();
        categoryMembers.remove(id);
        UUID player = playerDatabase.getPlayer(id);
        if (player != null)
        {
            discordPlayers.remove(player);
            if (discordPlayerListener != null)
                discordPlayerListener.onDiscordPlayerLeave(player, mem);
        }
    }

    private void onMemberVoiceJoin(GuildVoiceJoinEvent event) {
        if (category != null && event.getChannelJoined().getParent() == category) { // if joining mc category
            onMemberJoinCategory(event.getMember());
        }
    }

    private void onMemberVoiceLeave(GuildVoiceLeaveEvent event) {
        if (category != null && event.getChannelLeft().getParent() == category) {
            onMemberLeaveCategory(event.getMember());
        }
    }

    private void onMemberVoiceMove(GuildVoiceMoveEvent event) {
        if (category != null
            && event.getChannelLeft().getParent() != category
            && event.getChannelJoined().getParent() == category) { // if moved inside mc category

            onMemberJoinCategory(event.getMember());
        }
        else if (category != null
            && event.getChannelLeft().getParent() == category
            && event.getChannelJoined().getParent() != category) { // if moved outside of mc category

            onMemberLeaveCategory(event.getMember());
        }
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
        if (!MojangAPI.isValidName(playerName)) { // invalid name
            message.getPrivateChannel().sendMessage("❌ That is not a valid Minecraft name.").queue();
            return;
        }

        UUID playerUUID = MojangAPI.playerNameToUUID(playerName);
        if (playerUUID == null) { // player name not found
            message.getPrivateChannel().sendMessage("❌ That player name was not found.").queue();
            return;
        }

        if (!playerDatabase.putPlayer(member.getUser().getIdLong(), playerUUID)) { // database returns false
            message.getPrivateChannel().sendMessage("❌ You don't have the permission!").queue();
            return;
        }

        discordPlayers.put(playerUUID, member);
        if (discordPlayerListener != null)
            discordPlayerListener.onDiscordPlayerJoin(playerUUID, member);

        message.addReaction("✅").queue();
    }

    public void moveNow(Member member, VoiceChannel voiceChannel) {
        if (member == null || !isInVoiceChannel(member))
            return;
        guild.moveVoiceMember(member, voiceChannel).queue();
    }

    public boolean tryMoveDelayed(JavaPlugin scheduleAs, Member member, VoiceChannel voiceChannel) {
        int userLimit = voiceChannel.getUserLimit();
        if (userLimit == 0 || voiceChannel.getMembers().size() < userLimit) {
            forceMoveDelayed(scheduleAs, member, voiceChannel);
            return true;
        }
        else {
            return false;
        }
    }

    public void forceMoveDelayed(JavaPlugin scheduleAs, Member member, VoiceChannel voiceChannel) {
        GuildVoiceState state = member.getVoiceState();
        if (state == null || !state.inVoiceChannel() || state.getChannel() == voiceChannel)
            return;
        long id = member.getIdLong();
        long currentTime = System.currentTimeMillis();
        Long l = lastPlayerTryMoveTimes.get(id);
        if (l == null || currentTime - l > playerMinimumMoveInterval) {
            // move instantly
            moveNow(member, voiceChannel);
        }
        else {
            // delay the move
            final int MOVE_DELAY_TICKS = 17; // ~20 ticks per second
            if (delayedPlayerMoveTasks.containsKey(id))
                Bukkit.getScheduler().cancelTask(delayedPlayerMoveTasks.get(id));
            delayedPlayerMoveTasks.put(id, Bukkit.getScheduler().scheduleSyncDelayedTask(scheduleAs, () -> moveNow(member, voiceChannel), MOVE_DELAY_TICKS));
        }
        lastPlayerTryMoveTimes.put(id, currentTime);
    }

    public Map<UUID, Member> getCurrentDiscordPlayers() {
        return discordPlayers;
    }

    public Member getMember(UUID player) {
        return discordPlayers.get(player);
    }

    public void movePlayerToEntry(UUID player) {
        moveNow(getMember(player), entryChannel);
    }

    public void muteMember(Member member, boolean mute) {
        if (member == null || !isInVoiceChannel(member))
            return;
        member.mute(mute).queue();
    }

    public void muteMember(UUID player, boolean mute) {
        muteMember(getMember(player), mute);
    }
}
