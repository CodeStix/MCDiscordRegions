package nl.codestix.mcdiscordregions;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.UUID;

public class RegionEvents implements Listener, IDiscordPlayerEvents {

    private MCDiscordRegionsPlugin plugin;
    private boolean useWhitelist = false;

    private HashMap<UUID, Member> currentSession = new HashMap<>();

    // To limit player Discord channel moving
    private HashMap<Long, Integer> delayedPlayerMoveTasks = new HashMap<>();
    private HashMap<Long, Long> lastPlayerTryMoveTimes = new HashMap<>();

    public boolean kickOnDiscordLeave = true;
    public String kickOnDiscordLeaveMessage = "Not registered.";

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
            forcePlayerMoveChannel(pl, channelMember);
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
        Member member = currentSession.get(pl.getUniqueId());
        if (member == null) {
            if (plugin.bot.getEntryChannel() != null) {
                pl.sendMessage(String.format("§eThis server supports Discord Regions, to use this feature, " +
                        "go to the Discord server of this Minecraft server and join the §f%s§e channel. " +
                        "Then, send your Minecraft in-game name to a bot named §f%s§e in private.", plugin.bot.getEntryChannel().getName(), plugin.bot.getName()));
            }
        }
        else {
            forcePlayerMoveChannel(pl, member);
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
        Player pl = event.getPlayer();
        Member member = currentSession.get(pl.getUniqueId());
        if (member != null)
            forcePlayerMoveChannel(pl, member);
    }

    private boolean tryMoveDelayed(Member member, VoiceChannel voiceChannel) {
        int userLimit = voiceChannel.getUserLimit();
        if (userLimit == 0 || voiceChannel.getMembers().size() < userLimit) {
            forceMoveDelayed(member, voiceChannel);
            return true;
        }
        else {
            return false;
        }
    }

    private void forceMoveDelayed(Member member, VoiceChannel voiceChannel) {
        GuildVoiceState state = member.getVoiceState();
        if (state == null || !state.inVoiceChannel() || state.getChannel().getIdLong() == voiceChannel.getIdLong())
            return;
        long id = member.getIdLong();
        long currentTime = System.currentTimeMillis();
        Long l = lastPlayerTryMoveTimes.get(id);
        if (l == null || currentTime - l > 1000) {
            // move instantly
            plugin.bot.move(member, voiceChannel);
        }
        else {
            // delay the move
            final int MOVE_DELAY_TICKS = 16; // ~20 ticks per second
            if (delayedPlayerMoveTasks.containsKey(id))
                Bukkit.getScheduler().cancelTask(delayedPlayerMoveTasks.get(id));
            delayedPlayerMoveTasks.put(id, Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> plugin.bot.move(member, voiceChannel), MOVE_DELAY_TICKS));
        }
        lastPlayerTryMoveTimes.put(id, currentTime);
    }

    public void forcePlayerMoveChannel(Player pl, Member member) {
        ApplicableRegionSet set = WorldGuardHandler.getPlayerRegions(pl);
        String channelName = set.queryValue(null, plugin.discordChannelFlag);
        if (channelName == null) {
            forceMoveDelayed(member, plugin.bot.getEntryChannel());
            plugin.getLogger().warning("No global Discord channel defined, use '/region flag __global__ discord-channel Global' to set the global Discord channel to 'Global'.");
            return;
        }

        VoiceChannel vc = plugin.bot.getChannelByName(channelName);
        if (vc == null)
            plugin.bot.createChannel(channelName, nvc -> forceMoveDelayed(member, nvc));
        else
            forceMoveDelayed(member, vc);
    }

    @EventHandler
    public void onRegionChange(RegionChangeEvent event) {
        UUID id = event.getId();
        Member member = currentSession.get(id);
        if (member == null || !plugin.bot.isInVoiceChannel(member))
            return;

//        Bukkit.getLogger().info(event.getPlayer().getName() + " entered new region: " + (event.getEnteredRegion() == null ? "(null)" : event.getEnteredRegion().getId()));

        String channelName = event.getRegionSet().queryValue(null, plugin.discordChannelFlag);
        if (channelName == null)
        {
            forceMoveDelayed(member, plugin.bot.getEntryChannel());
            plugin.getLogger().warning("No global Discord channel defined, use '/region flag __global__ discord-channel Global' to set the global Discord channel to 'Global'.");
            return;
        }

        VoiceChannel vc = plugin.bot.getChannelByName(channelName);
        if (vc == null)
        {
            plugin.bot.createChannel(channelName, c -> {
                if (c != null)
                    plugin.getLogger().info("Created new voice channel");
            });
            event.setCancelled(true); // cancel until channel is created
            return;
        }

        if (!tryMoveDelayed(member, vc)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(String.format("§cYou can not enter %s, it has a limit of %d players.", channelName, vc.getUserLimit()));
        }
    }
}
