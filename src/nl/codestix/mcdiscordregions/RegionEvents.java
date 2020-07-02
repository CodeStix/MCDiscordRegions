package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.WidgetUtil;
import net.raidstone.wgevents.events.RegionEnteredEvent;
import net.raidstone.wgevents.events.RegionLeftEvent;
import net.raidstone.wgevents.events.RegionsEnteredEvent;
import net.raidstone.wgevents.events.RegionsLeftEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.UUID;

public class RegionEvents implements Listener {

    public DiscordBot bot;
    public DiscordPlayerLoader playerLoader;
    private boolean createChannelOnUnknown = true;
    private boolean useWhitelist;

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

    public RegionEvents(DiscordBot bot, DiscordPlayerLoader playerLoader, boolean useWhitelist) {
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

    @EventHandler
    public void onRegionsEntered(RegionsEnteredEvent event) {

        Bukkit.getLogger().info("RegionsEnteredEvent: " + String.join(", ", event.getRegionsNames()));
    }

    @EventHandler
    public void onRegionsLeft(RegionsLeftEvent event) {

        Bukkit.getLogger().info("RegionsLeftEvent: " + String.join(", ", event.getRegionsNames()));
    }

    @EventHandler
    public void onRegionEntered(RegionEnteredEvent event)
    {
        UUID id = event.getUUID();

        Member member = currentSession.get(id);
        if (member == null || !member.getVoiceState().inVoiceChannel())
            return;

        String regionName = event.getRegionName();
        VoiceChannel channel = bot.getChannelByName(regionName);
        if (channel == null) {
            if (createChannelOnUnknown) {
                bot.getChannelByNameOrCreate(regionName, vc -> {
                    bot.getGuild().moveVoiceMember(member, vc).queue();
                    Bukkit.getLogger().info(String.format("Created new voice channel for region '%s'", regionName));
                });
            }
            else {
                Bukkit.getLogger().warning(String.format("Player %s entered region %s but no voice channel was available.", event.getPlayer().getName(), regionName));
            }
        }
        else {
            bot.getGuild().moveVoiceMember(member, channel).queue();
        }
    }

    @EventHandler
    public void onRegionExit(RegionLeftEvent event) {

        Player player = event.getPlayer();
        if (player == null) return;

        String regionName = event.getRegionName();
        Bukkit.getServer().broadcastMessage(player.getName() + " left " + regionName);
    }
}
