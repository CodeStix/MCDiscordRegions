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
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.*;
import java.util.function.Consumer;

public class RegionEvents implements Listener, IDiscordPlayerEvents {

    private MCDiscordRegionsPlugin plugin;
    private boolean useWhitelist = false;
    private HashMap<Long, Integer> currentPlayerMoveTasks = new HashMap<>();
    private HashMap<UUID, Member> currentSession = new HashMap<>();

    public boolean kickOnDiscordLeave = true;
    public String kickOnDiscordLeaveMessage = "Not registered.";

    private final int MOVE_DELAY_TICKS = 4; // ~20 ticks per second

    public RegionEvents(MCDiscordRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean getUseWhitelist() {
        return useWhitelist;
    }

    public void setUseWhitelist(boolean useWhitelist) {
        if (this.useWhitelist && !useWhitelist)
            for(UUID pl : currentSession.keySet())
                Bukkit.getOfflinePlayer(pl).setWhitelisted(false);

        this.useWhitelist = useWhitelist;
    }

    @Override
    public void onDiscordPlayerJoin(UUID playerId, Member channelMember) {
        currentSession.put(playerId, channelMember);
        if (useWhitelist)
            Bukkit.getOfflinePlayer(playerId).setWhitelisted(true);

        Player pl = plugin.getServer().getPlayer(playerId);
        if (pl != null)
        {
            String regionName = getPlayerRegionName(playerId, null, null);
            plugin.bot.getChannelByNameOrCreate(regionName, vc -> {
                if (vc != null)
                    moveNow(channelMember, vc);
            });
        }
    }

    @Override
    public boolean onDiscordPlayerLeave(UUID playerId, Member channelMember) {
        if (playerId == null)
            return false;

        boolean removed = currentSession.remove(playerId) != null;

        if (useWhitelist)
            Bukkit.getOfflinePlayer(playerId).setWhitelisted(false);

        if (kickOnDiscordLeave) {
            Player pl = plugin.getServer().getPlayer(playerId);
            if (pl != null)
            {
                if (Bukkit.isPrimaryThread())
                    pl.kickPlayer(kickOnDiscordLeaveMessage);
                else
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> pl.kickPlayer(kickOnDiscordLeaveMessage));
            }
        }

        return removed;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player pl = event.getPlayer();
        UUID id = pl.getUniqueId();
        Member member = currentSession.get(id);
        if (member == null) {
            if (plugin.bot.getEntryChannel() != null) {
                pl.sendMessage(String.format("§eThis server supports Discord Regions, to use this feature, " +
                        "go to the Discord server of this Minecraft server and join the §f%s§e channel. " +
                        "Then, send your Minecraft in-game name to a bot named §f%s§e in private.", plugin.bot.getEntryChannel().getName(), plugin.bot.getName()));
            }
        }
        else {
            String regionName = getPlayerRegionName(id, null, null);
            plugin.bot.getChannelByNameOrCreate(regionName, vc -> {
                if (vc != null)
                    moveNow(member, vc);
            });
        }
    }

    private void moveToEntry(UUID id) {
        moveToEntry(currentSession.get(id));
    }

    private void moveToEntry(Member member) {
        if (member != null && plugin.bot.isInVoiceChannel(member))
            plugin.bot.getGuild().moveVoiceMember(member, plugin.bot.getEntryChannel()).queue();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        moveToEntry(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        moveToEntry(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        String regionName = getPlayerRegionName(id, null, null);
        plugin.bot.getChannelByNameOrCreate(regionName, vc -> {
            if (vc != null)
                moveNow(currentSession.get(id), vc);
        });
    }

    private void moveNow(Member member, VoiceChannel voiceChannel) {
        plugin.bot.getGuild().moveVoiceMember(member, voiceChannel).queue();
    }

    private void moveDelayed(Member member, VoiceChannel voiceChannel) {
        long id = member.getIdLong();
        if (currentPlayerMoveTasks.containsKey(id))
            Bukkit.getScheduler().cancelTask(currentPlayerMoveTasks.get(id));
        currentPlayerMoveTasks.put(id, Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> moveNow(member, voiceChannel), MOVE_DELAY_TICKS));
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
            return plugin.bot.getGlobalChannel().getName();
        else
            return reg[0].getId(); // The first item in the sorted list is the smallest region
    }

    @EventHandler
    public void onRegionEntered(RegionEnteredEvent event) {
        UUID id = event.getUUID();
        Member member = currentSession.get(id);
        if (member == null || !plugin.bot.isInVoiceChannel(member))
            return;

        String regionName = getPlayerRegionName(id, event.getRegion(), null);
        plugin.bot.getChannelByNameOrCreate(regionName, vc -> {
            int userLimit = vc.getUserLimit();
            if (vc == null)
                return;
            if (userLimit == 0 || vc.getMembers().size() < userLimit) {
                moveDelayed(member, vc);
            }
            else {
                event.setCancelled(true);
                event.getPlayer().sendMessage(String.format("§cYou can not enter %s, it has a limit of %d players.", regionName, userLimit));
            }
        });
    }

    @EventHandler
    public void onRegionExit(RegionLeftEvent event) {
        UUID id = event.getUUID();
        Member member = currentSession.get(id);
        if (member == null || !plugin.bot.isInVoiceChannel(member))
            return;

        String regionName = getPlayerRegionName(id, null, event.getRegion());
        plugin.bot.getChannelByNameOrCreate(regionName, vc -> {
            int userLimit = vc.getUserLimit();
            if (vc == null)
                return;
            if (userLimit == 0 || vc.getMembers().size() < userLimit) {
                moveDelayed(member, vc);
            }
            else {
                event.setCancelled(true);
                event.getPlayer().sendMessage(String.format("§cYou can not enter %s, it has a limit of %d players.", regionName, userLimit));
            }
        });
    }
}
