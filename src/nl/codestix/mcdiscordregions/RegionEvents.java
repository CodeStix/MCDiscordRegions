package nl.codestix.mcdiscordregions;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.dv8tion.jda.api.entities.*;
import net.raidstone.wgevents.WorldGuardEvents;
import net.raidstone.wgevents.events.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class RegionEvents implements Listener {

    private JavaPlugin plugin;
    private DiscordBot bot;
    private DiscordPlayerLoader playerLoader;
    private boolean createChannelOnUnknown = true;
    private boolean useWhitelist;

    public final int MOVE_DELAY_TICKS = 10;

    private HashMap<UUID, Integer> currentPlayerMoveTasks = new HashMap<>();
    private HashMap<UUID, Member> currentSession = new HashMap<>();

    public void registerPlayer(UUID playerId, Member channelMember) {
        //long userId = channelMember.getUser().getIdLong();
        //playerLoader.setDiscordUser(playerId, discordUser.getIdLong());

        currentSession.put(playerId, channelMember);
        if (useWhitelist)
            Bukkit.getOfflinePlayer(playerId).setWhitelisted(true);
    }

    public void unregisterPlayer(UUID playerId, Member channelMember) {
        currentSession.remove(playerId);
        if (useWhitelist)
            Bukkit.getOfflinePlayer(playerId).setWhitelisted(false);
    }

    public RegionEvents(JavaPlugin plugin, DiscordBot bot, DiscordPlayerLoader playerLoader, boolean useWhitelist) {
        this.plugin = plugin;
        this.useWhitelist = useWhitelist;
        this.bot = bot;
        this.playerLoader = playerLoader;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        UUID id = event.getPlayer().getUniqueId();
        Member member = currentSession.get(id);

        if (member != null && member.getVoiceState().inVoiceChannel())
            bot.getGuild().moveVoiceMember(member, bot.getEntryChannel()).queue();
    }

    private void move(Member member, String regionName) {
        VoiceChannel channel = bot.getChannelByName(regionName);
        if (channel == null) {
            if (createChannelOnUnknown) {
                bot.getChannelByNameOrCreate(regionName, vc -> {
                    bot.getGuild().moveVoiceMember(member, vc).queue();
                    Bukkit.getLogger().info(String.format("Created new voice channel for region '%s'", regionName));
                });
            }
            else {
                Bukkit.getLogger().warning(String.format("Member %s entered region %s but no voice channel was available.", member.getUser().getName(), regionName));
            }
        }
        else {
            bot.getGuild().moveVoiceMember(member, channel).queue();
        }
    }

    @EventHandler
    public void onRegionEntered(RegionEnteredEvent event)
    {
        UUID id = event.getUUID();

        Member member = currentSession.get(id);
        if (member == null || !member.getVoiceState().inVoiceChannel())
            return;

        Set<ProtectedRegion> regions = WorldGuardEvents.getRegions(id);
        regions.add(event.getRegion());
        ProtectedRegion[] reg = regions.toArray(new ProtectedRegion[0]);
        Arrays.sort(reg, (o1, o2) -> o1.volume() - o2.volume());
        ProtectedRegion p = reg[0];

        if (currentPlayerMoveTasks.containsKey(id))
            Bukkit.getScheduler().cancelTask(currentPlayerMoveTasks.get(id));
        currentPlayerMoveTasks.put(id, Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> move(member, p.getId()), MOVE_DELAY_TICKS));
    }

    @EventHandler
    public void onRegionExit(RegionLeftEvent event) {

        UUID id = event.getUUID();

        Member member = currentSession.get(id);
        if (member == null || !member.getVoiceState().inVoiceChannel())
            return;

        Set<ProtectedRegion> regions = WorldGuardEvents.getRegions(id);
        regions.remove(event.getRegion());
        ProtectedRegion[] reg = regions.toArray(new ProtectedRegion[0]);
        Arrays.sort(reg, (o1, o2) -> o1.volume() - o2.volume());
        ProtectedRegion p = reg[0];

        if (currentPlayerMoveTasks.containsKey(id))
            Bukkit.getScheduler().cancelTask(currentPlayerMoveTasks.get(id));
        currentPlayerMoveTasks.put(id, Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> move(member, p.getId()), MOVE_DELAY_TICKS));
    }
}
