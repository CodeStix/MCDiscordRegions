package nl.codestix.mcdiscordregions;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.dv8tion.jda.api.entities.*;
import net.raidstone.wgevents.WorldGuardEvents;
import net.raidstone.wgevents.events.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;

public class RegionEvents implements Listener, IDiscordPlayerLoader {

    private JavaPlugin plugin;
    private DiscordBot bot;

    public boolean createChannelOnUnknown = true;
    public boolean useWhitelist = true;

    public final int MOVE_DELAY_TICKS = 4; // ~20 ticks per second

    private HashMap<Long, Integer> currentPlayerMoveTasks = new HashMap<>();
    private HashMap<UUID, Member> currentSession = new HashMap<>();

    public RegionEvents(JavaPlugin plugin, DiscordBot bot) {
        this.plugin = plugin;
        this.bot = bot;
    }

    public void tryGetVoiceChannelForRegion(String regionName, Consumer<VoiceChannel> callback) {
        VoiceChannel channel = bot.getChannelByName(regionName);
        if (channel != null) {
            callback.accept(channel);
        }
        else if (createChannelOnUnknown) {
            bot.getChannelByNameOrCreate(regionName, callback);
        }
        else {
            Bukkit.getLogger().warning(String.format("Member entered region %s but no voice channel was available.", regionName));
            callback.accept(null);
        }
    }

    public void registerPlayer(UUID playerId, Member channelMember) {
        currentSession.put(playerId, channelMember);
        if (useWhitelist)
            Bukkit.getOfflinePlayer(playerId).setWhitelisted(true);

        Player pl = plugin.getServer().getPlayer(playerId);
        if (pl != null)
            moveNow(channelMember, getPlayerRegionName(playerId, null, null));
    }

    public UUID getUUIDFromMember(Member channelMember) {
        for(Map.Entry<UUID, Member> mem : currentSession.entrySet())
            if (mem.getValue().getIdLong() == channelMember.getIdLong())
                return mem.getKey();
        return null;
    }

    public boolean unregisterPlayer(Member channelMember, boolean async) {
        return unregisterPlayer(getUUIDFromMember(channelMember), async);
    }

    public void unregisterAllPlayers() {
        for(UUID id : currentSession.keySet())
            unregisterPlayer(id, false);
    }

    public boolean unregisterPlayer(UUID playerId, boolean async) {
        if (playerId == null)
            return false;

        boolean removed = currentSession.remove(playerId) != null;

        if (useWhitelist)
            Bukkit.getOfflinePlayer(playerId).setWhitelisted(false);

        Player pl = plugin.getServer().getPlayer(playerId);
        if (pl != null)
        {
            final String KICK_MESSAGE = "Not registered.";
            if (async)
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> pl.kickPlayer(KICK_MESSAGE));
            else
                pl.kickPlayer(KICK_MESSAGE);
        }
        return removed;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player pl = event.getPlayer();
        UUID id = pl.getUniqueId();
        Member member = currentSession.get(id);
        if (member == null) {
            pl.sendMessage(String.format("§eThis server supports Discord Regions, to use this feature, " +
                    "go to the Discord server of this Minecraft server and join the §f%s§e channel. " +
                    "Then, send your Minecraft in-game name to a bot named §f%s§e in private.",  bot.getEntryChannel().getName(), bot.getName()));
        }
        else {
            moveNow(member, getPlayerRegionName(id, null, null));
        }
    }

    private void moveToEntry(UUID id) {
        moveToEntry(currentSession.get(id));
    }

    private void moveToEntry(Member member) {
        if (member != null && bot.isInVoiceChannel(member))
            bot.getGuild().moveVoiceMember(member, bot.getEntryChannel()).queue();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        moveToEntry(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        moveToEntry(event.getPlayer().getUniqueId());
    }

    private void moveNow(Member member, String regionName) {
        tryGetVoiceChannelForRegion(regionName, (vc) -> {
            if (vc != null)
                bot.getGuild().moveVoiceMember(member, vc).queue();
        });
    }

    private void moveDelayed(Member member, String regionName) {
        long id = member.getIdLong();
        if (currentPlayerMoveTasks.containsKey(id))
            Bukkit.getScheduler().cancelTask(currentPlayerMoveTasks.get(id));
        currentPlayerMoveTasks.put(id, Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> moveNow(member, regionName), MOVE_DELAY_TICKS));
    }

    public String getPlayerRegionName(UUID playerId, ProtectedRegion include, ProtectedRegion exclude) {
        Set<ProtectedRegion> regions = WorldGuardEvents.getRegions(playerId);
        if (include != null)
            regions.add(include);
        if (exclude != null)
            regions.remove(exclude);
        ProtectedRegion[] reg = regions.toArray(new ProtectedRegion[0]);
        Arrays.sort(reg, (o1, o2) -> o1.volume() - o2.volume()); // Sort by region volume

        if (reg.length <= 0)
            return bot.getGlobalChannel().getName();
        else
            return reg[0].getId(); // The first item in the sorted list is the smallest region
    }

    @EventHandler
    public void onRegionEntered(RegionEnteredEvent event)
    {
        UUID id = event.getUUID();

        Member member = currentSession.get(id);
        if (member == null || !bot.isInVoiceChannel(member))
            return;

        moveDelayed(member, getPlayerRegionName(id, event.getRegion(), null));
    }

    @EventHandler
    public void onRegionExit(RegionLeftEvent event) {

        UUID id = event.getUUID();

        Member member = currentSession.get(id);
        if (member == null || !bot.isInVoiceChannel(member))
            return;

        moveDelayed(member, getPlayerRegionName(id, null, event.getRegion()));
    }
}
